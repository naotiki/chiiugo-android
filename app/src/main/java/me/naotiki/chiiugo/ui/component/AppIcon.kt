package me.naotiki.chiiugo.ui.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.*
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import kotlinx.coroutines.launch
import me.naotiki.chiiugo.R
import kotlin.math.roundToInt
import kotlin.random.Random

private val appIconDpSize = DpSize(60.dp, 80.dp)

@Composable
fun AppIcon(
    name: String,
    iconPainter: Painter,
    modifier: Modifier = Modifier,
    onClick: (Offset?) -> Unit = {},
    onDragStart: () -> Unit = {}
) {
    var iconPosition by remember { mutableStateOf<Offset?>(null) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    //
    val density=LocalDensity.current
    val screenWidth = LocalConfiguration.current.screenWidthDp
    val screenHeight = LocalConfiguration.current.screenHeightDp
    val offset = remember { Animatable(Offset(0f, 0f), Offset.VectorConverter) }
    LaunchedEffect(Unit) {
        launch {
            while (true) {
                val target = Offset(
                    Random.nextFloat() * (screenWidth - appIconDpSize.width.value),
                    Random.nextFloat() * (screenHeight - appIconDpSize.height.value)
                )
                offset.animateTo(
                    target,
                    tween(
                        500, easing = EaseInOut
                    )
                )
            }
        }
    }
    val currentOffset by offset.asState()
    //
    val result=currentOffset.round()-(iconPosition?.round()?:IntOffset.Zero)
    Column(Modifier
        .absoluteOffset(result.x.dp,result.y.dp)
        //.offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
        .size(appIconDpSize)
        .clickable { onClick(currentOffset) }
        .onGloballyPositioned {
            if (iconPosition!=null)return@onGloballyPositioned
            iconPosition = it.positionInRoot()/density.density
        }.pointerInput(Unit) {
            detectDragGesturesAfterLongPress(onDragStart = { onDragStart() }) { change, dragAmount ->
                change.consume()
                offsetX += dragAmount.x
                offsetY += dragAmount.y
            }
        } then modifier) {
        Image(iconPainter, contentDescription = name, Modifier.padding(10.dp, 5.dp).fillMaxWidth())
        Text(
            name,
            Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            fontSize = 12.sp,
            maxLines = 2,
            lineHeight = 12.sp,
            color = Color.Black
        )
    }
}

@Composable
fun DummyAppIcon(modifier: Modifier = Modifier) {
    Box(Modifier.size(appIconDpSize) then modifier)
}

@Preview
@Preview(showBackground = true, name = "Background")
@Composable
private fun AppIcon_Preview() {
    val drawable = LocalContext.current.getDrawable(R.drawable.ic_launcher_foreground) ?: return
    AppIcon(
        "Appfsrffrsfrrfrffrrffrrffrrffr",
        rememberDrawablePainter(drawable)
    )
}
