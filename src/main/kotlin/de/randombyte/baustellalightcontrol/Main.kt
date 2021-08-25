package de.randombyte.baustellalightcontrol

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.MenuBar
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.fazecast.jSerialComm.SerialPort
import com.fazecast.jSerialComm.SerialPortEvent
import com.fazecast.jSerialComm.SerialPortPacketListener
import de.tobiaserichsen.tevm.TeVirtualMIDI
import java.io.File
import kotlin.system.exitProcess

@ExperimentalComposeUiApi
fun main() = application {
    // styling for the menu bar, may be removed in future compose desktop versions
    System.setProperty("skiko.rendering.laf.global", "true")

    val midiPort = TeVirtualMIDI("BaustellaLightControl")

    val configHolder = ConfigHolder(
        file = File("settings.conf"),
        serializer = Config.serializer(),
        default = Config(
            bindings = emptyMap()
        )
    )
    configHolder.load()

    Window(
        onCloseRequest = { exitProcess(0) }
    ) {
        var settingsOpened by remember { mutableStateOf(false) }
        var learningOpened by remember { mutableStateOf(false) }

        var ports by remember { mutableStateOf(listOf<SerialPort>()) }
        var selectedPort by remember { mutableStateOf<SerialPort?>(null) }

        var messages by remember { mutableStateOf(listOf<String>()) }

        MaterialTheme {
            MenuBar {
                Menu("Edit") {
                    Item("Settings", onClick = { settingsOpened = true })
                }
            }

            if (settingsOpened) {
                var bindings by remember { mutableStateOf(configHolder.config.bindings) }

                Dialog(
                    onCloseRequest = {
                        configHolder.save()
                        settingsOpened = false
                    },
                    title = "Settings"
                ) {

                    if (learningOpened) {
                        Dialog(
                            onCloseRequest = {
                                learningOpened = false
                            },
                            title = "Learning..."
                        ) {
                            Text("Press a button")
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            Button(
                                onClick = {
                                    bindings += "" to MidiNotes.C3.byte
                                },
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Icon(Icons.Filled.Add, "Add")
                            }

                            Column(
                                modifier = Modifier.border(2.dp, Color.Black, MaterialTheme.shapes.large)
                            ) {
                                bindings.forEach { (remoteValue, midiValue) ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.width(400.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(remoteValue, modifier = Modifier.width(128.dp))
                                        IconButton(
                                            onClick = {
                                                learningOpened = true
                                            }
                                        ) {
                                            Icon(Icons.Filled.Edit, "Learn")
                                        }
                                        Dropdown(
                                            modifier = Modifier.width(96.dp),
                                            items = MidiNotes.mapping.values.toList(),
                                            defaultSelectedIndex = MidiNotes.mapping.keys.indexOf(midiValue),
                                            onSelect = { noteName, _ ->
                                                bindings += remoteValue to MidiNotes.reversedMapping.getValue(noteName)
                                            }
                                        )
                                        IconButton(
                                            onClick = {
                                                bindings -= remoteValue
                                            }
                                        ) {
                                            Icon(Icons.Filled.Delete, "Delete")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Dropdown(
                        modifier = Modifier.weight(1f),
                        items = ports.map { it.descriptivePortName },
                        defaultSelectedIndex = 0,
                        onSelect = { name, index ->
                            if (ports[index].descriptivePortName != name) throw RuntimeException("Port-Name is not in sync!")

                            selectedPort?.closePort()
                            selectedPort = ports[index]

                            if (selectedPort!!.openPort()) {
                                selectedPort!!.addDataListener(object : SerialPortPacketListener {
                                    override fun getListeningEvents() = SerialPort.LISTENING_EVENT_DATA_RECEIVED
                                    override fun getPacketSize() = 8

                                    override fun serialEvent(event: SerialPortEvent) {
                                        messages = messages + event.receivedData.decodeToString().dropLast(1)
                                    }
                                })
                            }
                        }
                    )

                    Button(
                        onClick = { ports = SerialPort.getCommPorts().toList() },
                        modifier = Modifier.padding(start = 16.dp)
                    ) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
                    }
                }

                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    messages.forEach { message ->
                        Text(message)
                    }
                }
            }
        }
    }
}
