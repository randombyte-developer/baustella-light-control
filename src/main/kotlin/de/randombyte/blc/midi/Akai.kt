package de.randombyte.blc.midi

import javax.sound.midi.*

@OptIn(ExperimentalUnsignedTypes::class)
class Akai(inDevice: MidiDevice, outDevice: MidiDevice) : MidiHandler(inDevice, outDevice) {
    companion object {
        private const val AKAI_NAME = "MPD26"

        fun findBestMatch(): Akai? = try {
            val devices = MidiSystem.getMidiDeviceInfo().map { MidiSystem.getMidiDevice(it) }
            fun findDevice(deviceClassName: String, direction: String): MidiDevice? {
                val filteredDevices = devices.filter { deviceClassName in it.javaClass.simpleName }
                println("$direction MIDI devices: ${filteredDevices.joinToString { it.deviceInfo.name }}")
                val device = filteredDevices.firstOrNull { AKAI_NAME in it.deviceInfo.name }
                println("Akai $direction: ${device?.deviceInfo?.name ?: "not found"}")
                return device
            }

            val akaiIn = findDevice("MidiInDevice", "input")
            val akaiOut = findDevice("MidiOutDevice", "output")
            if (akaiIn != null && akaiOut != null) Akai(akaiIn, akaiOut) else null
        } catch (ex: MidiUnavailableException) {
            null
        }
    }

    fun open(onSignal: (Signal) -> Unit, onClose: () -> Unit): Boolean {
        if (!open()) return false
        //enableSpecialMode()
        sendMapping("Baustella")
        setupAsyncListener(onSignal, onClose)
        return true
    }

    @OptIn(ExperimentalStdlibApi::class)
    private fun setupAsyncListener(onSignal: (Signal) -> Unit, onClose: () -> Unit) {
        inDevice.transmitter.receiver = object : Receiver {
            override fun send(message: MidiMessage, timestamp: Long) {
                val data = message.message
                println("Input: ${data.joinToString { it.toHexString(HexFormat.UpperCase) }}")

                val signal = when (data.size) {
                    3 -> Signal(
                        type = data[0].toUByte(),
                        control = data[1].toUByte(),
                        value = data[2].toUByte()
                    )

                    10 -> null // TODO: handle special buttons by parsing SysEx messages
                    else -> null
                }
                if (signal == null) {
                    println("Ignoring input")
                    return
                }

                onSignal(signal)
            }

            override fun close() {
                onClose()
            }
        }
    }

    /**
     * Sends a special SysEx message to the device to make it send every button press, even those that aren't mapped to
     * any midi signal. This is the mode the official editor operates in.
     */
    private fun enableSpecialMode() {
        println("Enabling special Akai MIDI mode")
        sendSysEx(SysEx.SYSEX_SPECIAL_MODE)
    }

    fun sendMapping(name: String) {
        println("Sending mapping to Akai")
        val mapping = SysEx.generate(name)
        outDevice.receiver.send(SysexMessage(mapping, mapping.size), -1)
    }
}
