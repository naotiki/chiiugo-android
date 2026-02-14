package me.naotiki.chiiugo.domain.screen

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Handler
import android.os.Looper
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.ByteArrayOutputStream
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.min
import kotlin.math.roundToInt

class ScreenCaptureManager(
    private val context: Context
) {
    private val sessionMutex = Mutex()
    private val mainHandler by lazy { Handler(Looper.getMainLooper()) }

    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null

    private val japaneseRecognizer: TextRecognizer by lazy {
        TextRecognition.getClient(JapaneseTextRecognizerOptions.Builder().build())
    }
    private val latinRecognizer: TextRecognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    suspend fun start(resultCode: Int, projectionData: Intent): Boolean = withContext(Dispatchers.Default) {
        sessionMutex.withLock {
            stopInternal()
            runCatching {
                val manager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                val projection = manager.getMediaProjection(resultCode, projectionData) ?: return@runCatching false
                val metrics = context.resources.displayMetrics
                val width = metrics.widthPixels.coerceAtLeast(1)
                val height = metrics.heightPixels.coerceAtLeast(1)
                val density = metrics.densityDpi.coerceAtLeast(1)

                val reader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 3)
                val display = projection.createVirtualDisplay(
                    "chiiugo-screen-capture",
                    width,
                    height,
                    density,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                    reader.surface,
                    null,
                    null
                )

                mediaProjection = projection
                imageReader = reader
                virtualDisplay = display
                true
            }.getOrDefault(false)
        }
    }

    suspend fun stop() {
        withContext(Dispatchers.Default) {
            sessionMutex.withLock {
                stopInternal()
            }
        }
    }

    suspend fun captureOnce(): ScreenCaptureResult? = withContext(Dispatchers.Default) {
        val reader = sessionMutex.withLock { imageReader } ?: return@withContext null
        val image = reader.acquireLatestImage() ?: awaitNextImage(reader, CAPTURE_TIMEOUT_MILLIS) ?: return@withContext null

        try {
            val rawBitmap = image.toBitmap()
            var resizedBitmap: Bitmap? = null
            try {
                val scaledBitmap = rawBitmap.scaleDown(MAX_IMAGE_DIMENSION)
                resizedBitmap = scaledBitmap
                val imageBytes = scaledBitmap.toJpegBytes(JPEG_QUALITY)
                val ocrText = extractText(scaledBitmap)
                ScreenCaptureResult(
                    imageJpegBytes = imageBytes,
                    ocrText = ocrText
                )
            } finally {
                resizedBitmap?.recycle()
                if (resizedBitmap !== rawBitmap) {
                    rawBitmap.recycle()
                }
            }
        } finally {
            image.close()
        }
    }

    private suspend fun extractText(bitmap: Bitmap): String {
        val input = InputImage.fromBitmap(bitmap, 0)
        val japaneseText = runCatching {
            japaneseRecognizer.process(input).awaitTask().text
        }.getOrDefault("")
        val latinText = runCatching {
            latinRecognizer.process(input).awaitTask().text
        }.getOrDefault("")
        return mergeRecognizedText(japaneseText, latinText)
    }

    private fun mergeRecognizedText(vararg recognized: String): String {
        return recognized
            .flatMap { text -> text.lines() }
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .joinToString("\n")
    }

    private suspend fun awaitNextImage(reader: ImageReader, timeoutMillis: Long): Image? {
        return withTimeoutOrNull(timeoutMillis) {
            suspendCancellableCoroutine { continuation ->
                val listener = ImageReader.OnImageAvailableListener { source ->
                    val latestImage = source.acquireLatestImage() ?: return@OnImageAvailableListener
                    source.setOnImageAvailableListener(null, null)
                    if (continuation.isActive) {
                        continuation.resume(latestImage)
                    } else {
                        latestImage.close()
                    }
                }
                reader.setOnImageAvailableListener(listener, mainHandler)
                continuation.invokeOnCancellation {
                    reader.setOnImageAvailableListener(null, null)
                }
            }
        }
    }

    private fun stopInternal() {
        runCatching { virtualDisplay?.release() }
        runCatching { imageReader?.close() }
        runCatching { mediaProjection?.stop() }
        virtualDisplay = null
        imageReader = null
        mediaProjection = null
    }

    private fun Image.toBitmap(): Bitmap {
        val plane = planes.firstOrNull() ?: throw IllegalStateException("No image planes available")
        val buffer = plane.buffer
        val pixelStride = plane.pixelStride
        val rowStride = plane.rowStride
        val rowPadding = rowStride - pixelStride * width
        val paddedWidth = width + rowPadding / pixelStride

        val paddedBitmap = Bitmap.createBitmap(paddedWidth, height, Bitmap.Config.ARGB_8888)
        paddedBitmap.copyPixelsFromBuffer(buffer)
        return if (paddedWidth == width) {
            paddedBitmap
        } else {
            val cropped = Bitmap.createBitmap(paddedBitmap, 0, 0, width, height)
            paddedBitmap.recycle()
            cropped
        }
    }

    private fun Bitmap.scaleDown(maxDimension: Int): Bitmap {
        val longestEdge = maxOf(width, height)
        if (longestEdge <= maxDimension) {
            return copy(Bitmap.Config.ARGB_8888, false)
        }
        val scale = min(maxDimension.toFloat() / width, maxDimension.toFloat() / height)
        val targetWidth = (width * scale).roundToInt().coerceAtLeast(1)
        val targetHeight = (height * scale).roundToInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(this, targetWidth, targetHeight, true)
    }

    private fun Bitmap.toJpegBytes(quality: Int): ByteArray {
        val output = ByteArrayOutputStream()
        compress(Bitmap.CompressFormat.JPEG, quality, output)
        val bytes = output.toByteArray()
        output.close()
        return bytes
    }

    private suspend fun <T> Task<T>.awaitTask(): T = suspendCancellableCoroutine { continuation ->
        addOnSuccessListener { result ->
            if (continuation.isActive) {
                continuation.resume(result)
            }
        }
        addOnFailureListener { error ->
            if (continuation.isActive) {
                continuation.resumeWithException(error)
            }
        }
        addOnCanceledListener {
            continuation.cancel()
        }
    }

    private companion object {
        const val MAX_IMAGE_DIMENSION = 720
        const val JPEG_QUALITY = 70
        const val CAPTURE_TIMEOUT_MILLIS = 1_500L
    }
}
