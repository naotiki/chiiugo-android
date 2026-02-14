package me.naotiki.chiiugo.data.llm

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

data class LmStudioConnectionResult(
    val success: Boolean,
    val message: String
)

@Singleton
class LmStudioApiClient @Inject constructor() {
    suspend fun testConnection(baseUrl: String, apiKey: String?): LmStudioConnectionResult {
        return withContext(Dispatchers.IO) {
            runCatching {
                val models = fetchModelsInternal(baseUrl, apiKey)
                if (models.isEmpty()) {
                    LmStudioConnectionResult(
                        success = true,
                        message = "接続成功（モデル0件）"
                    )
                } else {
                    LmStudioConnectionResult(
                        success = true,
                        message = "接続成功（モデル${models.size}件）"
                    )
                }
            }.getOrElse { error ->
                LmStudioConnectionResult(
                    success = false,
                    message = "接続失敗: ${error.message ?: "unknown error"}"
                )
            }
        }
    }

    suspend fun fetchModels(baseUrl: String, apiKey: String?): List<String> {
        return withContext(Dispatchers.IO) {
            fetchModelsInternal(baseUrl, apiKey)
        }
    }

    private fun fetchModelsInternal(baseUrl: String, apiKey: String?): List<String> {
        val normalized = normalizeBaseUrl(baseUrl)

        val openAiModelsUrl = "$normalized/models"
        val nativeModelsUrl = "${normalized.removeSuffix("/v1")}/api/v1/models"

        val openAiModels = runCatching {
            val payload = requestJson(openAiModelsUrl, apiKey)
            parseOpenAiModels(payload)
        }.getOrDefault(emptyList())

        val nativeModels = runCatching {
            val payload = requestJson(nativeModelsUrl, apiKey)
            parseNativeModels(payload)
        }.getOrDefault(emptyList())

        val merged = (openAiModels + nativeModels)
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .sorted()

        if (merged.isEmpty()) {
            throw IllegalStateException("モデル一覧を取得できませんでした")
        }
        return merged
    }

    private fun normalizeBaseUrl(baseUrl: String): String {
        val trimmed = baseUrl.trim().ifBlank { "http://127.0.0.1:1234/v1" }.trimEnd('/')
        return if (trimmed.endsWith("/v1")) trimmed else "$trimmed/v1"
    }

    private fun requestJson(url: String, apiKey: String?): String {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 5000
            readTimeout = 7000
            doInput = true
            setRequestProperty("Accept", "application/json")
            val token = apiKey?.trim()
            if (!token.isNullOrEmpty()) {
                setRequestProperty("Authorization", "Bearer $token")
            }
        }

        return try {
            val code = connection.responseCode
            val stream = if (code in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream
            }
            val body = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (code !in 200..299) {
                throw IllegalStateException("HTTP $code $body")
            }
            body
        } finally {
            connection.disconnect()
        }
    }

    private fun parseOpenAiModels(rawJson: String): List<String> {
        val root = JSONObject(rawJson)
        val data = root.optJSONArray("data") ?: JSONArray()
        return buildList {
            for (i in 0 until data.length()) {
                val item = data.optJSONObject(i) ?: continue
                val id = item.optString("id")
                if (id.isNotBlank()) {
                    add(id)
                }
            }
        }
    }

    private fun parseNativeModels(rawJson: String): List<String> {
        val root = JSONObject(rawJson)
        val models = root.optJSONArray("models") ?: JSONArray()
        return buildList {
            for (i in 0 until models.length()) {
                val item = models.optJSONObject(i) ?: continue
                val key = item.optString("key")
                if (key.isNotBlank()) {
                    add(key)
                    continue
                }
                val modelKey = item.optString("modelKey")
                if (modelKey.isNotBlank()) {
                    add(modelKey)
                }
            }
        }
    }
}
