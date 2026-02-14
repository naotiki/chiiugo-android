package me.naotiki.chiiugo.domain.context

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNull
import org.junit.Test

class ContextEventRepositoryTest {

    @Test
    fun `aggregates notification summary by app and category`() = runTest {
        val repository = ContextEventRepository()

        repository.onNotificationPosted(
            NotificationEventPayload(
                key = "a",
                appName = "YouTube",
                category = "social",
                title = "動画の通知",
                body = "最新アップロードがあります"
            )
        )
        repository.onNotificationPosted(
            NotificationEventPayload(
                key = "b",
                appName = "YouTube",
                category = "social"
            )
        )
        repository.onNotificationPosted(
            NotificationEventPayload(
                key = "c",
                appName = "Gmail",
                category = "email"
            )
        )

        Thread.sleep(200)

        val snapshot = repository.snapshotFlow.value
        assertEquals(3, snapshot.notificationCount)
        assertEquals(3, snapshot.recentNotifications.size)
        assertTrue(snapshot.recentNotifications.any { it.appName == "YouTube" })
        assertTrue(snapshot.recentNotifications.any { it.appName == "Gmail" })
        assertTrue(snapshot.toPromptJson().contains("最新アップロードがあります"))
        assertTrue(snapshot.recentNotifications.isNotEmpty())
    }

    @Test
    fun `replaces media snapshot and clears it`() = runTest {
        val repository = ContextEventRepository()

        repository.onMediaUpdated(
            MediaEventPayload(
                packageName = "com.test.music",
                appName = "Music",
                status = PlaybackStatus.PLAYING,
                mediaKind = MediaKind.MUSIC,
                title = "Song",
                artist = "Artist"
            )
        )
        Thread.sleep(100)
        assertEquals(PlaybackStatus.PLAYING, repository.snapshotFlow.value.mediaPlayback?.status)

        repository.clearMedia()
        Thread.sleep(100)
        assertNull(repository.snapshotFlow.value.mediaPlayback)
    }
}
