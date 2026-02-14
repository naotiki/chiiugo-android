package me.naotiki.chiiugo.domain.comment

import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.clients.openai.OpenAIClientSettings
import ai.koog.prompt.executor.clients.openai.OpenAILLMClient
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.params.LLMParams
import kotlinx.coroutines.withTimeout
import me.naotiki.chiiugo.data.llm.LlmSettings
import me.naotiki.chiiugo.domain.context.MascotContextSnapshot
import javax.inject.Inject
import javax.inject.Singleton

interface KoogPromptClient {
    suspend fun generate(settings: LlmSettings, snapshot: MascotContextSnapshot, apiKey: String?): String
}

@Singleton
class KoogPromptClientImpl @Inject constructor() : KoogPromptClient {
    override suspend fun generate(
        settings: LlmSettings,
        snapshot: MascotContextSnapshot,
        apiKey: String?
    ): String {
        val prompt = prompt(
            id = "mascot-context-comment",
            params = LLMParams(
                temperature = settings.temperature.toDouble(),
                maxTokens = settings.maxTokens
            )
        ) {
            system(
                """
                あなたはスマホ上で動くマスコットです。
                - 返答は日本語で1文のみ
                - 40文字以内
                - 既存キャラ感のある軽口
                - 攻撃/差別/脅し/個人情報推定は禁止
                - 口調メモ: ${settings.personaStyle}
                """.trimIndent()
            )
            user(
                """
                以下は現在のコンテキストJSONです。
                ${snapshot.toPromptJson()}
                今の状況に対する短い一言を返してください。
                """.trimIndent()
            )
        }

        val model = OpenAIModels.Chat.GPT4o.copy(id = settings.model.ifBlank { OpenAIModels.Chat.GPT4o.id })
        val responseMessages = withTimeout(10_000L) {
            OpenAILLMClient(
                apiKey = apiKey?.trim().takeUnless { it.isNullOrBlank() } ?: "lm-studio",
                settings = OpenAIClientSettings(baseUrl = normalizeBaseUrl(settings.baseUrl))
            ).execute(prompt, model, emptyList())
        }
        return responseMessages.joinToString(" ") { response ->
            extractResponseText(response)
        }.trim()
    }

    private fun normalizeBaseUrl(baseUrl: String): String {
        val trimmed = baseUrl.trim().ifBlank { "http://127.0.0.1:1234/v1" }.trimEnd('/')
        return if (trimmed.endsWith("/v1")) trimmed else "$trimmed/v1"
    }

    private fun extractResponseText(message: Any): String {
        val content = runCatching {
            message.javaClass.methods
                .firstOrNull { it.name == "getContent" && it.parameterCount == 0 }
                ?.invoke(message)
        }.getOrNull()
        return content?.toString().orEmpty()
    }
}
