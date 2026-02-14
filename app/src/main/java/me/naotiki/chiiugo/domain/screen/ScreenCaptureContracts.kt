package me.naotiki.chiiugo.domain.screen

import kotlinx.coroutines.flow.StateFlow

data class ScreenCaptureResult(
    val imageJpegBytes: ByteArray,
    val ocrText: String,
    val capturedAtEpochMillis: Long = System.currentTimeMillis()
)

interface ScreenCaptureSource {
    val isCaptureAvailableFlow: StateFlow<Boolean>
    suspend fun captureOnce(): ScreenCaptureResult?
}

interface ScreenCaptureController {
    fun attachManager(manager: ScreenCaptureManager)
    fun clearManager()
}
