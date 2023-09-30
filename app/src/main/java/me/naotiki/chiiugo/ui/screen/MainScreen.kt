package me.naotiki.chiiugo.ui.screen
import android.content.Intent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import androidx.compose.ui.zIndex
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import me.naotiki.chiiugo.R
import me.naotiki.chiiugo.data.AppInfo
import me.naotiki.chiiugo.ui.component.AppIcon
import me.naotiki.chiiugo.ui.component.GifImage
import me.naotiki.chiiugo.ui.component.SystemBroadcastReceiver
import me.naotiki.chiiugo.ui.viewmodel.MainScreenViewModel
import kotlin.random.Random


@Composable
fun MainScreen(
    viewModel: MainScreenViewModel = viewModel()
) {
    val context = LocalContext.current
    var apps by remember { mutableStateOf(listOf<List<AppInfo>>()) }

    LaunchedEffect(Unit) {
        apps = viewModel.getAppInfoList()
    }
    SystemBroadcastReceiver(
        Intent.ACTION_PACKAGE_ADDED,
        Intent.ACTION_PACKAGE_REMOVED,
        Intent.ACTION_PACKAGE_CHANGED,
        Intent.ACTION_PACKAGE_REPLACED,
        additionalIntentFilter = {
            addDataScheme("package")
        }
    ) {
        //TODO アプリリストの再構成
    }
    val screenWidth = LocalConfiguration.current.screenWidthDp
    val screenHeight = LocalConfiguration.current.screenHeightDp
    val offset = remember { Animatable(Offset(0f, 0f), Offset.VectorConverter) }
    val currentOffset by offset.asState()
    LaunchedEffect(Unit){
        launch {
            while (true){
                offset.animateTo(Offset(Random.nextFloat()*screenWidth,Random.nextFloat()*screenHeight),
                    tween(
                        5000, easing = EaseInOut
                    ))
            }
        }
    }
    Scaffold(containerColor = Color.Transparent) {
        Box(Modifier.padding(it).fillMaxSize()) {
            //GifImage(R.drawable.boom,Modifier.zIndex(1f).absoluteOffset(currentOffset.x.dp,currentOffset.y.dp))
            Column(Modifier.fillMaxSize().padding(it).verticalScroll(rememberScrollState())) {
                apps.forEach {
                    Row {
                        it.forEach { appInfo ->
                            val view = LocalView.current
                            AppIcon(appInfo.label, appInfo.icon.toBitmap().asImageBitmap(), onClick = {
                                viewModel.launchApp(context, appInfo, it?.round(), view)
                            })
                        }
                    }
                }
            }
        }
    }
}
