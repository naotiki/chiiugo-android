package me.naotiki.chiiugo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.serialization.Serializable
import me.naotiki.chiiugo.ui.screen.MainScreen
import me.naotiki.chiiugo.ui.screen.SettingsScreen
import me.naotiki.chiiugo.ui.theme.ChiiugoTheme

@Serializable
sealed interface Screen {
    object MainScreen : Screen
    object SettingScreen : Screen
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ChiiugoTheme(false) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    App()
                }
            }
        }
    }

    override fun finish() {/*　終了しないように　*/
    }
}


@Composable
fun App() {
    val backstack = remember { mutableStateListOf<Any>(Screen.MainScreen) }
    Scaffold(
        bottomBar = {
            NavigationBar(windowInsets = NavigationBarDefaults.windowInsets) {
                val last = backstack.lastOrNull()
                NavigationBarItem(
                    selected = last == Screen.MainScreen,
                    onClick = {
                        backstack.clear()
                        backstack.add(Screen.MainScreen)
                    },
                    icon = {
                        Icon(
                            painterResource(if (last == Screen.MainScreen) R.drawable.baseline_home_24 else R.drawable.outline_home_24),
                            contentDescription = null
                        )
                    },
                    label = {
                        Text("ホーム")
                    }
                )
                NavigationBarItem(
                    selected = backstack.lastOrNull() == Screen.SettingScreen,
                    onClick = {
                        backstack.clear()
                        backstack.add(Screen.SettingScreen)
                    },
                    icon = {
                        Icon(
                            painterResource(if (last == Screen.SettingScreen) R.drawable.baseline_settings_24 else R.drawable.outline_settings_24),
                            contentDescription = null,
                        )
                    },
                    label = {
                        Text("設定")
                    }
                )

            }
        }
    ) { padding ->
        NavDisplay(
            backStack = backstack,
            onBack = { backstack.removeLastOrNull() },
            modifier = Modifier.padding(padding),
            entryProvider = entryProvider {
                entry<Screen.MainScreen> {

                    MainScreen()
                }
                entry<Screen.SettingScreen> {
                    SettingsScreen()
                }

            },
        )
    }
}

@Composable
@Preview
fun AppPreview() {
    App()
}