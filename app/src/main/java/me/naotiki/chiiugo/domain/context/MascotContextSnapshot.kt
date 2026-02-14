package me.naotiki.chiiugo.domain.context

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.time.Instant

@Serializable
enum class PlaybackStatus {
    NONE,
    PLAYING,
    PAUSED,
    STOPPED
}

@Serializable
enum class MediaKind {
    UNKNOWN,
    MUSIC,
    VIDEO
}

@Serializable
data class NotificationAppSummary(
    val appName: String,
    val category: String?,
    val count: Int
)

@Serializable
data class RecentNotificationSnapshot(
    val appName: String,
    val category: String?,
    val title: String? = null,
    val body: String? = null,
    val postedAtEpochMillis: Long
)

@Serializable
data class MediaPlaybackSnapshot(
    val packageName: String,
    val appName: String,
    val status: PlaybackStatus,
    val mediaKind: MediaKind,
    val title: String? = null,
    val artist: String? = null
)

@Serializable
data class MascotContextSnapshot(
    val notificationCount: Int = 0,
    //val activeNotifications: List<NotificationAppSummary> = emptyList(),
    val recentNotifications: List<RecentNotificationSnapshot> = emptyList(),
    val mediaPlayback: MediaPlaybackSnapshot? = null,
    val updatedAtEpochMillis: Long = System.currentTimeMillis()
) {
    fun toPromptJson(): String {
        return promptJson.encodeToString(
            PromptMascotContextSnapshot(
                notificationCount = notificationCount,
                recentNotifications = recentNotifications.map { it.toPromptSnapshot() },
                mediaPlayback = mediaPlayback,
                updatedAtIso8601 = updatedAtEpochMillis.toIso8601()
            )
        )
    }
}

@Serializable
private data class PromptRecentNotificationSnapshot(
    val appName: String,
    val category: String?,
    val title: String? = null,
    val body: String? = null,
    val postedAtIso8601: String
)

@Serializable
private data class PromptMascotContextSnapshot(
    val notificationCount: Int,
    val recentNotifications: List<PromptRecentNotificationSnapshot>,
    val mediaPlayback: MediaPlaybackSnapshot? = null,
    val updatedAtIso8601: String
)

private fun RecentNotificationSnapshot.toPromptSnapshot(): PromptRecentNotificationSnapshot {
    return PromptRecentNotificationSnapshot(
        appName = appName,
        category = category,
        title = title,
        body = body,
        postedAtIso8601 = postedAtEpochMillis.toIso8601()
    )
}

private fun Long.toIso8601(): String = Instant.fromEpochMilliseconds(this).toString()

private val promptJson = Json {
    encodeDefaults = true
    explicitNulls = false
    prettyPrint = false
}
