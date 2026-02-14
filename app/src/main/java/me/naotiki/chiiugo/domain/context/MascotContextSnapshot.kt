package me.naotiki.chiiugo.domain.context

enum class PlaybackStatus {
    NONE,
    PLAYING,
    PAUSED,
    STOPPED
}

enum class MediaKind {
    UNKNOWN,
    MUSIC,
    VIDEO
}

data class NotificationAppSummary(
    val appName: String,
    val category: String?,
    val count: Int
)

data class MediaPlaybackSnapshot(
    val packageName: String,
    val appName: String,
    val status: PlaybackStatus,
    val mediaKind: MediaKind,
    val title: String? = null,
    val artist: String? = null
)

data class MascotContextSnapshot(
    val notificationCount: Int = 0,
    val activeNotifications: List<NotificationAppSummary> = emptyList(),
    val mediaPlayback: MediaPlaybackSnapshot? = null,
    val updatedAtEpochMillis: Long = System.currentTimeMillis()
) {
    fun toPromptJson(): String {
        val notificationItems = activeNotifications.joinToString(",") {
            buildString {
                append("{\"appName\":\"${it.appName.escapeJson()}\",")
                append("\"category\":")
                if (it.category == null) {
                    append("null,")
                } else {
                    append("\"${it.category.escapeJson()}\",")
                }
                append("\"count\":${it.count}}")
            }
        }

        val mediaJson = mediaPlayback?.let {
            buildString {
                append("{\"packageName\":\"${it.packageName.escapeJson()}\",")
                append("\"appName\":\"${it.appName.escapeJson()}\",")
                append("\"status\":\"${it.status.name}\",")
                append("\"mediaKind\":\"${it.mediaKind.name}\",")
                append("\"title\":")
                if (it.title == null) {
                    append("null,")
                } else {
                    append("\"${it.title.escapeJson()}\",")
                }
                append("\"artist\":")
                if (it.artist == null) {
                    append("null")
                } else {
                    append("\"${it.artist.escapeJson()}\"")
                }
                append("}")
            }
        } ?: "null"

        return buildString {
            append("{")
            append("\"notificationCount\":$notificationCount,")
            append("\"activeNotifications\":[")
            append(notificationItems)
            append("],")
            append("\"mediaPlayback\":$mediaJson,")
            append("\"updatedAtEpochMillis\":$updatedAtEpochMillis")
            append("}")
        }
    }
}

private fun String.escapeJson(): String {
    return this
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
}
