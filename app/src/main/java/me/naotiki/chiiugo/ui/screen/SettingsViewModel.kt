package me.naotiki.chiiugo.ui.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import me.naotiki.chiiugo.data.repository.ConfigRepository
import me.naotiki.chiiugo.ui.component.Config
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val configRepository: ConfigRepository
) : ViewModel() {

    val configState = configRepository.configFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = Config()
    )

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
}

