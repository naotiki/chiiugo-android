package me.naotiki.chiiugo.domain.screen

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ScreenCaptureBridge : ScreenCaptureSource, ScreenCaptureController {
    private val _isCaptureAvailable = MutableStateFlow(false)
    override val isCaptureAvailableFlow: StateFlow<Boolean> = _isCaptureAvailable.asStateFlow()

    @Volatile
    private var manager: ScreenCaptureManager? = null

    override fun attachManager(manager: ScreenCaptureManager) {
        this.manager = manager
        _isCaptureAvailable.value = true
    }

    override fun clearManager() {
        manager = null
        _isCaptureAvailable.value = false
    }

    override suspend fun captureOnce(): ScreenCaptureResult? {
        return manager?.captureOnce()
    }
}
