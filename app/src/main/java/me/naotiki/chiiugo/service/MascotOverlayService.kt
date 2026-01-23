package me.naotiki.chiiugo.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.view.View
import android.view.WindowManager
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
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
import me.naotiki.chiiugo.R
import me.naotiki.chiiugo.model.rememberAreaSize
import me.naotiki.chiiugo.ui.component.GifImage
import me.naotiki.chiiugo.ui.component.Mascot
import me.naotiki.chiiugo.ui.component.rememberMascotState

class MascotOverlayService() : LifecycleService(), SavedStateRegistryOwner {
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

    private val composeView by lazy {
        ComposeView(applicationContext).apply {
            setViewTreeSavedStateRegistryOwner(this@MascotOverlayService)
            setContent {
                val density =
                    LocalDensity.current.density


                //GifImage(R.drawable.boom)
                val areaSize = rememberAreaSize(with(LocalDensity.current) {
                    LocalConfiguration.current.run {
                        screenWidthDp.toDp() to screenHeightDp.toDp()
                    }
                })
                val mascotState = rememberMascotState(areaSize)

                LaunchedEffect(mascotState.areaPosState) {
                    val posPx = mascotState.areaPosState.toPx(density)
                    windowManager.updateViewLayout(
                        this@apply, overlayParams.copy {
                            x = posPx.first
                            y = posPx.second
                        }
                    )
                }

                Mascot(mascotState)
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

    private var currentView: View? = null
    private val windowManager: WindowManager by lazy {
        getSystemService(WINDOW_SERVICE) as WindowManager
    }

    private fun handleStartOverlay() {
        composeView.setViewTreeLifecycleOwner(this)
        setEnable(true)
        changeRootView(composeView)
    }

    private fun handleStopOverlay() {
        setEnable(false)
        if (currentView == null) return
        windowManager.removeView(currentView)
        currentView = null
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

    private fun changeRootView(view: View) {
        currentView?.run { windowManager.removeView(this) }

        windowManager.addView(view, overlayParams)
        currentView = view
    }
}


fun WindowManager.LayoutParams.copy(block: WindowManager.LayoutParams.() -> Unit): WindowManager.LayoutParams {
    return WindowManager.LayoutParams().apply {
        copyFrom(this@copy)
        block()
    }
}