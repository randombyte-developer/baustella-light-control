package de.randombyte.baustellalightcontrol

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.window.*
import com.fazecast.jSerialComm.SerialPort
import com.fazecast.jSerialComm.SerialPortEvent
import com.fazecast.jSerialComm.SerialPortPacketListener
import de.randombyte.baustellalightcontrol.Config.Companion.replaceAt
import de.tobiaserichsen.tevm.TeVirtualMIDI
import de.tobiaserichsen.tevm.TeVirtualMIDI.TE_VM_FLAGS_INSTANTIATE_TX_ONLY
import de.tobiaserichsen.tevm.TeVirtualMIDI.TE_VM_FLAGS_PARSE_TX
import java.io.File
import javax.swing.ImageIcon
import kotlin.system.exitProcess

@ExperimentalComposeUiApi
fun main() = application {
    // styling for the menu bar, may be removed in future compose desktop versions
    System.setProperty("skiko.rendering.laf.global", "true")

    val midiPort = TeVirtualMIDI("BaustellaLightControl", 65535, TE_VM_FLAGS_PARSE_TX or TE_VM_FLAGS_INSTANTIATE_TX_ONLY)

    val configHolder = ConfigHolder.init(
        file = File("settings.conf"),
        default = Config(
            bindings = emptyList()
        )
    )
    configHolder.load()

    Window(
        title = "BaustellaLightControl",
        onCloseRequest = { exitProcess(0) },
        state = WindowState(size = WindowSize(width = 800.dp, height = 400.dp)),
        initialAlignment = Alignment.Center,
        resizable = false,
        icon = ImageIcon(MidiNotes.javaClass.getResource("/new-moon.png").readBytes()).image
    ) {
        var settingsOpened by remember { mutableStateOf(false) }
        var learningOpened by remember { mutableStateOf(false) }

        var ports by remember { mutableStateOf(listOf<SerialPort>()) }
        var selectedPort by remember { mutableStateOf<SerialPort?>(null) }

        var lastSerialData by remember { mutableStateOf("") }
        var lastSentMidiData by remember { mutableStateOf(listOf<Byte>()) }

        MaterialTheme {
            MenuBar {
                Menu("Edit") {
                    Item("Settings", onClick = { settingsOpened = true })
                }
            }

            if (settingsOpened) {
                var bindings by remember { mutableStateOf(configHolder.config.bindings) }
                var bindingOpenForEditingIndex by remember { mutableStateOf<Int?>(null) }

                Dialog(
                    onCloseRequest = {
                        configHolder.config = configHolder.config.copy(bindings = bindings)
                        configHolder.save()
                        settingsOpened = false
                    },
                    title = "Settings",
                    state = DialogState(size = WindowSize(width = 400.dp, height = 800.dp)),
                    resizable = true,
                    initialAlignment = Alignment.Center
                ) {

                    if (learningOpened) {
                        Dialog(
                            onCloseRequest = {
                                learningOpened = false
                            },
                            title = "Learning...",
                            state = DialogState(size = WindowSize(width = 200.dp, height = 100.dp)),
                            resizable = false,
                            initialAlignment = Alignment.Center
                        ) {
                            // check this for safety, sometimes learningOpened is false and this logic is executed, even if after the first call the dialog should be closed
                            if (learningOpened && bindingOpenForEditingIndex != null && lastSerialData != "") {
                                val (_, midiValue) = bindings[bindingOpenForEditingIndex!!]
                                bindings = bindings.replaceAt(bindingOpenForEditingIndex!!, lastSerialData to midiValue)
                                learningOpened = false
                                bindingOpenForEditingIndex = null
                            }

                            Row(
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Text("Press a button")
                            }
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            Button(
                                onClick = {
                                    bindings += "" to MidiNotes.C.byte
                                },
                                modifier = Modifier.align(Alignment.End).padding(vertical = 8.dp)
                            ) {
                                Icon(Icons.Filled.Add, "Add")
                            }

                            Column(
                                modifier = Modifier.border(2.dp, Color.Black, MaterialTheme.shapes.large)
                            ) {
                                bindings.mapIndexed { index, entry -> index to entry }.sortedBy { (index, entry) -> entry.first }.forEach { (trueIndex, entry) ->
                                    val (serialData, midiValue) = entry
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.width(300.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(serialData, modifier = Modifier.width(128.dp).padding(start = 16.dp))
                                        IconButton(
                                            onClick = {
                                                learningOpened = true
                                                lastSerialData = ""
                                                bindingOpenForEditingIndex = trueIndex
                                            }
                                        ) {
                                            Icon(Icons.Filled.Edit, "Learn")
                                        }
                                        Dropdown(
                                            modifier = Modifier.width(96.dp),
                                            items = MidiNotes.mapping.values.toList(),
                                            selectedIndex = MidiNotes.mapping.keys.indexOf(midiValue),
                                            onSelect = { noteName, _ ->
                                                bindings = bindings.replaceAt(trueIndex, serialData to MidiNotes.reversedMapping.getValue(noteName))
                                            }
                                        )
                                        IconButton(
                                            onClick = {
                                                bindings = bindings.toMutableList().apply { removeAt(trueIndex) }
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

            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Dropdown(
                        modifier = Modifier.weight(1f),
                        items = ports.map { it.descriptivePortName },
                        selectedIndex = selectedPort?.let { currentPort -> ports.indexOfFirst { port -> port.systemPortName == currentPort.systemPortName }} ?: 0,
                        onSelect = { name, index ->
                            if (ports[index].descriptivePortName != name) throw RuntimeException("Port-Name is not in sync!")

                            selectedPort?.closePort()
                            selectedPort = ports[index]

                            if (selectedPort!!.openPort()) {
                                selectedPort!!.addDataListener(object : SerialPortPacketListener {
                                    private val receivedData = StringBuilder()

                                    override fun getListeningEvents() = SerialPort.LISTENING_EVENT_DATA_RECEIVED
                                    override fun getPacketSize() = 1

                                    override fun serialEvent(event: SerialPortEvent) {
                                        event.receivedData.map { String(byteArrayOf(it)) }.forEach { newChar ->
                                            if (newChar in (0..9).map(Int::toString)) {
                                                receivedData.append(newChar)
                                                return@forEach
                                            }

                                            if (newChar == ";") {
                                                val newSerialData = receivedData.toString()
                                                receivedData.clear()

                                                if (newSerialData.length == 7) {
                                                    if (newSerialData == lastSerialData) return@forEach

                                                    val dataList = configHolder.config.bindings.filter { (data, _) -> data == newSerialData }
                                                    dataList
                                                        // don't send if it would have been sent the last time (don't deactive a button that was activated last time)
                                                        .filter { (_, midi) -> midi !in lastSentMidiData }
                                                        .forEach { (_, midiValue) ->
                                                            midiPort.sendCommand(byteArrayOf(0x90.toByte(), midiValue, 0x7F.toByte()))
                                                        }
                                                    lastSentMidiData = dataList.map { (_, midi) -> midi }.toList()
                                                    lastSerialData = newSerialData
                                                }
                                            }
                                        }
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

                Text(lastSerialData)
            }
        }
    }
}
