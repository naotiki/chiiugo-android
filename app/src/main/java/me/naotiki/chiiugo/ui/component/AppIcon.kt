package me.naotiki.chiiugo.ui.component

import android.content.res.Resources
import android.view.View
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.BitmapCompat
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toBitmap
import me.naotiki.chiiugo.R

@Composable
fun AppIcon(name: String, icon: ImageBitmap, onClick: (Offset?) -> Unit = {}) {
    var offset by remember { mutableStateOf<Offset?>(null) }
    Column(Modifier.width(60.dp).clickable { onClick(offset) }.onGloballyPositioned {
        offset=it.positionInRoot()
    }) {
        Image(icon, contentDescription = name, Modifier.padding(10.dp,5.dp).fillMaxWidth())
        Text(name, Modifier.fillMaxWidth(), textAlign = TextAlign.Center, fontSize = 12.sp, maxLines = 2, lineHeight =20.sp )
    }
}

@Preview
@Composable
private fun AppIcon_Preview() {
    AppIcon(
        "App",
        Resources.getSystem().getDrawable(R.drawable.ic_launcher_foreground).toBitmap().asImageBitmap()
    )
}
