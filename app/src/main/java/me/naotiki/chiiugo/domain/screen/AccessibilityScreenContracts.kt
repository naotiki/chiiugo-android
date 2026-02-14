package me.naotiki.chiiugo.domain.screen

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

data class AccessibilityScreenResult(
    val text: String,
    val appName: String? = null,
    val packageName: String? = null,
    val activityName: String? = null,
    val capturedAtEpochMillis: Long = System.currentTimeMillis()
)

data class ActivityChangeEvent(
    val packageName: String?,
    val activityName: String?,
    val occurredAtEpochMillis: Long = System.currentTimeMillis()
)

interface AccessibilityScreenSource {
    val isCaptureAvailableFlow: StateFlow<Boolean>
    val activityChangeFlow: Flow<ActivityChangeEvent>
    suspend fun captureOnce(): AccessibilityScreenResult?
}

interface AccessibilityScreenController {
    fun setAvailable(available: Boolean)
    fun updateSnapshot(snapshot: AccessibilityScreenResult)
    fun clearSnapshot()
}
