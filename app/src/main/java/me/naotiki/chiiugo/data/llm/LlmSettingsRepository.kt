package me.naotiki.chiiugo.data.llm

import kotlinx.coroutines.flow.Flow

interface LlmSettingsRepository {
    val settingsFlow: Flow<LlmSettings>

    suspend fun updateEnabled(enabled: Boolean)
    suspend fun updateBaseUrl(baseUrl: String)
    suspend fun updateModel(model: String)
    suspend fun updateCooldownSec(cooldownSec: Int)
    suspend fun updateMaxTokens(maxTokens: Int)
    suspend fun updateTemperature(temperature: Float)
    suspend fun updatePersonaStyle(personaStyle: String)
    suspend fun updateScreenAnalysisEnabled(enabled: Boolean)
    suspend fun updateAnalysisMode(mode: ScreenAnalysisMode)
    suspend fun updateScreenCaptureIntervalSec(intervalSec: Int)

    suspend fun saveApiKey(apiKey: String)
    suspend fun hasApiKey(): Boolean
    suspend fun getApiKeyOrNull(): String?
}
