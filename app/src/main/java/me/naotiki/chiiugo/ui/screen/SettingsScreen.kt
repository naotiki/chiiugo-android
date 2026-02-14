package me.naotiki.chiiugo.ui.screen

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.naotiki.chiiugo.data.llm.LlmSettings
import me.naotiki.chiiugo.data.llm.LlmSettingsRepositoryImpl.Companion.MAX_CONFIGURABLE_TOKENS
import me.naotiki.chiiugo.data.llm.LlmSettingsRepositoryImpl.Companion.MIN_CONFIGURABLE_TOKENS
import me.naotiki.chiiugo.data.llm.LlmSettingsRepositoryImpl.Companion.MAX_SCREEN_CAPTURE_INTERVAL_SEC
import me.naotiki.chiiugo.data.llm.LlmSettingsRepositoryImpl.Companion.MIN_SCREEN_CAPTURE_INTERVAL_SEC
import me.naotiki.chiiugo.data.llm.ScreenAnalysisMode
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val config by viewModel.configState.collectAsStateWithLifecycle()
    val llmSettings by viewModel.llmSettingsState.collectAsStateWithLifecycle()
    val hasApiKey by viewModel.hasApiKey.collectAsStateWithLifecycle()
    val connectionTestResult by viewModel.connectionTestResult.collectAsStateWithLifecycle()
    val isTestingConnection by viewModel.isTestingConnection.collectAsStateWithLifecycle()
    val availableModels by viewModel.availableModels.collectAsStateWithLifecycle()
    val isLoadingModels by viewModel.isLoadingModels.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var apiKeyInput by rememberSaveable { mutableStateOf("") }
    var notificationPermissionGranted by remember {
        mutableStateOf(isNotificationListenerEnabled(context))
    }

    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                notificationPermissionGranted = isNotificationListenerEnabled(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "マスコット設定",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        SettingsCard(title = "画像サイズ") {
            Text(
                text = "${config.imageSize.roundToInt()} dp",
                style = MaterialTheme.typography.bodyMedium
            )
            Slider(
                value = config.imageSize,
                onValueChange = { viewModel.updateImageSize(it) },
                valueRange = 50f..300f,
                steps = 24
            )
        }

        SettingsCard(title = "移動速度") {
            Text(
                text = "${config.moveSpeedMs} ms",
                style = MaterialTheme.typography.bodyMedium
            )
            Slider(
                value = config.moveSpeedMs.toFloat(),
                onValueChange = { viewModel.updateMoveSpeed(it.roundToInt()) },
                valueRange = 500f..5000f,
                steps = 8
            )
        }

        SettingsCard(title = "透明度") {
            Text(
                text = "${(config.transparency * 100).roundToInt()}%",
                style = MaterialTheme.typography.bodyMedium
            )
            Slider(
                value = config.transparency,
                onValueChange = { viewModel.updateTransparency(it) },
                valueRange = 0.1f..1f
            )
        }

        SettingsCard(title = "タッチブロック") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (config.blockingTouch) "有効" else "無効",
                    style = MaterialTheme.typography.bodyMedium
                )
                Switch(
                    checked = config.blockingTouch,
                    onCheckedChange = { viewModel.updateBlockingTouch(it) }
                )
            }
        }

        SettingsCard(title = "LLM発話設定") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "LLM生成",
                    style = MaterialTheme.typography.titleSmall
                )
                Switch(
                    checked = llmSettings.enabled,
                    onCheckedChange = { viewModel.updateLlmEnabled(it) }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "画面解析",
                    style = MaterialTheme.typography.titleSmall
                )
                Switch(
                    checked = llmSettings.screenAnalysisEnabled,
                    onCheckedChange = { viewModel.updateScreenAnalysisEnabled(it) },
                    enabled = llmSettings.enabled
                )
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "画面収録許可はマスコットON時に毎回表示されます",
                style = MaterialTheme.typography.bodySmall
            )

            if (llmSettings.screenAnalysisEnabled) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "解析モード",
                    style = MaterialTheme.typography.titleSmall
                )
                ScreenAnalysisModeItem(
                    title = "Multimodal only",
                    selected = llmSettings.analysisMode == ScreenAnalysisMode.MULTIMODAL_ONLY,
                    enabled = llmSettings.enabled,
                    onClick = { viewModel.updateAnalysisMode(ScreenAnalysisMode.MULTIMODAL_ONLY) }
                )
                ScreenAnalysisModeItem(
                    title = "OCR only",
                    selected = llmSettings.analysisMode == ScreenAnalysisMode.OCR_ONLY,
                    enabled = llmSettings.enabled,
                    onClick = { viewModel.updateAnalysisMode(ScreenAnalysisMode.OCR_ONLY) }
                )
                ScreenAnalysisModeItem(
                    title = "OFF",
                    selected = llmSettings.analysisMode == ScreenAnalysisMode.OFF,
                    enabled = llmSettings.enabled,
                    onClick = { viewModel.updateAnalysisMode(ScreenAnalysisMode.OFF) }
                )

                if (llmSettings.analysisMode != ScreenAnalysisMode.OFF) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("画面送信間隔: ${llmSettings.screenCaptureIntervalSec} 秒")
                    Slider(
                        value = llmSettings.screenCaptureIntervalSec.toFloat(),
                        onValueChange = {
                            viewModel.updateScreenCaptureIntervalSec(it.roundToInt())
                        },
                        valueRange = MIN_SCREEN_CAPTURE_INTERVAL_SEC.toFloat()..MAX_SCREEN_CAPTURE_INTERVAL_SEC.toFloat(),
                        steps = (MAX_SCREEN_CAPTURE_INTERVAL_SEC - MIN_SCREEN_CAPTURE_INTERVAL_SEC) / 10 - 1,
                        enabled = llmSettings.enabled
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = llmSettings.baseUrl,
                onValueChange = { viewModel.updateLlmBaseUrl(it) },
                label = { Text("LMStudio Base URL") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = llmSettings.model,
                onValueChange = { viewModel.updateLlmModel(it) },
                label = { Text("Model ID") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { viewModel.testLmStudioConnection() },
                    enabled = !isTestingConnection && !isLoadingModels,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (isTestingConnection) "接続中..." else "接続テスト")
                }
                Button(
                    onClick = { viewModel.loadAvailableModels() },
                    enabled = !isTestingConnection && !isLoadingModels,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (isLoadingModels) "取得中..." else "モデル一覧取得")
                }
            }

            if (!connectionTestResult.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = connectionTestResult ?: "",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            if (availableModels.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "利用可能モデル (${availableModels.size})",
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(modifier = Modifier.height(6.dp))
                availableModels.forEach { modelId ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = modelId,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodySmall
                        )
                        Button(
                            onClick = { viewModel.updateLlmModel(modelId) },
                            modifier = Modifier.padding(start = 8.dp)
                        ) {
                            Text("使う")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text("クールダウン: ${llmSettings.cooldownSec} 秒")
            Slider(
                value = llmSettings.cooldownSec.toFloat(),
                onValueChange = { viewModel.updateLlmCooldownSec(it.roundToInt()) },
                valueRange = 5f..120f,
                steps = 22
            )

            Text("Max Tokens: ${llmSettings.maxTokens}")
            Slider(
                value = llmSettings.maxTokens.toFloat(),
                onValueChange = { viewModel.updateLlmMaxTokens(it.roundToInt()) },
                valueRange = MIN_CONFIGURABLE_TOKENS.toFloat()..MAX_CONFIGURABLE_TOKENS.toFloat(),
                steps = 28
            )

            Text("Temperature: ${"%.2f".format(llmSettings.temperature)}")
            Slider(
                value = llmSettings.temperature,
                onValueChange = { viewModel.updateLlmTemperature(it) },
                valueRange = 0f..2f
            )

            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = llmSettings.personaStyle,
                onValueChange = { viewModel.updatePersonaStyle(it) },
                label = { Text("口調メモ") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (notificationPermissionGranted) {
                        "通知アクセス: 許可済み"
                    } else {
                        "通知アクセス: 未許可"
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
                Button(onClick = {
                    context.startActivity(
                        Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                    )
                }) {
                    Text("許可画面を開く")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = apiKeyInput,
                onValueChange = { apiKeyInput = it },
                label = { Text("API Key (任意)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                visualTransformation = PasswordVisualTransformation()
            )

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (hasApiKey) "APIキー: 保存済み" else "APIキー: 未保存",
                    style = MaterialTheme.typography.bodyMedium
                )
                Button(onClick = {
                    viewModel.saveApiKey(apiKeyInput)
                    apiKeyInput = ""
                }) {
                    Text("APIキー保存")
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun SettingsCard(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            content()
        }
    }
}

@Composable
private fun ScreenAnalysisModeItem(
    title: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick, enabled = enabled)
        Text(text = title, style = MaterialTheme.typography.bodyMedium)
    }
}

private fun isNotificationListenerEnabled(context: Context): Boolean {
    return NotificationManagerCompat.getEnabledListenerPackages(context)
        .contains(context.packageName)
}
