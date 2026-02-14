package me.naotiki.chiiugo.domain.comment

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.isActive
import me.naotiki.chiiugo.data.llm.LlmSettings
import me.naotiki.chiiugo.data.llm.LlmSettingsRepository
import me.naotiki.chiiugo.data.llm.ScreenAnalysisMode
import me.naotiki.chiiugo.domain.context.ContextEvent
import me.naotiki.chiiugo.domain.context.ContextEventRepository
import me.naotiki.chiiugo.domain.context.MascotContextSnapshot
import me.naotiki.chiiugo.domain.context.NotificationContextEvent
import me.naotiki.chiiugo.domain.screen.ScreenCaptureResult
import me.naotiki.chiiugo.domain.screen.ScreenCaptureSource
import me.naotiki.chiiugo.ui.component.MascotState

class MascotCommentOrchestrator(
    private val contextEventRepository: ContextEventRepository,
    private val llmSettingsRepository: LlmSettingsRepository,
    private val commentGenerator: MascotCommentGenerator,
    private val screenCaptureSource: ScreenCaptureSource
) {
    suspend fun run(mascotState: MascotState) {
        run { text ->
            mascotState.say(text, delayMillis = 5000, important = true)
        }
    }

    suspend fun run(speak: suspend (String) -> Unit) {
        var lastGeneratedAt = 0L

        combine(
            llmSettingsRepository.settingsFlow,
            screenCaptureSource.isCaptureAvailableFlow
        ) { settings, captureAvailable ->
            settings to captureAvailable
        }.transformLatest { (settings, captureAvailable) ->
            if (!settings.enabled || settings.analysisMode == ScreenAnalysisMode.OFF) {
                return@transformLatest
            }

            val useScreenAnalysis = settings.screenAnalysisEnabled &&
                settings.analysisMode != ScreenAnalysisMode.OFF &&
                captureAvailable

            if (useScreenAnalysis) {
                while (currentCoroutineContext().isActive) {
                    val comment = generateScreenBasedComment(settings).trim()
                    if (comment.isNotBlank()) {
                        emit(comment)
                    }
                    delay(settings.screenCaptureIntervalSec * 1000L)
                }
            } else {
                contextEventRepository.events.collect { event ->
                    val now = System.currentTimeMillis()
                    val cooldownMillis = settings.cooldownSec * 1000L
                    if (now - lastGeneratedAt < cooldownMillis) {
                        return@collect
                    }

                    val snapshot = contextEventRepository.snapshotFlow.value.withEventFocus(event)
                    val comment = commentGenerator.generate(snapshot, settings).trim()
                    if (comment.isNotBlank()) {
                        lastGeneratedAt = now
                        emit(comment)
                    }
                }
            }
        }.map { it.trim() }
            .filter { it.isNotBlank() }
            .collect { text ->
                speak(text)
            }
    }

    private suspend fun generateScreenBasedComment(settings: LlmSettings): String {
        val capture = screenCaptureSource.captureOnce() ?: return ""
        return when (settings.analysisMode) {
            ScreenAnalysisMode.MULTIMODAL_ONLY -> generateMultimodalComment(settings, capture)
            ScreenAnalysisMode.OCR_ONLY -> generateOcrComment(settings, capture)
            ScreenAnalysisMode.OFF -> ""
        }
    }

    private suspend fun generateMultimodalComment(
        settings: LlmSettings,
        capture: ScreenCaptureResult
    ): String {
        val multimodalResult = runCatching {
            commentGenerator.generateScreenComment(
                input = ScreenPromptInput(imageJpegBytes = capture.imageJpegBytes),
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
                input = ScreenPromptInput(ocrText = ocr),
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
                input = ScreenPromptInput(ocrText = ocr),
                settings = settings
            ).trim()
        }.getOrDefault("")
            .ifBlank { OCR_EMPTY_FALLBACK_TEXT }
    }

    private companion object {
        const val OCR_EMPTY_FALLBACK_TEXT = "画面の文字が読み取れなかったから、次でもう一回見るね。"
    }
}

private fun MascotContextSnapshot.withEventFocus(event: ContextEvent): MascotContextSnapshot {
    val latestNotification = (event as? NotificationContextEvent)?.latestNotification ?: return this
    return copy(recentNotifications = listOf(latestNotification))
}
