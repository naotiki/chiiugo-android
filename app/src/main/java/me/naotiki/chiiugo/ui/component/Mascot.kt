package me.naotiki.chiiugo.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch


private val forceClose = arrayOf(Key.R, Key.E, Key.I, Key.S, Key.U, Key.B)


@Composable
fun Mascot(
    mascotState: MascotState,
    modifier: Modifier = Modifier,
    configData: Config = Config()
) {
    LaunchedEffect(Unit) {
        launch {
            mascotState.loop()
        }
    }
    Box(modifier = modifier.width(configData.imageSize.dp+200.dp)) {

        //SUCちゃん
        GifImage(
            mascotState.gifRes,
            modifier = Modifier.size(configData.imageSize.dp),
            null,
            colorFilter = ColorFilter.tint(mascotState.colorState, BlendMode.Modulate),
        )
        val serif by mascotState.serifFlow.collectAsState()

        if (serif != null) {
            Box(
                modifier = Modifier
                    .padding(
                        start = configData.imageSize.dp - (configData.imageSize.dp * 0.05f),
                        top = (configData.imageSize.dp * 0.05f)
                    )
                    .width(150.dp)
            ) {


                Text(
                    serif ?: "",
                    fontSize = 10.sp,
                    modifier = Modifier
                        .background(
                            color = Color(0xff5ff4ac),
                            shape = RoundedCornerShape(30)
                        )
                        .padding(10.dp)
                )
            }
        }

        mascotState.charMap.forEach { (c, a) ->
            val anim by a.second.asState()
            Text(c.toString(), Modifier.offset(x = a.first.dp, y = anim.dp), color = Color.Red)
        }
    }
}