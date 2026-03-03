package cat.agrisync

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import cat.agrisync.data.JvmEnvConfig

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "AgriSync",
    ) {
        App(JvmEnvConfig())
    }
}