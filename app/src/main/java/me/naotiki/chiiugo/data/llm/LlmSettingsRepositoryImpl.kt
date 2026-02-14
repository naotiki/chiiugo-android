package me.naotiki.chiiugo.data.llm

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

private val Context.llmSettingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "llm_settings")

@Singleton
class LlmSettingsRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val secureApiKeyStore: SecureApiKeyStore
) : LlmSettingsRepository {
    companion object {
        val MIN_CONFIGURABLE_TOKENS = 16
        val MAX_CONFIGURABLE_TOKENS = 2048
        val MIN_SCREEN_CAPTURE_INTERVAL_SEC = 30
        val MAX_SCREEN_CAPTURE_INTERVAL_SEC = 300
    }
    private object PreferenceKeys {
        val ENABLED = booleanPreferencesKey("enabled")
        val BASE_URL = stringPreferencesKey("base_url")
        val MODEL = stringPreferencesKey("model")
        val COOLDOWN_SEC = intPreferencesKey("cooldown_sec")
        val MAX_TOKENS = intPreferencesKey("max_tokens")
        val TEMPERATURE = floatPreferencesKey("temperature")
        val PERSONA_STYLE = stringPreferencesKey("persona_style")
        val SCREEN_ANALYSIS_ENABLED = booleanPreferencesKey("screen_analysis_enabled")
        val ANALYSIS_MODE = stringPreferencesKey("analysis_mode")
        val SCREEN_CAPTURE_INTERVAL_SEC = intPreferencesKey("screen_capture_interval_sec")
    }

    private val defaultSettings = LlmSettings()

    override val settingsFlow: Flow<LlmSettings> =
        context.llmSettingsDataStore.data.map { preferences ->
            LlmSettings(
                enabled = preferences[PreferenceKeys.ENABLED] ?: defaultSettings.enabled,
                baseUrl = preferences[PreferenceKeys.BASE_URL] ?: defaultSettings.baseUrl,
                model = preferences[PreferenceKeys.MODEL] ?: defaultSettings.model,
                cooldownSec = preferences[PreferenceKeys.COOLDOWN_SEC]
                    ?: defaultSettings.cooldownSec,
                maxTokens = preferences[PreferenceKeys.MAX_TOKENS] ?: defaultSettings.maxTokens,
                temperature = preferences[PreferenceKeys.TEMPERATURE]
                    ?: defaultSettings.temperature,
                personaStyle = preferences[PreferenceKeys.PERSONA_STYLE]
                    ?: defaultSettings.personaStyle,
                screenAnalysisEnabled = preferences[PreferenceKeys.SCREEN_ANALYSIS_ENABLED]
                    ?: defaultSettings.screenAnalysisEnabled,
                analysisMode = preferences[PreferenceKeys.ANALYSIS_MODE]
                    ?.let { value ->
                        runCatching { ScreenAnalysisMode.valueOf(value) }
                            .getOrDefault(defaultSettings.analysisMode)
                    } ?: defaultSettings.analysisMode,
                screenCaptureIntervalSec = (preferences[PreferenceKeys.SCREEN_CAPTURE_INTERVAL_SEC]
                    ?: defaultSettings.screenCaptureIntervalSec)
                    .coerceIn(MIN_SCREEN_CAPTURE_INTERVAL_SEC, MAX_SCREEN_CAPTURE_INTERVAL_SEC)
            )
        }

    override suspend fun updateEnabled(enabled: Boolean) {
        context.llmSettingsDataStore.edit { preferences ->
            preferences[PreferenceKeys.ENABLED] = enabled
        }
    }

    override suspend fun updateBaseUrl(baseUrl: String) {
        context.llmSettingsDataStore.edit { preferences ->
            preferences[PreferenceKeys.BASE_URL] =
                baseUrl.trim().ifBlank { defaultSettings.baseUrl }
        }
    }

    override suspend fun updateModel(model: String) {
        context.llmSettingsDataStore.edit { preferences ->
            preferences[PreferenceKeys.MODEL] = model.trim().ifBlank { defaultSettings.model }
        }
    }

    override suspend fun updateCooldownSec(cooldownSec: Int) {
        context.llmSettingsDataStore.edit { preferences ->
            preferences[PreferenceKeys.COOLDOWN_SEC] = cooldownSec.coerceAtLeast(1)
        }
    }

    override suspend fun updateMaxTokens(maxTokens: Int) {
        context.llmSettingsDataStore.edit { preferences ->
            preferences[PreferenceKeys.MAX_TOKENS] =
                maxTokens.coerceIn(MIN_CONFIGURABLE_TOKENS, MAX_CONFIGURABLE_TOKENS)
        }
    }

    override suspend fun updateTemperature(temperature: Float) {
        context.llmSettingsDataStore.edit { preferences ->
            preferences[PreferenceKeys.TEMPERATURE] = temperature.coerceIn(0f, 2f)
        }
    }

    override suspend fun updatePersonaStyle(personaStyle: String) {
        context.llmSettingsDataStore.edit { preferences ->
            preferences[PreferenceKeys.PERSONA_STYLE] = personaStyle.trim().ifBlank {
                defaultSettings.personaStyle
            }
        }
    }

    override suspend fun updateScreenAnalysisEnabled(enabled: Boolean) {
        context.llmSettingsDataStore.edit { preferences ->
            preferences[PreferenceKeys.SCREEN_ANALYSIS_ENABLED] = enabled
        }
    }

    override suspend fun updateAnalysisMode(mode: ScreenAnalysisMode) {
        context.llmSettingsDataStore.edit { preferences ->
            preferences[PreferenceKeys.ANALYSIS_MODE] = mode.name
        }
    }

    override suspend fun updateScreenCaptureIntervalSec(intervalSec: Int) {
        context.llmSettingsDataStore.edit { preferences ->
            preferences[PreferenceKeys.SCREEN_CAPTURE_INTERVAL_SEC] =
                intervalSec.coerceIn(MIN_SCREEN_CAPTURE_INTERVAL_SEC, MAX_SCREEN_CAPTURE_INTERVAL_SEC)
        }
    }

    override suspend fun saveApiKey(apiKey: String) {
        withContext(Dispatchers.IO) {
            secureApiKeyStore.saveApiKey(apiKey)
        }
    }

    override suspend fun hasApiKey(): Boolean {
        return withContext(Dispatchers.IO) {
            secureApiKeyStore.hasApiKey()
        }
    }

    override suspend fun getApiKeyOrNull(): String? {
        return withContext(Dispatchers.IO) {
            secureApiKeyStore.readApiKey()
        }
    }
}
