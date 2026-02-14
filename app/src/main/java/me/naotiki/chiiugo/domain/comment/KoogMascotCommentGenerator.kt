package me.naotiki.chiiugo.domain.comment

import me.naotiki.chiiugo.data.llm.LlmSettings
import me.naotiki.chiiugo.data.llm.LlmSettingsRepository
import me.naotiki.chiiugo.domain.context.MascotContextSnapshot
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KoogMascotCommentGenerator @Inject constructor(
    private val llmSettingsRepository: LlmSettingsRepository,
    private val koogPromptClient: KoogPromptClient
) : MascotCommentGenerator {
    override suspend fun generate(snapshot: MascotContextSnapshot, settings: LlmSettings): String {
        return runCatching {
            koogPromptClient.generate(
                settings = settings,
                snapshot = snapshot,
                apiKey = llmSettingsRepository.getApiKeyOrNull()
            )
                .toSingleSentence()
        }.getOrDefault("")
    }

    override suspend fun generateScreenComment(input: ScreenPromptInput, settings: LlmSettings): String {
        return runCatching {
            koogPromptClient.generateScreenComment(
                settings = settings,
                input = input,
                apiKey = llmSettingsRepository.getApiKeyOrNull()
            ).toSingleSentence()
        }.getOrDefault("")
    }
}

internal fun String.toSingleSentence(): String {
    val normalized = replace("\n", " ").trim()
    if (normalized.isBlank()) return ""

    val sentenceBreak = normalized.indexOfFirst { it == '。' || it == '！' || it == '!' || it == '？' || it == '?' }
    return if (sentenceBreak >= 0) {
        normalized.substring(0, sentenceBreak + 1).trim()
    } else {
        normalized
    }
}
