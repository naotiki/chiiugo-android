package me.naotiki.chiiugo.ui.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import me.naotiki.chiiugo.R

@Composable
fun AppIcon(name: String, iconPainter: Painter, onClick: (Offset?) -> Unit = {}) {
    var offset by remember { mutableStateOf<Offset?>(null) }
    Column(Modifier.width(60.dp).clickable { onClick(offset) }.onGloballyPositioned {
        offset=it.positionInRoot()
    }) {
        Image(iconPainter, contentDescription = name, Modifier.padding(10.dp,5.dp).fillMaxWidth())
        Text(name, Modifier.fillMaxWidth(), textAlign = TextAlign.Center, fontSize = 12.sp, maxLines = 2, lineHeight =20.sp )
    }
}

@Preview
@Composable
private fun AppIcon_Preview() {
    val drawable = LocalContext.current.getDrawable(R.drawable.ic_launcher_foreground)?:return

    AppIcon(
        "App",
        rememberDrawablePainter(drawable)
    )
}
