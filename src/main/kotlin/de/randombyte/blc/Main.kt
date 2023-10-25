package de.randombyte.baustellalightcontrol.de.randombyte.blc

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import de.randombyte.blc.midi.Akai

fun main() = application {
    MainWindow(
        onCloseRequest = ::exitApplication
    )
}

private enum class Status(val text: String) {
    Ready("Ready"),
    Started("Started"),
    Error("Error")
}

private class AppState {
    var status by mutableStateOf(Status.Ready)
    val rtl433 = Rtl433(onSignal = { data ->
        oscPort?.send("/QlcPlus/$data", 1)
    })
    var akai by mutableStateOf<Akai?>(null)

    init {
        rtl433.init()
        rtl433.start()
    }
}

@Composable
@Preview
fun MainWindow(
    onCloseRequest: () -> Unit
) {
    val state by remember { mutableStateOf(AppState()) }

    Window(
        onCloseRequest = {
            if (state.status != Status.Started) onCloseRequest()
        },
        state = WindowState(
            width = 350.dp,
            height = 100.dp,
            position = WindowPosition(Alignment.Center)
        ),
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
                Row(
                    horizontalArrangement = Arrangement.Start
                ) {
                    Icon(
                        when (state.status) {
                            Status.Ready, Status.Started -> painterResource("circle.svg")
                            Status.Error -> painterResource("error.svg")
                        },
                        tint = when (state.status) {
                            Status.Ready -> Color(0xFFFF8800) // orange
                            Status.Started -> Color.Green
                            Status.Error -> Color.Red
                        },
                        modifier = Modifier.padding(end = 16.dp),
                        contentDescription = "Status"
                    )
                    Text("Status: ${state.status.text}")
                }

                Row {
                    IconButton(
                        enabled = state.akai == null,
                        onClick = {
                            state.akai = Akai.findBestMatch()
                            val openSuccess = state.akai?.open(
                                onSignal = { signal ->
                                    // TODO: forward normal signals to out Midi
                                },
                                onClose = {
                                    println("Closing because input devices closed")
                                    state.akai?.close()
                                    state.akai = null
                                    state.status = Status.Error
                                }
                            )
                            state.status = if (state.akai == null || openSuccess != true) Status.Error else Status.Started
                        }
                    ) {
                        Icon(painterResource("play-arrow.svg"), contentDescription = "Start")
                    }
                    IconButton(
                        enabled = state.akai != null,
                        onClick = {
                            state.akai?.close()
                            state.akai = null
                            state.status = Status.Ready
                        }
                    ) {
                        Icon(painterResource("stop.svg"), contentDescription = "Stop")
                    }
                }
            }
        }
    }
}
