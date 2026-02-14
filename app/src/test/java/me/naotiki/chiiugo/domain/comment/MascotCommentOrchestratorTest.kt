package me.naotiki.chiiugo.domain.comment

import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import me.naotiki.chiiugo.data.llm.LlmSettings
import me.naotiki.chiiugo.data.llm.LlmSettingsRepository
import me.naotiki.chiiugo.domain.context.ContextEventRepository
import me.naotiki.chiiugo.domain.context.MascotContextSnapshot
import me.naotiki.chiiugo.domain.context.NotificationEventPayload
import org.junit.Assert.assertEquals
import org.junit.Test

class MascotCommentOrchestratorTest {

    @Test
    fun `respects cooldown and emits once during cooldown window`() = runTest {
        val settingsRepository = FakeLlmSettingsRepository(
            LlmSettings(enabled = true, cooldownSec = 1)
        )
        val contextRepository = ContextEventRepository()
        val generated = mutableListOf<String>()
        val orchestrator = MascotCommentOrchestrator(
            contextEventRepository = contextRepository,
            llmSettingsRepository = settingsRepository,
            commentGenerator = object : MascotCommentGenerator {
                override suspend fun generate(
                    snapshot: MascotContextSnapshot,
                    settings: LlmSettings
                ): String = "test-comment"
            }
        )

        val job: Job = launch {
            orchestrator.run { text -> generated += text }
        }

        contextRepository.onNotificationPosted(
            NotificationEventPayload(
                key = "n1",
                appName = "Sample",
                category = "msg"
            )
        )
        Thread.sleep(250)
        contextRepository.onNotificationPosted(
            NotificationEventPayload(
                key = "n2",
                appName = "Sample",
                category = "msg"
            )
        )
        Thread.sleep(250)

        assertEquals(1, generated.size)

        Thread.sleep(1100)
        contextRepository.onNotificationPosted(
            NotificationEventPayload(
                key = "n3",
                appName = "Sample",
                category = "msg"
            )
        )
        Thread.sleep(250)

        assertEquals(2, generated.size)

        job.cancel()
    }

    private class FakeLlmSettingsRepository(initial: LlmSettings) : LlmSettingsRepository {
        private val mutableState = MutableStateFlow(initial)
        override val settingsFlow: Flow<LlmSettings> = mutableState.asStateFlow()
        override suspend fun updateEnabled(enabled: Boolean) {}
        override suspend fun updateBaseUrl(baseUrl: String) {}
        override suspend fun updateModel(model: String) {}
        override suspend fun updateCooldownSec(cooldownSec: Int) {}
        override suspend fun updateMaxTokens(maxTokens: Int) {}
        override suspend fun updateTemperature(temperature: Float) {}
        override suspend fun updatePersonaStyle(personaStyle: String) {}
        override suspend fun saveApiKey(apiKey: String) {}
        override suspend fun hasApiKey(): Boolean = false
        override suspend fun getApiKeyOrNull(): String? = null
    }
}
