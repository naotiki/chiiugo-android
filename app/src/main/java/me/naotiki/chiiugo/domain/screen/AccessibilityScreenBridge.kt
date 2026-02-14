package me.naotiki.chiiugo.domain.screen

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

class AccessibilityScreenBridge : AccessibilityScreenSource, AccessibilityScreenController {
    private val _isCaptureAvailable = MutableStateFlow(false)
    override val isCaptureAvailableFlow: StateFlow<Boolean> = _isCaptureAvailable.asStateFlow()

    private val _activityChangeFlow = MutableSharedFlow<ActivityChangeEvent>(
        replay = 0,
        extraBufferCapacity = 16
    )
    override val activityChangeFlow: Flow<ActivityChangeEvent> = _activityChangeFlow.asSharedFlow()

    @Volatile
    private var latestSnapshot: AccessibilityScreenResult? = null
    @Volatile
    private var lastActivityKey: String? = null

    override fun setAvailable(available: Boolean) {
        _isCaptureAvailable.value = available
        if (!available) {
            clearSnapshot()
        }
    }

    override fun updateSnapshot(snapshot: AccessibilityScreenResult) {
        latestSnapshot = snapshot

        val activityKey = "${snapshot.packageName.orEmpty()}|${snapshot.activityName.orEmpty()}"
        if (activityKey == "|") {
            return
        }
        if (activityKey == lastActivityKey) {
            return
        }
        lastActivityKey = activityKey
        _activityChangeFlow.tryEmit(
            ActivityChangeEvent(
                packageName = snapshot.packageName,
                activityName = snapshot.activityName,
                occurredAtEpochMillis = snapshot.capturedAtEpochMillis
            )
        )
    }

    override fun clearSnapshot() {
        latestSnapshot = null
        lastActivityKey = null
    }

    override suspend fun captureOnce(): AccessibilityScreenResult? {
        return latestSnapshot
    }
}
