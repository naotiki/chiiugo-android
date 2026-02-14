package me.naotiki.chiiugo.domain.context

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
                category = "social"
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
        assertEquals("YouTube", snapshot.activeNotifications.first().appName)
        assertEquals(2, snapshot.activeNotifications.first().count)
        assertFalse(snapshot.toPromptJson().contains("body", ignoreCase = true))
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
