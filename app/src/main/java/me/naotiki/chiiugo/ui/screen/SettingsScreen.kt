package me.naotiki.chiiugo.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val config by viewModel.configState.collectAsStateWithLifecycle()

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

        // Image Size Setting
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

        // Move Speed Setting
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

        // Transparency Setting
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

        // Area Offset Setting
        SettingsCard(title = "移動エリアオフセット") {
            Text(
                text = "X: ${(config.areaOffset.first * 100).roundToInt()}%",
                style = MaterialTheme.typography.bodySmall
            )
            Slider(
                value = config.areaOffset.first,
                onValueChange = {
                    viewModel.updateAreaOffset(it to config.areaOffset.second)
                },
                valueRange = 0f..0.5f
            )
            Text(
                text = "Y: ${(config.areaOffset.second * 100).roundToInt()}%",
                style = MaterialTheme.typography.bodySmall
            )
            Slider(
                value = config.areaOffset.second,
                onValueChange = {
                    viewModel.updateAreaOffset(config.areaOffset.first to it)
                },
                valueRange = 0f..0.5f
            )
        }

        // Area Size Setting
        SettingsCard(title = "移動エリアサイズ") {
            Text(
                text = "幅: ${(config.areaSize.first * 100).roundToInt()}%",
                style = MaterialTheme.typography.bodySmall
            )
            Slider(
                value = config.areaSize.first,
                onValueChange = {
                    viewModel.updateAreaSize(it to config.areaSize.second)
                },
                valueRange = 0.1f..1f
            )
            Text(
                text = "高さ: ${(config.areaSize.second * 100).roundToInt()}%",
                style = MaterialTheme.typography.bodySmall
            )
            Slider(
                value = config.areaSize.second,
                onValueChange = {
                    viewModel.updateAreaSize(config.areaSize.first to it)
                },
                valueRange = 0.1f..1f
            )
        }

        // Blocking Touch Setting
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