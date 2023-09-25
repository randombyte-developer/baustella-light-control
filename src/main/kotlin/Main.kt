import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Row
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application

private class AppState {
    var settingsOpened by mutableStateOf(false)
    var learningOpened by mutableStateOf(false)

    var oscPort by mutableStateOf(7700)
}

@Composable
@Preview
fun App() {
    val state by remember { mutableStateOf(AppState()) }

    MaterialTheme {
        Row {
            TextField(
                value = state.oscPort.toString(),
                onValueChange = { it.toIntOrNull()?.also { port -> state.oscPort = port } },
                label = { Text("Port") },
            )
        }
    }
}

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        state = WindowState(width = 800.dp, height = 400.dp),
        title = "Baustella Light Control",
        icon = painterResource("new-moon.png"),
        resizable = false,
    ) {
        App()
    }
}
