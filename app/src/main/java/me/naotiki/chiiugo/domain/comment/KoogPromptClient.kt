package me.naotiki.chiiugo.domain.comment

import ai.koog.agents.core.agent.AIAgent
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.clients.openai.OpenAIClientSettings
import ai.koog.prompt.executor.clients.openai.OpenAILLMClient
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.executor.llms.SingleLLMPromptExecutor
import ai.koog.prompt.executor.llms.all.simpleOpenAIExecutor
import ai.koog.prompt.executor.ollama.client.OllamaClient
import ai.koog.prompt.llm.LLMProvider
import ai.koog.prompt.llm.LLModel
import ai.koog.prompt.llm.OllamaModels
import ai.koog.prompt.params.LLMParams
import android.util.Log
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
                maxTokens = settings.maxTokens,

                )
        ) {
            system("/no_think")
            /* system(
                 """
                 # 制約条件
                 - 名詞の前には必ず「ビブリオ」を出力
                 - 形容詞の前には必ず「ブリブリ」を出力
                 - 語尾には必ず「ビブリブオ」を出力
                 - 動詞の前には必ず「ボブリビア」を出力
                 - 助詞の前には「モリモリ」を出力
                 - 助動詞の前には「ブリキ」を出力
                 - 助詞は英語に変換
                 - アルファベットは筆記体を使用
                   - MATHEMATICAL SCRIPT 𝒜など
                 - 全ての単語の前に2~3個の文脈とは未関係な絵文字を出力
             """.trimIndent()
             )*/


            system(
                """
                あなたはスマホ上で動くマスコットです。
                - 返答は日本語で1文のみ
                - 攻撃/差別/脅し/個人情報推定は禁止
                - カギ括弧などは用いない
                - フレンドリー
                """.trimIndent()
            )
            user(
                """
                以下は現在のコンテキストJSONです。
                ${snapshot.toPromptJson()}
                今の状況に対する文を返してください。
                """.trimIndent()
            )
            Log.d("agent_context", snapshot.toPromptJson())
        }
        val model =
            OpenAIModels.Chat.GPT4o.copy(id = settings.model.ifBlank { OpenAIModels.Chat.GPT4o.id })
        val responseMessages = withTimeout(15_000L) {
            OpenAILLMClient(
                apiKey = apiKey?.trim().takeUnless { it.isNullOrBlank() } ?: "lm-studio",
                settings = OpenAIClientSettings(baseUrl = normalizeBaseUrl(settings.baseUrl))
            ).execute(prompt, model, emptyList())
        }

        /*   AIAgent(
               SingleLLMPromptExecutor(
                   OpenAILLMClient(
                       apiKey = apiKey?.trim().takeUnless { it.isNullOrBlank() } ?: "lm-studio",
                       settings = OpenAIClientSettings(baseUrl = normalizeBaseUrl(settings.baseUrl))),

                   ),
               llmModel = model
           )
   */
        return responseMessages.joinToString(" ") { response ->
            response.content
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
