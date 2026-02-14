package me.naotiki.chiiugo.domain.comment

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import me.naotiki.chiiugo.data.llm.LlmSettings
import me.naotiki.chiiugo.data.llm.LlmSettingsRepository
import me.naotiki.chiiugo.data.llm.ScreenAnalysisMode
import me.naotiki.chiiugo.domain.context.ContextEventRepository
import me.naotiki.chiiugo.domain.context.MascotContextSnapshot
import me.naotiki.chiiugo.domain.context.NotificationEventPayload
import me.naotiki.chiiugo.domain.screen.AccessibilityScreenResult
import me.naotiki.chiiugo.domain.screen.AccessibilityScreenSource
import me.naotiki.chiiugo.domain.screen.ActivityChangeEvent
import me.naotiki.chiiugo.domain.screen.ScreenCaptureResult
import me.naotiki.chiiugo.domain.screen.ScreenCaptureSource
import me.naotiki.chiiugo.ui.component.texts
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Collections

class MascotCommentOrchestratorTest {

    @Test
    fun `respects cooldown and emits once during cooldown window on event mode`() = runBlocking {
        val settingsRepository = FakeLlmSettingsRepository(
            LlmSettings(
                enabled = true,
                cooldownSec = 1,
                screenAnalysisEnabled = false,
                analysisMode = ScreenAnalysisMode.MULTIMODAL_ONLY
            )
        )
        val contextRepository = ContextEventRepository()
        val generated = Collections.synchronizedList(mutableListOf<String>())
        val generator = ConfigurableCommentGenerator(
            eventResponse = "test-comment",
            screenResponder = { _, _ -> "screen-comment" }
        )
        val orchestrator = MascotCommentOrchestrator(
            contextEventRepository = contextRepository,
            llmSettingsRepository = settingsRepository,
            commentGenerator = generator,
            screenCaptureSource = FakeScreenCaptureSource(available = false),
            accessibilityScreenSource = FakeAccessibilityScreenSource(available = false)
        )

        val job: Job = launch(Dispatchers.Default) {
            orchestrator.run { text -> generated += text }
        }
        delay(200)

        contextRepository.onNotificationPosted(
            NotificationEventPayload(
                key = "n1",
                appName = "Sample",
                category = "msg"
            )
        )
        waitForCondition(timeoutMillis = 2_000L) { generated.size >= 1 }
        contextRepository.onNotificationPosted(
            NotificationEventPayload(
                key = "n2",
                appName = "Sample",
                category = "msg"
            )
        )

        assertEquals(1, generated.size)

        delay(1_100)
        contextRepository.onNotificationPosted(
            NotificationEventPayload(
                key = "n3",
                appName = "Sample",
                category = "msg"
            )
        )
        waitForCondition(timeoutMillis = 2_000L) { generated.size >= 2 }

        assertEquals(2, generated.size)
        job.cancelAndJoin()
    }

    @Test
    fun `uses built-in random text when llm is disabled`() = runBlocking {
        val settingsRepository = FakeLlmSettingsRepository(
            LlmSettings(
                enabled = false,
                cooldownSec = 1,
                screenAnalysisEnabled = true,
                analysisMode = ScreenAnalysisMode.ACCESSIBILITY_ONLY
            )
        )
        val contextRepository = ContextEventRepository()
        val generated = Collections.synchronizedList(mutableListOf<String>())
        val orchestrator = MascotCommentOrchestrator(
            contextEventRepository = contextRepository,
            llmSettingsRepository = settingsRepository,
            commentGenerator = ConfigurableCommentGenerator(
                eventResponse = "",
                screenResponder = { _, _ -> "" }
            ),
            screenCaptureSource = FakeScreenCaptureSource(available = false),
            accessibilityScreenSource = FakeAccessibilityScreenSource(available = false)
        )

        val job: Job = launch(Dispatchers.Default) {
            orchestrator.run { text -> generated += text }
        }
        delay(200)

        contextRepository.onNotificationPosted(
            NotificationEventPayload(
                key = "llm-off-random-1",
                appName = "Sample",
                category = "msg"
            )
        )
        waitForCondition(timeoutMillis = 2_000L) { generated.isNotEmpty() }

        assertTrue(texts.contains(generated.first()))
        job.cancelAndJoin()
    }

    @Test
    fun `falls back to OCR when multimodal screen generation is blank`() = runBlocking {
        val settingsRepository = FakeLlmSettingsRepository(
            LlmSettings(
                enabled = true,
                screenAnalysisEnabled = true,
                analysisMode = ScreenAnalysisMode.MULTIMODAL_ONLY,
                screenCaptureIntervalSec = 30
            )
        )
        val generated = Collections.synchronizedList(mutableListOf<String>())
        val receivedScreenInputs = Collections.synchronizedList(mutableListOf<ScreenPromptInput>())
        val contextRepository = ContextEventRepository()
        val generator = ConfigurableCommentGenerator(
            eventResponse = "",
            screenResponder = { input, _ ->
                receivedScreenInputs += input
                if (input.sourceType == ScreenPromptSourceType.IMAGE) "" else "ocr-comment"
            }
        )
        val orchestrator = MascotCommentOrchestrator(
            contextEventRepository = contextRepository,
            llmSettingsRepository = settingsRepository,
            commentGenerator = generator,
            screenCaptureSource = FakeScreenCaptureSource(
                available = true,
                result = ScreenCaptureResult(
                    imageJpegBytes = byteArrayOf(1, 2, 3),
                    ocrText = "OCR_RESULT"
                )
            ),
            accessibilityScreenSource = FakeAccessibilityScreenSource(available = false)
        )

        val job: Job = launch(Dispatchers.Default) {
            orchestrator.run { text -> generated += text }
        }

        waitForCondition(timeoutMillis = 2_000L) { generated.isNotEmpty() }

        assertEquals("ocr-comment", generated.first())
        assertEquals(2, receivedScreenInputs.size)
        assertEquals(ScreenPromptSourceType.IMAGE, receivedScreenInputs.first().sourceType)
        assertEquals(ScreenPromptSourceType.OCR_TEXT, receivedScreenInputs.last().sourceType)
        assertEquals("OCR_RESULT", receivedScreenInputs.last().ocrText)
        job.cancelAndJoin()
    }

    @Test
    fun `uses fallback template when OCR mode has empty OCR text`() = runBlocking {
        val settingsRepository = FakeLlmSettingsRepository(
            LlmSettings(
                enabled = true,
                screenAnalysisEnabled = true,
                analysisMode = ScreenAnalysisMode.OCR_ONLY,
                screenCaptureIntervalSec = 30
            )
        )
        val generated = Collections.synchronizedList(mutableListOf<String>())
        val contextRepository = ContextEventRepository()
        val orchestrator = MascotCommentOrchestrator(
            contextEventRepository = contextRepository,
            llmSettingsRepository = settingsRepository,
            commentGenerator = ConfigurableCommentGenerator(
                eventResponse = "",
                screenResponder = { _, _ -> "should-not-be-used" }
            ),
            screenCaptureSource = FakeScreenCaptureSource(
                available = true,
                result = ScreenCaptureResult(
                    imageJpegBytes = byteArrayOf(7, 8, 9),
                    ocrText = "   "
                )
            ),
            accessibilityScreenSource = FakeAccessibilityScreenSource(available = false)
        )

        val job: Job = launch(Dispatchers.Default) {
            orchestrator.run { text -> generated += text }
        }

        waitForCondition(timeoutMillis = 2_000L) { generated.isNotEmpty() }
        assertEquals("画面の文字が読み取れなかったから、次でもう一回見るね。", generated.first())
        job.cancelAndJoin()
    }

    @Test
    fun `falls back to event mode when screen capture is unavailable`() = runBlocking {
        val settingsRepository = FakeLlmSettingsRepository(
            LlmSettings(
                enabled = true,
                cooldownSec = 1,
                screenAnalysisEnabled = true,
                analysisMode = ScreenAnalysisMode.MULTIMODAL_ONLY
            )
        )
        val contextRepository = ContextEventRepository()
        val generated = Collections.synchronizedList(mutableListOf<String>())
        val orchestrator = MascotCommentOrchestrator(
            contextEventRepository = contextRepository,
            llmSettingsRepository = settingsRepository,
            commentGenerator = ConfigurableCommentGenerator(
                eventResponse = "event-comment",
                screenResponder = { _, _ -> "screen-comment" }
            ),
            screenCaptureSource = FakeScreenCaptureSource(available = false),
            accessibilityScreenSource = FakeAccessibilityScreenSource(available = false)
        )

        val job: Job = launch(Dispatchers.Default) {
            orchestrator.run { text -> generated += text }
        }
        delay(200)

        contextRepository.onNotificationPosted(
            NotificationEventPayload(
                key = "fallback-n1",
                appName = "Sample",
                category = "msg"
            )
        )
        waitForCondition(timeoutMillis = 2_000L) { generated.isNotEmpty() }
        assertEquals("event-comment", generated.first())
        job.cancelAndJoin()
    }

    @Test
    fun `keeps event comments enabled while screen analysis is active`() = runBlocking {
        val settingsRepository = FakeLlmSettingsRepository(
            LlmSettings(
                enabled = true,
                cooldownSec = 1,
                screenAnalysisEnabled = true,
                analysisMode = ScreenAnalysisMode.MULTIMODAL_ONLY,
                screenCaptureIntervalSec = 30
            )
        )
        val contextRepository = ContextEventRepository()
        val generated = Collections.synchronizedList(mutableListOf<String>())
        val orchestrator = MascotCommentOrchestrator(
            contextEventRepository = contextRepository,
            llmSettingsRepository = settingsRepository,
            commentGenerator = ConfigurableCommentGenerator(
                eventResponse = "event-comment",
                screenResponder = { input, _ ->
                    if (input.sourceType == ScreenPromptSourceType.IMAGE) {
                        "screen-comment"
                    } else {
                        "screen-ocr-comment"
                    }
                }
            ),
            screenCaptureSource = FakeScreenCaptureSource(
                available = true,
                result = ScreenCaptureResult(
                    imageJpegBytes = byteArrayOf(1, 2, 3),
                    ocrText = "OCR_RESULT"
                )
            ),
            accessibilityScreenSource = FakeAccessibilityScreenSource(available = false)
        )

        val job: Job = launch(Dispatchers.Default) {
            orchestrator.run { text -> generated += text }
        }

        waitForCondition(timeoutMillis = 2_000L) { generated.contains("screen-comment") }
        contextRepository.onNotificationPosted(
            NotificationEventPayload(
                key = "event-active-screen-1",
                appName = "Sample",
                category = "msg"
            )
        )
        waitForCondition(timeoutMillis = 2_000L) { generated.contains("event-comment") }

        assertTrue(generated.contains("screen-comment"))
        assertTrue(generated.contains("event-comment"))
        job.cancelAndJoin()
    }

    @Test
    fun `emits periodic comment in accessibility mode`() = runBlocking {
        val settingsRepository = FakeLlmSettingsRepository(
            LlmSettings(
                enabled = true,
                screenAnalysisEnabled = true,
                analysisMode = ScreenAnalysisMode.ACCESSIBILITY_ONLY,
                screenCaptureIntervalSec = 30
            )
        )
        val generated = Collections.synchronizedList(mutableListOf<String>())
        val contextRepository = ContextEventRepository()
        val accessibilitySource = FakeAccessibilityScreenSource(
            available = true,
            result = AccessibilityScreenResult(
                text = "ホーム画面です",
                appName = "Launcher",
                packageName = "com.android.launcher",
                activityName = "LauncherActivity"
            )
        )
        val orchestrator = MascotCommentOrchestrator(
            contextEventRepository = contextRepository,
            llmSettingsRepository = settingsRepository,
            commentGenerator = ConfigurableCommentGenerator(
                eventResponse = "event-comment",
                screenResponder = { input, _ ->
                    if (input.sourceType == ScreenPromptSourceType.ACCESSIBILITY_TEXT) {
                        "accessibility-comment"
                    } else {
                        "unexpected"
                    }
                }
            ),
            screenCaptureSource = FakeScreenCaptureSource(available = false),
            accessibilityScreenSource = accessibilitySource
        )

        val job: Job = launch(Dispatchers.Default) {
            orchestrator.run { text -> generated += text }
        }

        waitForCondition(timeoutMillis = 2_000L) { generated.isNotEmpty() }
        assertEquals("accessibility-comment", generated.first())
        job.cancelAndJoin()
    }

    @Test
    fun `activity change trigger in accessibility mode respects cooldown`() = runBlocking {
        val settingsRepository = FakeLlmSettingsRepository(
            LlmSettings(
                enabled = true,
                screenAnalysisEnabled = true,
                analysisMode = ScreenAnalysisMode.ACCESSIBILITY_ONLY,
                cooldownSec = 1,
                screenCaptureIntervalSec = 300
            )
        )
        val generated = Collections.synchronizedList(mutableListOf<String>())
        val contextRepository = ContextEventRepository()
        val accessibilitySource = FakeAccessibilityScreenSource(
            available = true,
            result = AccessibilityScreenResult(
                text = "初期画面",
                appName = "AppA",
                packageName = "com.test.a",
                activityName = "AActivity"
            )
        )
        val orchestrator = MascotCommentOrchestrator(
            contextEventRepository = contextRepository,
            llmSettingsRepository = settingsRepository,
            commentGenerator = ConfigurableCommentGenerator(
                eventResponse = "event-comment",
                screenResponder = { input, _ ->
                    if (input.sourceType == ScreenPromptSourceType.ACCESSIBILITY_TEXT) {
                        "access-comment"
                    } else {
                        "unexpected"
                    }
                }
            ),
            screenCaptureSource = FakeScreenCaptureSource(available = false),
            accessibilityScreenSource = accessibilitySource
        )

        val job: Job = launch(Dispatchers.Default) {
            orchestrator.run { text -> generated += text }
        }
        waitForCondition(timeoutMillis = 2_000L) { generated.size >= 1 }

        accessibilitySource.updateResult(
            AccessibilityScreenResult(
                text = "次の画面",
                appName = "AppB",
                packageName = "com.test.b",
                activityName = "BActivity"
            )
        )
        accessibilitySource.emitActivityChange("com.test.b", "BActivity")
        waitForCondition(timeoutMillis = 2_000L) { generated.size >= 2 }

        accessibilitySource.emitActivityChange("com.test.c", "CActivity")
        delay(200)
        assertEquals(2, generated.size)

        delay(1_100)
        accessibilitySource.emitActivityChange("com.test.d", "DActivity")
        waitForCondition(timeoutMillis = 2_000L) { generated.size >= 3 }
        assertEquals(3, generated.size)
        job.cancelAndJoin()
    }

    @Test
    fun `falls back to event mode when accessibility capture is unavailable`() = runBlocking {
        val settingsRepository = FakeLlmSettingsRepository(
            LlmSettings(
                enabled = true,
                cooldownSec = 1,
                screenAnalysisEnabled = true,
                analysisMode = ScreenAnalysisMode.ACCESSIBILITY_ONLY
            )
        )
        val contextRepository = ContextEventRepository()
        val generated = Collections.synchronizedList(mutableListOf<String>())
        val orchestrator = MascotCommentOrchestrator(
            contextEventRepository = contextRepository,
            llmSettingsRepository = settingsRepository,
            commentGenerator = ConfigurableCommentGenerator(
                eventResponse = "event-fallback",
                screenResponder = { _, _ -> "screen-comment" }
            ),
            screenCaptureSource = FakeScreenCaptureSource(available = false),
            accessibilityScreenSource = FakeAccessibilityScreenSource(available = false)
        )

        val job: Job = launch(Dispatchers.Default) {
            orchestrator.run { text -> generated += text }
        }
        delay(200)

        contextRepository.onNotificationPosted(
            NotificationEventPayload(
                key = "access-fallback-n1",
                appName = "Sample",
                category = "msg"
            )
        )
        waitForCondition(timeoutMillis = 2_000L) { generated.isNotEmpty() }
        assertEquals("event-fallback", generated.first())
        job.cancelAndJoin()
    }

    private class ConfigurableCommentGenerator(
        private val eventResponse: String,
        private val screenResponder: suspend (ScreenPromptInput, LlmSettings) -> String
    ) : MascotCommentGenerator {
        override suspend fun generate(
            snapshot: MascotContextSnapshot,
            settings: LlmSettings
        ): String = eventResponse

        override suspend fun generateScreenComment(
            input: ScreenPromptInput,
            settings: LlmSettings
        ): String = screenResponder(input, settings)
    }

    private class FakeScreenCaptureSource(
        available: Boolean = false,
        private val result: ScreenCaptureResult? = null
    ) : ScreenCaptureSource {
        override val isCaptureAvailableFlow = MutableStateFlow(available).asStateFlow()

        override suspend fun captureOnce(): ScreenCaptureResult? = result
    }

    private class FakeAccessibilityScreenSource(
        available: Boolean,
        result: AccessibilityScreenResult? = null
    ) : AccessibilityScreenSource {
        private val mutableAvailability = MutableStateFlow(available)
        private val mutableActivityChange = MutableSharedFlow<ActivityChangeEvent>(
            replay = 0,
            extraBufferCapacity = 8
        )

        @Volatile
        private var latestResult: AccessibilityScreenResult? = result

        override val isCaptureAvailableFlow = mutableAvailability.asStateFlow()
        override val activityChangeFlow = mutableActivityChange.asSharedFlow()

        override suspend fun captureOnce(): AccessibilityScreenResult? = latestResult

        fun updateResult(result: AccessibilityScreenResult?) {
            latestResult = result
        }

        fun emitActivityChange(packageName: String?, activityName: String?) {
            mutableActivityChange.tryEmit(
                ActivityChangeEvent(
                    packageName = packageName,
                    activityName = activityName
                )
            )
        }
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
        override suspend fun updateScreenAnalysisEnabled(enabled: Boolean) {}
        override suspend fun updateAnalysisMode(mode: ScreenAnalysisMode) {}
        override suspend fun updateScreenCaptureIntervalSec(intervalSec: Int) {}
        override suspend fun saveApiKey(apiKey: String) {}
        override suspend fun hasApiKey(): Boolean = false
        override suspend fun getApiKeyOrNull(): String? = null
    }

    private suspend fun waitForCondition(timeoutMillis: Long, condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + timeoutMillis
        while (System.currentTimeMillis() < deadline) {
            if (condition()) {
                return
            }
            delay(50)
        }
    }
}
