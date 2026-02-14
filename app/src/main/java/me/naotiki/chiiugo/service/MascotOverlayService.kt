package me.naotiki.chiiugo.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.Build
import android.view.WindowManager
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import me.naotiki.chiiugo.R
import me.naotiki.chiiugo.data.llm.LlmSettingsRepository
import me.naotiki.chiiugo.data.llm.ScreenAnalysisMode
import me.naotiki.chiiugo.domain.comment.MascotCommentOrchestrator
import me.naotiki.chiiugo.domain.screen.ScreenCaptureController
import me.naotiki.chiiugo.domain.screen.ScreenCaptureManager
import me.naotiki.chiiugo.model.rememberAreaSize
import me.naotiki.chiiugo.ui.component.ConfigManager
import me.naotiki.chiiugo.ui.component.Mascot
import me.naotiki.chiiugo.ui.component.rememberMascotState
import javax.inject.Inject

@AndroidEntryPoint
class MascotOverlayService : LifecycleService(), SavedStateRegistryOwner {

    @Inject
    lateinit var configManager: ConfigManager

    @Inject
    lateinit var mascotCommentOrchestrator: MascotCommentOrchestrator

    @Inject
    lateinit var llmSettingsRepository: LlmSettingsRepository

    @Inject
    lateinit var screenCaptureManager: ScreenCaptureManager

    @Inject
    lateinit var screenCaptureController: ScreenCaptureController

    companion object {
        private const val START_OVERLAY = "START_OVERLAY"
        private const val STOP_OVERLAY = "STOP_OVERLAY"
        private const val EXTRA_CAPTURE_RESULT_CODE = "extra_capture_result_code"
        private const val EXTRA_CAPTURE_DATA = "extra_capture_data"
        private const val FOREGROUND_CHANNEL_ID = "screen_capture_channel"
        private const val FOREGROUND_NOTIFICATION_ID = 41001

        fun createStartOverlayIntent(
            context: Context,
            captureResultCode: Int? = null,
            captureData: Intent? = null
        ): Intent {
            return Intent(context, MascotOverlayService::class.java).apply {
                action = START_OVERLAY
                if (captureResultCode != null && captureData != null) {
                    putExtra(EXTRA_CAPTURE_RESULT_CODE, captureResultCode)
                    putExtra(EXTRA_CAPTURE_DATA, captureData)
                }
            }
        }

        fun createStopOverlayIntent(context: Context): Intent {
            return Intent(context, MascotOverlayService::class.java).apply {
                action = STOP_OVERLAY
            }
        }

        private val _enable = MutableLiveData(false)
        val enable: LiveData<Boolean> = _enable
        fun setEnable(value: Boolean) {
            _enable.postValue(value)
        }
    }

    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    private var composeView: ComposeView? = null
    private val windowManager: WindowManager by lazy {
        getSystemService(WINDOW_SERVICE) as WindowManager
    }
    private var screenCaptureJob: Job? = null

    private fun createComposeView(): ComposeView {
        return ComposeView(applicationContext).apply {
            setViewTreeSavedStateRegistryOwner(this@MascotOverlayService)
            setContent {
                val density = LocalDensity.current.density
                val config by configManager.configFlow.collectAsState()
                val areaSize = rememberAreaSize(with(LocalDensity.current) {
                    LocalConfiguration.current.run {
                        screenWidthDp.toDp() to screenHeightDp.toDp()
                    }
                })
                val mascotState = rememberMascotState(areaSize) { configManager.conf }

                LaunchedEffect(mascotState.areaPosState, config.transparency) {
                    val posPx = mascotState.areaPosState.toPx(density)
                    windowManager.updateViewLayout(
                        this@apply, overlayParams.copy {
                            x = posPx.first
                            y = posPx.second
                            alpha = config.transparency
                        }
                    )
                }
                LaunchedEffect(mascotState) {
                    mascotCommentOrchestrator.run(mascotState)
                }

                Mascot(mascotState, configData = config)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
    }

    override fun onDestroy() {
        stopScreenCaptureSession()
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            START_OVERLAY -> handleStartOverlay(intent)
            STOP_OVERLAY -> handleStopOverlay()
            else -> {}
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun handleStartOverlay(intent: Intent?) {
        setEnable(true)
        changeRootView(createComposeView().apply {
            setViewTreeLifecycleOwner(this@MascotOverlayService)
        })
        startScreenCaptureSessionIfNeeded(intent)
    }

    private fun handleStopOverlay() {
        setEnable(false)
        composeView?.let { view ->
            windowManager.removeView(view)
            composeView = null
        }
        stopScreenCaptureSession()
    }

    private fun startScreenCaptureSessionIfNeeded(intent: Intent?) {
        screenCaptureJob?.cancel()
        screenCaptureJob = lifecycleScope.launch {
            resetScreenCaptureSession()

            val resultCode = intent?.getIntExtra(EXTRA_CAPTURE_RESULT_CODE, Int.MIN_VALUE) ?: Int.MIN_VALUE
            val projectionData = intent?.getParcelableIntentExtra(EXTRA_CAPTURE_DATA)
            if (resultCode == Int.MIN_VALUE || projectionData == null) {
                return@launch
            }

            val settings = llmSettingsRepository.settingsFlow.first()
            if (!settings.enabled || !settings.screenAnalysisEnabled || settings.analysisMode == ScreenAnalysisMode.OFF) {
                return@launch
            }

            startScreenCaptureForeground()
            val started = screenCaptureManager.start(resultCode, projectionData)
            if (started) {
                screenCaptureController.attachManager(screenCaptureManager)
            } else {
                stopForeground(STOP_FOREGROUND_REMOVE)
            }
        }
    }

    private fun stopScreenCaptureSession() {
        screenCaptureJob?.cancel()
        screenCaptureJob = null
        lifecycleScope.launch {
            resetScreenCaptureSession()
        }
    }

    private suspend fun resetScreenCaptureSession() {
        screenCaptureController.clearManager()
        screenCaptureManager.stop()
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    private fun startScreenCaptureForeground() {
        ensureForegroundChannel()
        val notification = NotificationCompat.Builder(this, FOREGROUND_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("ちぃうご")
            .setContentText("画面解析を実行中")
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                FOREGROUND_NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            )
        } else {
            startForeground(FOREGROUND_NOTIFICATION_ID, notification)
        }
    }

    private fun ensureForegroundChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }
        val manager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            FOREGROUND_CHANNEL_ID,
            "画面解析",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "画面解析実行中の通知"
        }
        manager.createNotificationChannel(channel)
    }

    private val overlayParams = WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
        PixelFormat.TRANSPARENT
    ).apply {
        alpha = 0.8f
    }

    private fun changeRootView(view: ComposeView) {
        composeView?.let { windowManager.removeView(it) }
        windowManager.addView(view, overlayParams)
        composeView = view
    }

    @Suppress("DEPRECATION")
    private fun Intent.getParcelableIntentExtra(name: String): Intent? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getParcelableExtra(name, Intent::class.java)
        } else {
            getParcelableExtra(name)
        }
    }

}

fun WindowManager.LayoutParams.copy(block: WindowManager.LayoutParams.() -> Unit): WindowManager.LayoutParams {
    return WindowManager.LayoutParams().apply {
        copyFrom(this@copy)
        block()
    }
}
