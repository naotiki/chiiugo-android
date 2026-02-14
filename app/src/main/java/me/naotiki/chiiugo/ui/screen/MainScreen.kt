package me.naotiki.chiiugo.ui.screen

import android.app.Activity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.*
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.naotiki.chiiugo.R
import me.naotiki.chiiugo.data.llm.ScreenAnalysisMode
import me.naotiki.chiiugo.service.MascotOverlayService
import me.naotiki.chiiugo.ui.component.GifImage
import me.naotiki.chiiugo.ui.viewmodel.MainScreenViewModel
import android.content.Context
import android.content.ContextWrapper
import android.media.projection.MediaProjectionManager

@Composable
fun MainScreen(viewModel: MainScreenViewModel = hiltViewModel()) {
    var isMascotEnabled by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val llmSettings by viewModel.llmSettingsState.collectAsStateWithLifecycle()
    MascotOverlayService.enable.observe(LocalLifecycleOwner.current) { value ->
        isMascotEnabled = value
    }

    val projectionManager = remember(context) {
        context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
    }
    val capturePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val startIntent = if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            MascotOverlayService.createStartOverlayIntent(
                context.applicationContext,
                captureResultCode = result.resultCode,
                captureData = result.data
            )
        } else {
            MascotOverlayService.createStartOverlayIntent(context.applicationContext)
        }
        context.startService(startIntent)
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Switch(
            checked = isMascotEnabled,
            onCheckedChange = {
                isMascotEnabled = it
                if (it) {
                    val shouldRequestScreenCapture = llmSettings.enabled &&
                        llmSettings.screenAnalysisEnabled &&
                        (
                            llmSettings.analysisMode == ScreenAnalysisMode.MULTIMODAL_ONLY ||
                                llmSettings.analysisMode == ScreenAnalysisMode.OCR_ONLY
                            )

                    if (shouldRequestScreenCapture && context.findActivity() != null) {
                        capturePermissionLauncher.launch(projectionManager.createScreenCaptureIntent())
                    } else {
                        context.startService(
                            MascotOverlayService.createStartOverlayIntent(context.applicationContext)
                        )
                    }
                } else {
                    context.startService(
                        MascotOverlayService.createStopOverlayIntent(context.applicationContext)
                    )
                }
            },
            thumbContent = if (isMascotEnabled) {
                {
                    GifImage(
                        R.drawable.boom, contentDescription = null,
                        modifier = Modifier.size(40.dp)
                    )
                }
            } else null,
            modifier = Modifier.scale(2f)
        )
    }
}

private fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
