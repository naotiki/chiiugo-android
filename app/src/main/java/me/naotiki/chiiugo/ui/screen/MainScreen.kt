package me.naotiki.chiiugo.ui.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
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
import androidx.lifecycle.compose.LocalLifecycleOwner
import me.naotiki.chiiugo.R
import me.naotiki.chiiugo.service.MascotOverlayService
import me.naotiki.chiiugo.ui.component.GifImage

@Composable
fun MainScreen() {
    var isMascotEnabled by remember { mutableStateOf(false) }

    val context = LocalContext.current
    MascotOverlayService.enable.observe(LocalLifecycleOwner.current) { value ->
        isMascotEnabled = value
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
                    context.startService(
                        MascotOverlayService.createStartOverlayIntent(context.applicationContext)
                    )
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

