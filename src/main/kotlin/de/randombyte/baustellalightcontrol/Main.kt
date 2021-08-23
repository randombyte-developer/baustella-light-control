package de.randombyte.baustellalightcontrol

import androidx.compose.desktop.Window
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fazecast.jSerialComm.*

fun main() = Window {
    var ports by remember { mutableStateOf(listOf<SerialPort>()) }
    var selectedPort by remember { mutableStateOf<SerialPort?>(null) }
    var portOpened by remember { mutableStateOf(false) }

    var messages by remember { mutableStateOf(listOf<String>()) }

    MaterialTheme {
        Row(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(onClick = {
                    ports = SerialPort.getCommPorts().toList()
                }) {
                    Text("Refresh COM-Ports")
                }

                Dropdown(ports.map { it.systemPortName }, onSelect = { portName ->
                    selectedPort = try {
                        SerialPort.getCommPort(portName)
                    } catch (e: SerialPortInvalidPortException) {
                        null
                    }

                    if (selectedPort?.openPort() == true) {
                        selectedPort?.addDataListener(object : SerialPortPacketListener {
                            override fun getListeningEvents() = SerialPort.LISTENING_EVENT_DATA_RECEIVED
                            override fun getPacketSize() = 8

                            override fun serialEvent(event: SerialPortEvent) {
                                messages = messages + event.receivedData.decodeToString().dropLast(1)
                            }
                        })
                    }
                })

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
