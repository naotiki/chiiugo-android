package me.naotiki.chiiugo.domain.comment

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.transformLatest
import me.naotiki.chiiugo.domain.context.ContextEvent
import me.naotiki.chiiugo.data.llm.LlmSettingsRepository
import me.naotiki.chiiugo.domain.context.ContextEventRepository
import me.naotiki.chiiugo.domain.context.MascotContextSnapshot
import me.naotiki.chiiugo.domain.context.NotificationContextEvent
import me.naotiki.chiiugo.ui.component.MascotState
import me.naotiki.chiiugo.ui.component.texts
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MascotCommentOrchestrator @Inject constructor(
    private val contextEventRepository: ContextEventRepository,
    private val llmSettingsRepository: LlmSettingsRepository,
    private val commentGenerator: MascotCommentGenerator
) {
    suspend fun run(mascotState: MascotState) {
        run { text ->
            mascotState.say(text, delayMillis = 5000, important = true)
        }
    }

    suspend fun run(speak: suspend (String) -> Unit) {
        var lastGeneratedAt = 0L

        contextEventRepository.events.transformLatest { event ->
            val settings = llmSettingsRepository.settingsFlow.first()
            val now = System.currentTimeMillis()
            val cooldownMillis = settings.cooldownSec * 1000L
            if (now - lastGeneratedAt < cooldownMillis) {
                return@transformLatest
            }

            val comment = if (settings.enabled) {
                val snapshot = contextEventRepository.snapshotFlow.value.withEventFocus(event)
                commentGenerator.generate(snapshot, settings)
            } else {
                texts.random()
            }.trim()

            if (comment.isNotBlank()) {
                emit(comment)
                lastGeneratedAt = now
            }
        }.mapNotNull { it.takeIf(String::isNotBlank) }
            .collect { text ->
                speak(text)
            }
    }
}

private fun MascotContextSnapshot.withEventFocus(event: ContextEvent): MascotContextSnapshot {
    val latestNotification = (event as? NotificationContextEvent)?.latestNotification ?: return this
    return copy(recentNotifications = listOf(latestNotification))
}
