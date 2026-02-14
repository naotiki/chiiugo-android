package me.naotiki.chiiugo.domain.comment

import ai.koog.prompt.dsl.Prompt
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.clients.openai.OpenAIClientSettings
import ai.koog.prompt.executor.clients.openai.OpenAILLMClient
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.message.AttachmentContent
import ai.koog.prompt.message.ContentPart
import ai.koog.prompt.params.LLMParams
import kotlinx.coroutines.withTimeout
import me.naotiki.chiiugo.data.llm.LlmSettings
import me.naotiki.chiiugo.domain.context.MascotContextSnapshot
import javax.inject.Inject
import javax.inject.Singleton

interface KoogPromptClient {
    suspend fun generate(
        settings: LlmSettings,
        snapshot: MascotContextSnapshot,
        apiKey: String?
    ): String

    suspend fun generateScreenComment(
        settings: LlmSettings,
        input: ScreenPromptInput,
        apiKey: String?
    ): String
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
            params = settings.toLlmParams()
        ) {
            buildCommonSystemPrompts()
            user(
                """
                以下は現在のコンテキストJSONです。
                ${snapshot.toPromptJson()}
                今の状況に対する文を返してください。
                """.trimIndent()
            )
        }
        return executePrompt(prompt, settings, apiKey)
    }

    override suspend fun generateScreenComment(
        settings: LlmSettings,
        input: ScreenPromptInput,
        apiKey: String?
    ): String {
        val prompt = prompt(
            id = "mascot-screen-comment",
            params = settings.toLlmParams()
        ) {
            buildCommonSystemPrompts()
            when {
                input.imageJpegBytes != null -> {
                    user {
                        text("以下は現在の画面キャプチャです。今の表示に対する短い文を返してください。")
                        image(
                            ContentPart.Image(
                                content = AttachmentContent.Binary.Bytes(input.imageJpegBytes),
                                format = "jpg",
                                mimeType = "image/jpeg",
                                fileName = "screen.jpg"
                            )
                        )
                    }
                }

                !input.ocrText.isNullOrBlank() -> {
                    user(
                        """
                        以下は現在画面のOCR結果です。
                        ${input.ocrText}
                        今の表示に対する短い文を返してください。
                        """.trimIndent()
                    )
                }

                else -> user("画面情報が取得できませんでした。")
            }
        }
        return executePrompt(prompt, settings, apiKey)
    }

    private suspend fun executePrompt(
        prompt: Prompt,
        settings: LlmSettings,
        apiKey: String?
    ): String {
        val model = OpenAIModels.Chat.GPT4o.copy(
            id = settings.model.ifBlank { OpenAIModels.Chat.GPT4o.id }
        )
        val responseMessages = withTimeout(15_000L) {
            OpenAILLMClient(
                apiKey = apiKey?.trim().takeUnless { it.isNullOrBlank() } ?: "lm-studio",
                settings = OpenAIClientSettings(baseUrl = normalizeBaseUrl(settings.baseUrl))
            ).execute(prompt, model, emptyList())
        }
        return responseMessages.joinToString(" ") { response -> response.content }.trim()
    }

    private fun normalizeBaseUrl(baseUrl: String): String {
        val trimmed = baseUrl.trim().ifBlank { "http://127.0.0.1:1234/v1" }.trimEnd('/')
        return if (trimmed.endsWith("/v1")) trimmed else "$trimmed/v1"
    }

    private fun LlmSettings.toLlmParams(): LLMParams {
        return LLMParams(
            temperature = temperature.toDouble(),
            maxTokens = maxTokens
        )
    }
}

private fun ai.koog.prompt.dsl.PromptBuilder.buildCommonSystemPrompts() {
    system("/no_think")
    system(
        """
        あなたはスマホ上で動くマスコットです。
        スマホの情報を定期的に取得し，定期的にユーザーに話しかけます．
        - 返答は日本語で1文のみ
        - 攻撃/差別/脅し/個人情報推定は禁止
        - カギ括弧などは用いない
        - 敬語は一切用いず，友達のように話しかけて
        """.trimIndent()
    )
}
