package me.naotiki.chiiugo.service

import android.content.ComponentName
import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSession
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import dagger.hilt.android.AndroidEntryPoint
import me.naotiki.chiiugo.domain.context.ContextEventRepository
import me.naotiki.chiiugo.domain.context.MediaEventPayload
import me.naotiki.chiiugo.domain.context.MediaKind
import me.naotiki.chiiugo.domain.context.NotificationEventPayload
import me.naotiki.chiiugo.domain.context.PlaybackStatus
import javax.inject.Inject

@AndroidEntryPoint
class MascotContextListenerService : NotificationListenerService() {

    @Inject
    lateinit var contextEventRepository: ContextEventRepository

    private val mediaSessionManager by lazy {
        getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
    }

    private val activeControllers = linkedMapOf<MediaSession.Token, MediaController>()
    private val callbacks = linkedMapOf<MediaSession.Token, MediaController.Callback>()

    private val activeSessionsChangedListener = MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
        updateMediaControllers(controllers.orEmpty())
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        syncActiveNotifications()
        registerMediaSessionListener()
    }

    override fun onListenerDisconnected() {
        unregisterMediaSessionListener()
        super.onListenerDisconnected()
    }

    override fun onDestroy() {
        unregisterMediaSessionListener()
        super.onDestroy()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        val entry = sbn ?: return
        contextEventRepository.onNotificationPosted(
            NotificationEventPayload(
                key = entry.key,
                appName = resolveAppName(entry.packageName),
                category = entry.notification?.category
            )
        )
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        val entry = sbn ?: return
        contextEventRepository.onNotificationRemoved(entry.key)
    }

    private fun syncActiveNotifications() {
        val current = activeNotifications.orEmpty().map {
            NotificationEventPayload(
                key = it.key,
                appName = resolveAppName(it.packageName),
                category = it.notification?.category
            )
        }
        contextEventRepository.replaceActiveNotifications(current)
    }

    private fun registerMediaSessionListener() {
        runCatching {
            val componentName = ComponentName(this, MascotContextListenerService::class.java)
            mediaSessionManager.addOnActiveSessionsChangedListener(
                activeSessionsChangedListener,
                componentName
            )
            updateMediaControllers(mediaSessionManager.getActiveSessions(componentName).orEmpty())
        }.onFailure {
            contextEventRepository.clearMedia()
        }
    }

    private fun unregisterMediaSessionListener() {
        runCatching {
            mediaSessionManager.removeOnActiveSessionsChangedListener(activeSessionsChangedListener)
        }
        clearMediaControllerCallbacks()
        contextEventRepository.clearMedia()
    }

    private fun updateMediaControllers(controllers: List<MediaController>) {
        val nextTokens = controllers.map { it.sessionToken }.toSet()
        val staleTokens = activeControllers.keys.filterNot { nextTokens.contains(it) }
        staleTokens.forEach { token ->
            callbacks.remove(token)?.let { callback ->
                activeControllers[token]?.unregisterCallback(callback)
            }
            activeControllers.remove(token)
        }

        controllers.forEach { controller ->
            val token = controller.sessionToken
            if (activeControllers.containsKey(token)) {
                emitMediaEvent(controller, controller.playbackState, controller.metadata)
                return@forEach
            }

            val callback = object : MediaController.Callback() {
                override fun onPlaybackStateChanged(state: PlaybackState?) {
                    emitMediaEvent(controller, state, controller.metadata)
                }

                override fun onMetadataChanged(metadata: MediaMetadata?) {
                    emitMediaEvent(controller, controller.playbackState, metadata)
                }
            }
            controller.registerCallback(callback)
            callbacks[token] = callback
            activeControllers[token] = controller
            emitMediaEvent(controller, controller.playbackState, controller.metadata)
        }

        if (activeControllers.isEmpty()) {
            contextEventRepository.clearMedia()
        }
    }

    private fun emitMediaEvent(
        controller: MediaController,
        playbackState: PlaybackState?,
        metadata: MediaMetadata?
    ) {
        val packageName = controller.packageName ?: return
        contextEventRepository.onMediaUpdated(
            MediaEventPayload(
                packageName = packageName,
                appName = resolveAppName(packageName),
                status = playbackState.toPlaybackStatus(),
                mediaKind = inferMediaKind(packageName, metadata),
                title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE),
                artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST)
            )
        )
    }

    private fun clearMediaControllerCallbacks() {
        callbacks.forEach { (token, callback) ->
            activeControllers[token]?.unregisterCallback(callback)
        }
        callbacks.clear()
        activeControllers.clear()
    }

    private fun resolveAppName(packageName: String): String {
        return runCatching {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        }.getOrElse {
            packageName
        }
    }

    private fun inferMediaKind(packageName: String, metadata: MediaMetadata?): MediaKind {
        val loweredPackage = packageName.lowercase()
        val title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE)?.lowercase().orEmpty()
        val artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST)?.lowercase().orEmpty()

        val likelyVideo = loweredPackage.contains("youtube") ||
            loweredPackage.contains("netflix") ||
            loweredPackage.contains("abema") ||
            loweredPackage.contains("primevideo") ||
            loweredPackage.contains("video")

        if (likelyVideo) {
            return MediaKind.VIDEO
        }
        if (artist.isNotBlank() || title.contains("official")) {
            return MediaKind.MUSIC
        }
        return MediaKind.UNKNOWN
    }

    private fun PlaybackState?.toPlaybackStatus(): PlaybackStatus {
        return when (this?.state) {
            PlaybackState.STATE_PLAYING -> PlaybackStatus.PLAYING
            PlaybackState.STATE_PAUSED -> PlaybackStatus.PAUSED
            PlaybackState.STATE_STOPPED,
            PlaybackState.STATE_NONE,
            PlaybackState.STATE_ERROR -> PlaybackStatus.STOPPED

            null -> PlaybackStatus.NONE
            else -> PlaybackStatus.STOPPED
        }
    }
}
