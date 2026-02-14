package me.naotiki.chiiugo.service

import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.view.View
import android.view.WindowManager
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import dagger.hilt.android.AndroidEntryPoint
import me.naotiki.chiiugo.domain.comment.MascotCommentOrchestrator
import me.naotiki.chiiugo.model.rememberAreaSize
import me.naotiki.chiiugo.ui.component.ConfigManager
import me.naotiki.chiiugo.ui.component.Mascot
import me.naotiki.chiiugo.ui.component.rememberMascotState
import javax.inject.Inject

@AndroidEntryPoint
class MascotOverlayService() : LifecycleService(), SavedStateRegistryOwner {

    @Inject
    lateinit var configManager: ConfigManager

    @Inject
    lateinit var mascotCommentOrchestrator: MascotCommentOrchestrator

    companion object {
        private const val START_OVERLAY = "START_OVERLAY"
        private const val STOP_OVERLAY = "STOP_OVERLAY"
        fun createStartOverlayIntent(context: Context): Intent {
            return Intent(context, MascotOverlayService::class.java).apply {
                action = START_OVERLAY
            }
        }

        fun createStopOverlayIntent(context: Context): Intent {
            return Intent(context, MascotOverlayService::class.java).apply {
                action = STOP_OVERLAY
            }
        }

        private val _enable = MutableLiveData<Boolean>(false)
        val enable: LiveData<Boolean> = _enable
        fun setEnable(value: Boolean){
            _enable.postValue(value)
        }
    }

    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    override val savedStateRegistry: SavedStateRegistry
        get() =
            savedStateRegistryController.savedStateRegistry

    private fun createComposeView(): ComposeView {
        return ComposeView(applicationContext).apply {
            setViewTreeSavedStateRegistryOwner(this@MascotOverlayService)
            setContent {
                val density =
                    LocalDensity.current.density

                val config by configManager.configFlow.collectAsState()

                //GifImage(R.drawable.boom)
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

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            START_OVERLAY -> handleStartOverlay()
            STOP_OVERLAY -> handleStopOverlay()
            else -> {}
        }
        return super.onStartCommand(intent, flags, startId)
    }



    override fun onCreate() {
        super.onCreate()

        savedStateRegistryController.performRestore(null)

    }

    private var composeView: ComposeView? = null
    private val windowManager: WindowManager by lazy {
        getSystemService(WINDOW_SERVICE) as WindowManager
    }

    private fun handleStartOverlay() {

        setEnable(true)
        changeRootView(createComposeView().apply {
            setViewTreeLifecycleOwner(this@MascotOverlayService)
        })
    }

    private fun handleStopOverlay() {
        setEnable(false)
        if (composeView == null) return
        windowManager.removeView(composeView)
        composeView = null
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
}


fun WindowManager.LayoutParams.copy(block: WindowManager.LayoutParams.() -> Unit): WindowManager.LayoutParams {
    return WindowManager.LayoutParams().apply {
        copyFrom(this@copy)
        block()
    }
}
