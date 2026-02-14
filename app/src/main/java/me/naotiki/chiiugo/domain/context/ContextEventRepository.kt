package me.naotiki.chiiugo.domain.context

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

sealed interface ContextEvent {
    val timestampMillis: Long
}

data class NotificationContextEvent(
    val totalNotificationCount: Int,
    override val timestampMillis: Long = System.currentTimeMillis()
) : ContextEvent

data class MediaContextEvent(
    val playbackStatus: PlaybackStatus,
    val mediaKind: MediaKind,
    override val timestampMillis: Long = System.currentTimeMillis()
) : ContextEvent

data class NotificationEventPayload(
    val key: String,
    val appName: String,
    val category: String?,
    val title: String? = null,
    val body: String? = null,
    val postedAtEpochMillis: Long = System.currentTimeMillis()
)

data class MediaEventPayload(
    val packageName: String,
    val appName: String,
    val status: PlaybackStatus,
    val mediaKind: MediaKind,
    val title: String?,
    val artist: String?
)

private data class NotificationEntry(
    val appName: String,
    val category: String?,
    val title: String?,
    val body: String?,
    val postedAtEpochMillis: Long
)

@Singleton
class ContextEventRepository @Inject constructor() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val notifications = linkedMapOf<String, NotificationEntry>()

    private val _snapshotFlow = MutableStateFlow(MascotContextSnapshot())
    val snapshotFlow: StateFlow<MascotContextSnapshot> = _snapshotFlow.asStateFlow()

    private val _events = MutableSharedFlow<ContextEvent>(
        replay = 0,
        extraBufferCapacity = 32,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val events: SharedFlow<ContextEvent> = _events.asSharedFlow()

    fun onNotificationPosted(payload: NotificationEventPayload) {
        scope.launch {
            notifications[payload.key] = NotificationEntry(
                appName = payload.appName,
                category = payload.category,
                title = payload.title,
                body = payload.body,
                postedAtEpochMillis = payload.postedAtEpochMillis
            )
            publishNotificationUpdate()
        }
    }

    fun onNotificationRemoved(key: String) {
        scope.launch {
            if (notifications.remove(key) != null) {
                publishNotificationUpdate()
            }
        }
    }

    fun replaceActiveNotifications(payloads: List<NotificationEventPayload>) {
        scope.launch {
            notifications.clear()
            payloads.forEach { payload ->
                notifications[payload.key] = NotificationEntry(
                    appName = payload.appName,
                    category = payload.category,
                    title = payload.title,
                    body = payload.body,
                    postedAtEpochMillis = payload.postedAtEpochMillis
                )
            }
            publishNotificationUpdate()
        }
    }

    fun onMediaUpdated(payload: MediaEventPayload) {
        scope.launch {
            _snapshotFlow.value = _snapshotFlow.value.copy(
                mediaPlayback = MediaPlaybackSnapshot(
                    packageName = payload.packageName,
                    appName = payload.appName,
                    status = payload.status,
                    mediaKind = payload.mediaKind,
                    title = payload.title,
                    artist = payload.artist
                ),
                updatedAtEpochMillis = System.currentTimeMillis()
            )
            _events.emit(
                MediaContextEvent(
                    playbackStatus = payload.status,
                    mediaKind = payload.mediaKind
                )
            )
        }
    }

    fun clearMedia() {
        scope.launch {
            _snapshotFlow.value = _snapshotFlow.value.copy(
                mediaPlayback = null,
                updatedAtEpochMillis = System.currentTimeMillis()
            )
            _events.emit(
                MediaContextEvent(
                    playbackStatus = PlaybackStatus.NONE,
                    mediaKind = MediaKind.UNKNOWN
                )
            )
        }
    }

    private suspend fun publishNotificationUpdate() {
        val recentNotifications = notifications.values
            .sortedByDescending { it.postedAtEpochMillis }
            .take(8)
            .map { entry ->
                RecentNotificationSnapshot(
                    appName = entry.appName,
                    category = entry.category,
                    title = entry.title,
                    body = entry.body,
                    postedAtEpochMillis = entry.postedAtEpochMillis
                )
            }

        _snapshotFlow.value = _snapshotFlow.value.copy(
            notificationCount = notifications.size,
            recentNotifications = recentNotifications,
            updatedAtEpochMillis = System.currentTimeMillis()
        )

        _events.emit(NotificationContextEvent(totalNotificationCount = notifications.size))
    }
}
