package me.naotiki.chiiugo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.view.WindowCompat
import dagger.hilt.android.AndroidEntryPoint
import me.naotiki.chiiugo.ui.screen.MainScreen
import me.naotiki.chiiugo.ui.screen.TestScreen
import me.naotiki.chiiugo.ui.theme.ChiiugoTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ChiiugoTheme(false) {
                Surface(modifier = Modifier.fillMaxSize(), color = Color.Transparent) {
                    TestScreen()
                }
            }
        }
    }

    override fun finish() {/*　終了しないように　*/}
}
