package me.naotiki.chiiugo.ui.component

import android.graphics.drawable.Drawable
import android.os.Build.VERSION.SDK_INT
import androidx.annotation.DrawableRes
import androidx.annotation.IdRes
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest

@Composable
fun GifImage(@DrawableRes id: Int,modifier: Modifier=Modifier,contentDescription:String?=null) {
    val imageRequest = ImageRequest.Builder(LocalContext.current)
        .data(id)
        .build()
    AsyncImage(
        model = imageRequest,
        contentDescription = contentDescription,
        contentScale = ContentScale.FillWidth,
        modifier = Modifier.then(modifier),
        imageLoader = ImageLoader.Builder(context = LocalContext.current)
            .components {
                add(ImageDecoderDecoder.Factory())
            }
            .build()
    )
}
