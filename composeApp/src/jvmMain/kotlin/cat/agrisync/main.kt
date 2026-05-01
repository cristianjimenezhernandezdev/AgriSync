package cat.agrisync

import agrisync.composeapp.generated.resources.Res
import agrisync.composeapp.generated.resources.app_icon
import org.jetbrains.compose.resources.painterResource
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import cat.agrisync.data.JvmEnvConfig

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "AgriSync",
        icon = painterResource(Res.drawable.app_icon),
    ) {
        App(JvmEnvConfig())
    }
}
