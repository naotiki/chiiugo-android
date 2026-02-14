package me.naotiki.chiiugo.domain.comment

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.runTest
import me.naotiki.chiiugo.data.llm.LlmSettings
import me.naotiki.chiiugo.data.llm.LlmSettingsRepository
import me.naotiki.chiiugo.data.llm.ScreenAnalysisMode
import me.naotiki.chiiugo.domain.context.MascotContextSnapshot
import org.junit.Assert.assertEquals
import org.junit.Test

class KoogMascotCommentGeneratorTest {

    @Test
    fun `uses generated sentence when client succeeds`() = runTest {
        val repository = FakeLlmSettingsRepository()
        val generator = KoogMascotCommentGenerator(
            llmSettingsRepository = repository,
            koogPromptClient = FakeKoogPromptClient(result = "今いい感じ。次もいける！")
        )

        val output = generator.generate(
            snapshot = MascotContextSnapshot(notificationCount = 1),
            settings = LlmSettings()
        )

        assertEquals("今いい感じ。", output)
    }

    @Test
    fun `returns empty when client throws`() = runTest {
        val repository = FakeLlmSettingsRepository()
        val generator = KoogMascotCommentGenerator(
            llmSettingsRepository = repository,
            koogPromptClient = FakeKoogPromptClient(error = IllegalStateException("boom"))
        )

        val output = generator.generate(
            snapshot = MascotContextSnapshot(notificationCount = 2),
            settings = LlmSettings()
        )

        assertEquals("", output)
    }

    @Test
    fun `generates single sentence from screen prompt`() = runTest {
        val repository = FakeLlmSettingsRepository()
        val generator = KoogMascotCommentGenerator(
            llmSettingsRepository = repository,
            koogPromptClient = FakeKoogPromptClient(screenResult = "表示は順調です。つぎも進めます。")
        )

        val output = generator.generateScreenComment(
            input = ScreenPromptInput(ocrText = "sample text"),
            settings = LlmSettings()
        )

        assertEquals("表示は順調です。", output)
    }

    private class FakeKoogPromptClient(
        private val result: String = "",
        private val screenResult: String = "",
        private val error: Throwable? = null
    ) : KoogPromptClient {
        override suspend fun generate(
            settings: LlmSettings,
            snapshot: MascotContextSnapshot,
            apiKey: String?
        ): String {
            error?.let { throw it }
            return result
        }

        override suspend fun generateScreenComment(
            settings: LlmSettings,
            input: ScreenPromptInput,
            apiKey: String?
        ): String {
            error?.let { throw it }
            return screenResult
        }
    }

    private class FakeLlmSettingsRepository : LlmSettingsRepository {
        private val mutableState = MutableStateFlow(LlmSettings())

        override val settingsFlow: Flow<LlmSettings> = mutableState.asStateFlow()
        override suspend fun updateEnabled(enabled: Boolean) {}
        override suspend fun updateBaseUrl(baseUrl: String) {}
        override suspend fun updateModel(model: String) {}
        override suspend fun updateCooldownSec(cooldownSec: Int) {}
        override suspend fun updateMaxTokens(maxTokens: Int) {}
        override suspend fun updateTemperature(temperature: Float) {}
        override suspend fun updatePersonaStyle(personaStyle: String) {}
        override suspend fun updateScreenAnalysisEnabled(enabled: Boolean) {}
        override suspend fun updateAnalysisMode(mode: ScreenAnalysisMode) {}
        override suspend fun updateScreenCaptureIntervalSec(intervalSec: Int) {}
        override suspend fun saveApiKey(apiKey: String) {}
        override suspend fun hasApiKey(): Boolean = false
        override suspend fun getApiKeyOrNull(): String? = null
    }
}
