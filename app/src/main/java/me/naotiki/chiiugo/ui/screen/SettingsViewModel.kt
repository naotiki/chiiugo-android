package me.naotiki.chiiugo.ui.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import me.naotiki.chiiugo.data.llm.LmStudioApiClient
import me.naotiki.chiiugo.data.llm.LlmSettingsRepository
import me.naotiki.chiiugo.data.repository.ConfigRepository
import me.naotiki.chiiugo.ui.component.Config
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val configRepository: ConfigRepository,
    private val llmSettingsRepository: LlmSettingsRepository,
    private val lmStudioApiClient: LmStudioApiClient
) : ViewModel() {

    val configState = configRepository.configFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = Config()
    )
    val llmSettingsState = llmSettingsRepository.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = me.naotiki.chiiugo.data.llm.LlmSettings()
    )

    private val _hasApiKey = MutableStateFlow(false)
    val hasApiKey: StateFlow<Boolean> = _hasApiKey.asStateFlow()
    private val _connectionTestResult = MutableStateFlow<String?>(null)
    val connectionTestResult: StateFlow<String?> = _connectionTestResult.asStateFlow()
    private val _isTestingConnection = MutableStateFlow(false)
    val isTestingConnection: StateFlow<Boolean> = _isTestingConnection.asStateFlow()
    private val _availableModels = MutableStateFlow<List<String>>(emptyList())
    val availableModels: StateFlow<List<String>> = _availableModels.asStateFlow()
    private val _isLoadingModels = MutableStateFlow(false)
    val isLoadingModels: StateFlow<Boolean> = _isLoadingModels.asStateFlow()

    init {
        viewModelScope.launch {
            _hasApiKey.value = llmSettingsRepository.hasApiKey()
        }
    }

    fun updateImageSize(size: Float) {
        viewModelScope.launch {
            configRepository.updateImageSize(size)
        }
    }

    fun updateMoveSpeed(speedMs: Int) {
        viewModelScope.launch {
            configRepository.updateMoveSpeed(speedMs)
        }
    }

    fun updateTransparency(transparency: Float) {
        viewModelScope.launch {
            configRepository.updateTransparency(transparency)
        }
    }

    fun updateAreaOffset(offset: Pair<Float, Float>) {
        viewModelScope.launch {
            configRepository.updateAreaOffset(offset)
        }
    }

    fun updateAreaSize(size: Pair<Float, Float>) {
        viewModelScope.launch {
            configRepository.updateAreaSize(size)
        }
    }

    fun updateBlockingTouch(blocking: Boolean) {
        viewModelScope.launch {
            configRepository.updateBlockingTouch(blocking)
        }
    }

    fun updateLlmEnabled(enabled: Boolean) {
        viewModelScope.launch {
            llmSettingsRepository.updateEnabled(enabled)
        }
    }

    fun updateLlmBaseUrl(baseUrl: String) {
        viewModelScope.launch {
            llmSettingsRepository.updateBaseUrl(baseUrl)
        }
    }

    fun updateLlmModel(model: String) {
        viewModelScope.launch {
            llmSettingsRepository.updateModel(model)
        }
    }

    fun updateLlmCooldownSec(cooldownSec: Int) {
        viewModelScope.launch {
            llmSettingsRepository.updateCooldownSec(cooldownSec)
        }
    }

    fun updateLlmMaxTokens(maxTokens: Int) {
        viewModelScope.launch {
            llmSettingsRepository.updateMaxTokens(maxTokens)
        }
    }

    fun updateLlmTemperature(temperature: Float) {
        viewModelScope.launch {
            llmSettingsRepository.updateTemperature(temperature)
        }
    }

    fun updatePersonaStyle(personaStyle: String) {
        viewModelScope.launch {
            llmSettingsRepository.updatePersonaStyle(personaStyle)
        }
    }

    fun saveApiKey(apiKey: String) {
        viewModelScope.launch {
            llmSettingsRepository.saveApiKey(apiKey)
            _hasApiKey.value = llmSettingsRepository.hasApiKey()
        }
    }

    fun testLmStudioConnection() {
        viewModelScope.launch(Dispatchers.IO) {
            _isTestingConnection.value = true
            try {
                val settings = llmSettingsRepository.settingsFlow.first()
                val apiKey = llmSettingsRepository.getApiKeyOrNull()
                val result = lmStudioApiClient.testConnection(settings.baseUrl, apiKey)
                _connectionTestResult.value = result.message
            } finally {
                _isTestingConnection.value = false
            }
        }
    }

    fun loadAvailableModels() {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoadingModels.value = true
            try {
                val settings = llmSettingsRepository.settingsFlow.first()
                val apiKey = llmSettingsRepository.getApiKeyOrNull()
                val models = lmStudioApiClient.fetchModels(settings.baseUrl, apiKey)
                _availableModels.value = models
                _connectionTestResult.value = "モデル一覧を取得しました"
            } catch (e: Exception) {
                _availableModels.value = emptyList()
                _connectionTestResult.value = "モデル取得失敗: ${e.message ?: "unknown error"}"
            } finally {
                _isLoadingModels.value = false
            }
        }
    }
}
