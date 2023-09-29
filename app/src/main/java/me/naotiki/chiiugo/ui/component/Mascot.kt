package me.naotiki.chiiugo.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember

@Stable
class MascotState{

}

@Composable
fun rememberMascotState():MascotState{
    return remember { MascotState() }
}

@Composable
fun Mascot(){

}
