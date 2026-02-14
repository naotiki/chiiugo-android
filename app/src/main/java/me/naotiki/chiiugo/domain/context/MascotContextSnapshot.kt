package me.naotiki.chiiugo.domain.context

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

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
        return promptJson.encodeToString(this)
    }
}

private val promptJson = Json {
    encodeDefaults = true
    explicitNulls = false
    prettyPrint = false
}
