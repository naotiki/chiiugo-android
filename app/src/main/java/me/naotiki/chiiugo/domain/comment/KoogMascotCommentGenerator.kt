package me.naotiki.chiiugo.domain.comment

import android.util.Log
import me.naotiki.chiiugo.data.llm.LlmSettings
import me.naotiki.chiiugo.data.llm.LlmSettingsRepository
import me.naotiki.chiiugo.domain.context.MascotContextSnapshot
import me.naotiki.chiiugo.ui.component.texts
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
                //.toSingleSentence()
                //.take(40)
                .ifBlank { "debug" }
                //.ifBlank { texts.random() }
        }.getOrThrow()/*.getOrElse {

            ((it.message?:it.cause) as String).apply {
                Log.d("agent_context",it.message.toString())
                Log.d("agent_context",it.cause.toString())
            }

            //texts.random()
        }*/
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
