package me.naotiki.chiiugo.ui.viewmodel

import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.view.View
import androidx.compose.ui.unit.IntOffset
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import me.naotiki.chiiugo.data.AppInfo
import me.naotiki.chiiugo.data.llm.LlmSettings
import me.naotiki.chiiugo.data.llm.LlmSettingsRepository
import me.naotiki.chiiugo.data.repository.InstalledAppRepository
import javax.inject.Inject

private const val ROW_COUNT = 6

@HiltViewModel
class MainScreenViewModel @Inject constructor(
    private val repository: InstalledAppRepository,
    llmSettingsRepository: LlmSettingsRepository
) : ViewModel() {
    val llmSettingsState = llmSettingsRepository.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = LlmSettings()
    )

    fun appPickup(app:AppInfo){

    }
    fun getAppInfoList(): List<List<AppInfo>> {
        return repository.getInstalledAppInfoList().chunked(ROW_COUNT)
    }

    fun launchApp(context: Context, appInfo: AppInfo, offset: IntOffset? = null, view: View? = null) {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
            addCategory(Intent.CATEGORY_LAUNCHER)
            component = appInfo.componentName
        }
        val options = if (view != null && offset != null)
            ActivityOptions.makeScaleUpAnimation(view, offset.x, offset.y, 0, 0).toBundle()
        else null
        context.startActivity(intent, options)
    }
}
