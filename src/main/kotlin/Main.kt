package de.randombyte.baustellalightcontrol

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import java.lang.Exception

fun main() = application {
    MainWindow(
        onCloseRequest = ::exitApplication
    )
}

private enum class OscPortStatus(val text: String) {
    Closed("Closed"),
    Opened("Opened"),
    Error("Error")
}

private class AppState {
    var oscPort by mutableStateOf<OscPort?>(null)
    var oscPortStatus by mutableStateOf(OscPortStatus.Closed)
}

@Composable
@Preview
fun MainWindow(
    onCloseRequest: () -> Unit,
) {
    val state by remember { mutableStateOf(AppState()) }

    Window(
        onCloseRequest = onCloseRequest,
        state = WindowState(width = 300.dp, height = 100.dp),
        title = "Baustella Light Control",
        icon = painterResource("new-moon.png"),
        resizable = false
    ) {
        MaterialTheme {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Status: ${state.oscPortStatus.text}"
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        enabled = state.oscPort == null,
                        onClick = {
                            try {
                                state.oscPort = OscPort()
                                state.oscPortStatus = OscPortStatus.Opened
                            } catch (ex: Exception) {
                                state.oscPortStatus = OscPortStatus.Error
                            }
                        }
                    ) {
                        Icon(painterResource("play-arrow.svg"), contentDescription = "Start")
                    }
                    IconButton(
                        enabled = state.oscPort != null,
                        onClick = {
                            try {
                                state.oscPort?.close()
                                state.oscPort = null
                                state.oscPortStatus = OscPortStatus.Closed
                            } catch (ex: Exception) {
                                state.oscPortStatus = OscPortStatus.Error
                            }
                        }
                    ) {
                        Icon(painterResource("stop.svg"), contentDescription = "Stop")
                    }
                }
            }
        }
    }
}