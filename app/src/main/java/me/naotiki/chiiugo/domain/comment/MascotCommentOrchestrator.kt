package me.naotiki.chiiugo.domain.comment

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.isActive
import me.naotiki.chiiugo.data.llm.LlmSettings
import me.naotiki.chiiugo.data.llm.LlmSettingsRepository
import me.naotiki.chiiugo.data.llm.ScreenAnalysisMode
import me.naotiki.chiiugo.domain.context.ContextEvent
import me.naotiki.chiiugo.domain.context.ContextEventRepository
import me.naotiki.chiiugo.domain.context.MascotContextSnapshot
import me.naotiki.chiiugo.domain.context.NotificationContextEvent
import me.naotiki.chiiugo.domain.screen.AccessibilityScreenResult
import me.naotiki.chiiugo.domain.screen.AccessibilityScreenSource
import me.naotiki.chiiugo.domain.screen.ScreenCaptureResult
import me.naotiki.chiiugo.domain.screen.ScreenCaptureSource
import me.naotiki.chiiugo.ui.component.MascotState

class MascotCommentOrchestrator(
    private val contextEventRepository: ContextEventRepository,
    private val llmSettingsRepository: LlmSettingsRepository,
    private val commentGenerator: MascotCommentGenerator,
    private val screenCaptureSource: ScreenCaptureSource,
    private val accessibilityScreenSource: AccessibilityScreenSource
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun run(mascotState: MascotState) {
        run { text ->
            mascotState.say(text, delayMillis = 5000, important = true)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun run(speak: suspend (String) -> Unit) {
        var lastEventGeneratedAt = 0L
        var lastAccessibilityImmediateGeneratedAt = 0L

        suspend fun FlowCollector<String>.collectEventMode(settings: LlmSettings) {
            contextEventRepository.events.collect { event ->
                val now = System.currentTimeMillis()
                val cooldownMillis = settings.cooldownSec * 1000L
                if (now - lastEventGeneratedAt < cooldownMillis) {
                    return@collect
                }

                val snapshot = contextEventRepository.snapshotFlow.value.withEventFocus(event)
                val comment = commentGenerator.generate(snapshot, settings).trim()
                if (comment.isNotBlank()) {
                    lastEventGeneratedAt = now
                    emit(comment)
                }
            }
        }

        suspend fun FlowCollector<String>.collectScreenCaptureMode(settings: LlmSettings) {
            while (currentCoroutineContext().isActive) {
                val comment = generateScreenCaptureBasedComment(settings).trim()
                if (comment.isNotBlank()) {
                    emit(comment)
                }
                delay(settings.screenCaptureIntervalSec * 1000L)
            }
        }

        suspend fun FlowCollector<String>.collectAccessibilityMode(settings: LlmSettings) {
            merge(
                periodicTriggerFlow(settings.screenCaptureIntervalSec),
                accessibilityScreenSource.activityChangeFlow.map { AccessibilityTrigger.ACTIVITY_CHANGE }
            ).collect { trigger ->
                val now = System.currentTimeMillis()
                if (trigger == AccessibilityTrigger.ACTIVITY_CHANGE) {
                    val cooldownMillis = settings.cooldownSec * 1000L
                    if (now - lastAccessibilityImmediateGeneratedAt < cooldownMillis) {
                        return@collect
                    }
                }

                val comment = generateAccessibilityBasedComment(settings).trim()
                if (comment.isNotBlank()) {
                    if (trigger == AccessibilityTrigger.ACTIVITY_CHANGE) {
                        lastAccessibilityImmediateGeneratedAt = now
                    }
                    emit(comment)
                }
            }
        }

        combine(
            llmSettingsRepository.settingsFlow,
            screenCaptureSource.isCaptureAvailableFlow,
            accessibilityScreenSource.isCaptureAvailableFlow
        ) { settings, screenCaptureAvailable, accessibilityAvailable ->
            RuntimeModeState(
                settings = settings,
                screenCaptureAvailable = screenCaptureAvailable,
                accessibilityAvailable = accessibilityAvailable
            )
        }.transformLatest { state ->
            val settings = state.settings
            if (!settings.enabled) {
                return@transformLatest
            }
            if (!settings.screenAnalysisEnabled) {
                collectEventMode(settings)
                return@transformLatest
            }

            when (settings.analysisMode) {
                ScreenAnalysisMode.MULTIMODAL_ONLY,
                ScreenAnalysisMode.OCR_ONLY -> {
                    if (!state.screenCaptureAvailable) {
                        collectEventMode(settings)
                    } else {
                        collectScreenCaptureMode(settings)
                    }
                }

                ScreenAnalysisMode.ACCESSIBILITY_ONLY -> {
                    if (!state.accessibilityAvailable) {
                        collectEventMode(settings)
                    } else {
                        collectAccessibilityMode(settings)
                    }
                }
            }
        }.map { it.trim() }
            .filter { it.isNotBlank() }
            .collect { text ->
                speak(text)
            }
    }

    private suspend fun generateScreenCaptureBasedComment(settings: LlmSettings): String {
        val capture = screenCaptureSource.captureOnce() ?: return ""
        return when (settings.analysisMode) {
            ScreenAnalysisMode.MULTIMODAL_ONLY -> generateMultimodalComment(settings, capture)
            ScreenAnalysisMode.OCR_ONLY -> generateOcrComment(settings, capture)
            ScreenAnalysisMode.ACCESSIBILITY_ONLY -> ""
        }
    }

    private suspend fun generateMultimodalComment(
        settings: LlmSettings,
        capture: ScreenCaptureResult
    ): String {
        val multimodalResult = runCatching {
            commentGenerator.generateScreenComment(
                input = ScreenPromptInput(
                    sourceType = ScreenPromptSourceType.IMAGE,
                    imageJpegBytes = capture.imageJpegBytes
                ),
                settings = settings
            )
        }.getOrDefault("")
            .trim()
        if (multimodalResult.isNotBlank()) {
            return multimodalResult
        }

        val ocr = capture.ocrText.trim()
        if (ocr.isBlank()) {
            return OCR_EMPTY_FALLBACK_TEXT
        }
        return runCatching {
            commentGenerator.generateScreenComment(
                input = ScreenPromptInput(
                    sourceType = ScreenPromptSourceType.OCR_TEXT,
                    ocrText = ocr
                ),
                settings = settings
            ).trim()
        }.getOrDefault("")
            .ifBlank { OCR_EMPTY_FALLBACK_TEXT }
    }

    private suspend fun generateOcrComment(
        settings: LlmSettings,
        capture: ScreenCaptureResult
    ): String {
        val ocr = capture.ocrText.trim()
        if (ocr.isBlank()) {
            return OCR_EMPTY_FALLBACK_TEXT
        }
        return runCatching {
            commentGenerator.generateScreenComment(
                input = ScreenPromptInput(
                    sourceType = ScreenPromptSourceType.OCR_TEXT,
                    ocrText = ocr
                ),
                settings = settings
            ).trim()
        }.getOrDefault("")
            .ifBlank { OCR_EMPTY_FALLBACK_TEXT }
    }

    private suspend fun generateAccessibilityBasedComment(settings: LlmSettings): String {
        val capture = accessibilityScreenSource.captureOnce() ?: return ""
        val text = capture.text.trim()
        if (text.isBlank()) {
            return OCR_EMPTY_FALLBACK_TEXT
        }

        return runCatching {
            commentGenerator.generateScreenComment(
                input = capture.toPromptInput(),
                settings = settings
            ).trim()
        }.getOrDefault("")
            .ifBlank { OCR_EMPTY_FALLBACK_TEXT }
    }

    private fun periodicTriggerFlow(intervalSec: Int): Flow<AccessibilityTrigger> = flow {
        emit(AccessibilityTrigger.PERIODIC)
        while (currentCoroutineContext().isActive) {
            delay(intervalSec * 1000L)
            emit(AccessibilityTrigger.PERIODIC)
        }
    }

    private fun AccessibilityScreenResult.toPromptInput(): ScreenPromptInput {
        return ScreenPromptInput(
            sourceType = ScreenPromptSourceType.ACCESSIBILITY_TEXT,
            ocrText = text,
            appName = appName,
            packageName = packageName,
            activityName = activityName
        )
    }

    private companion object {
        const val OCR_EMPTY_FALLBACK_TEXT = "画面の文字が読み取れなかったから、次でもう一回見るね。"
    }
}

private data class RuntimeModeState(
    val settings: LlmSettings,
    val screenCaptureAvailable: Boolean,
    val accessibilityAvailable: Boolean
)

private enum class AccessibilityTrigger {
    PERIODIC,
    ACTIVITY_CHANGE
}

private fun MascotContextSnapshot.withEventFocus(event: ContextEvent): MascotContextSnapshot {
    val latestNotification = (event as? NotificationContextEvent)?.latestNotification ?: return this
    return copy(recentNotifications = listOf(latestNotification))
}
