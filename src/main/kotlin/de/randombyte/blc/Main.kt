package de.randombyte.blc

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
import de.randombyte.blc.midi.Signal
import de.randombyte.blc.midi.VirtualMidiPort
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.deepsymmetry.libcarabiner.Message
import org.deepsymmetry.libcarabiner.Runner
import java.net.InetAddress
import java.net.Socket
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

fun main() = application {
    MainWindow(
        onCloseRequest = ::exitApplication
    )
}

private enum class Status(val text: String) {
    Ready("Ready"),
    Started("Started"),
    StartedWithRtl433("Started"),
    Error("Error"),
    ErrorHard("Hard error")
}

private class AppState(status: Status, midiOut: VirtualMidiPort?) {
    var status by mutableStateOf(status)

    var midiOut by mutableStateOf(midiOut)
    var akai by mutableStateOf<Akai?>(null)
    var rtl433 by mutableStateOf<Rtl433?>(null)

    val remoteMapping = mutableStateMapOf<String, Signal>()
    var lastRemoteData by mutableStateOf<String?>(null)
}

// init the MIDI out port
private fun initAppState(): AppState {
    // first open the virtual port
    val midiOut = VirtualMidiPort.openPort()

    // then start QLC+, which should be able to see the port instantly
    if (!QlcPlus.isRunning()) {
        val project = QlcPlus.findTheOnlyProjectFile()
        if (project != null) {
            println("Found unique QLC+ project file")
            QlcPlus.start(project)
        } else {
            println("Didn't find unique QLC+ project file")
        }
    }

    val carabiner = Runner.getInstance()
    if (carabiner.canRunCarabiner()) {
        println("can run carabiner")

        Runtime.getRuntime().addShutdownHook(Thread {
            carabiner.stop()
        })

        val port = 17_000
        carabiner.setPort(port)
        carabiner.start()

        Thread.sleep(1000)

        val socket = Socket(InetAddress.getLoopbackAddress(), port)
        GlobalScope.launch {
            socket.getInputStream().bufferedReader().use { reader ->
                while (true) {
                    println(reader.readLine())
                }
            }
        }

        GlobalScope.launch {
            socket.getOutputStream().writer().use { writer ->
                while (true) {
                    writer.write("status\n")
                    writer.flush()
                    delay(50.milliseconds)
                }
            }
        }
    }

    return AppState(
        status = if (midiOut != null) Status.Ready else Status.ErrorHard,
        midiOut = midiOut
    )
}

private fun startAkai(state: AppState): Boolean {
    state.akai = Akai.findBestMatch()
    val openSuccess = state.akai?.open(
        onSignal = { signal ->
            state.midiOut?.send(signal)

            // learn mapping
            val lastRemoteData = state.lastRemoteData
            if (lastRemoteData != null && !state.remoteMapping.containsKey(lastRemoteData)) {
                println("Learned new mapping: $lastRemoteData -> $signal")
                state.remoteMapping[lastRemoteData] = signal
            }
        },
        onClose = {
            println("Closing because input devices closed")
            state.akai?.close()
            state.akai = null
            state.status = Status.Error
        }
    )
    val success = state.akai != null && openSuccess == true
    state.status = if (success) Status.Started else Status.Error
    return success
}

@OptIn(DelicateCoroutinesApi::class)
private fun tryStartRtl433(state: AppState) {
    val rtl433 = Rtl433(onSignal = { data ->
        state.lastRemoteData = data
        // simulate MIDI message from learned mapping
        val signal = state.remoteMapping[data] ?: return@Rtl433
        println("Found mapping")
        state.midiOut?.send(signal)
    })
    if (rtl433.init()) {
        val startSuccess = rtl433.start(onClose = {
            closeRtl433(state)
            // "downgrade" status if RTL433 was closed
            if (state.status == Status.StartedWithRtl433) {
                state.status = Status.Started
            }
        })

        if (startSuccess) {
            // Delay changing icon to RTL433 because RTL433 takes half a second to realize that there is no RTLSDR hardware connected to the PC.
            // By delaying the icon, it doesn't flicker.
            GlobalScope.launch {
                delay(1.seconds)
                if (state.rtl433 != null && state.status == Status.Started) state.status = Status.StartedWithRtl433
            }
            state.rtl433 = rtl433
            println("RTL_433 initialised")
        }
    } else {
        println("RTL_433 not initialised")
    }
}

private fun closeAkai(state: AppState) {
    state.akai?.close()
    state.akai = null
}

private fun closeRtl433(state: AppState) {
    state.rtl433?.close()
    state.rtl433 = null
}

@Composable
@Preview
fun MainWindow(
    onCloseRequest: () -> Unit
) {
    val state by remember { mutableStateOf(initAppState()) }

    Window(
        onCloseRequest = {
            if (state.status !in listOf(Status.Started, Status.StartedWithRtl433)) onCloseRequest()
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
                            Status.Ready, Status.Started, Status.StartedWithRtl433 -> painterResource("circle.svg")
                            Status.Error, Status.ErrorHard -> painterResource("error.svg")
                        },
                        tint = when (state.status) {
                            Status.Ready -> Color(0xFFFF8800) // orange
                            Status.Started -> Color.Green
                            Status.StartedWithRtl433 -> Color.Blue
                            Status.Error, Status.ErrorHard -> Color.Red
                        },
                        modifier = Modifier.padding(end = 16.dp),
                        contentDescription = "Status"
                    )
                    Text("Status: ${state.status.text}")
                }

                Row {
                    IconButton(
                        enabled = state.status == Status.Ready,
                        onClick = {
                            if (startAkai(state)) {
                                tryStartRtl433(state)
                            }
                        }
                    ) {
                        Icon(painterResource("play-arrow.svg"), contentDescription = "Start")
                    }
                    IconButton(
                        enabled = state.status in listOf(Status.Started, Status.StartedWithRtl433, Status.Error),
                        onClick = {
                            closeAkai(state)
                            closeRtl433(state)
                            state.remoteMapping.clear()
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
