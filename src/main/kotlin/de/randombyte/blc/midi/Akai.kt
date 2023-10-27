package de.randombyte.blc.midi

import javax.sound.midi.*

@OptIn(ExperimentalUnsignedTypes::class)
/**
 * The mapping that is sent to the Akai is trying to be as close as possible to the default "Stylus" mapping (number: 12) to keep compatibility with QLC+
 * projects. If this program doesn't work at some point, the QLC+ project will still mostly work with the Stylus mapping.
 */
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
        enableSpecialMode()
        sendMapping("Baustella")
        setupAsyncListener(onSignal, onClose)
        return true
    }

    @OptIn(ExperimentalStdlibApi::class)
    private fun setupAsyncListener(onSignal: (Signal) -> Unit, onClose: () -> Unit) {
        inDevice.transmitter.receiver = object : Receiver {
            override fun send(message: MidiMessage, timestamp: Long) {
                val data = message.message.map { it.toInt() }
                println("Input: ${data.joinToString { it.toByte().toHexString(HexFormat.UpperCase) }}")

                val signal = when (data.size) {
                    // normal MIDI message
                    3 -> Signal(
                        type = data[0],
                        control = data[1],
                        value = data[2]
                    )

                    // parse Sysex messages and forward some as if they were normal MIDI messages
                    10 -> {
                        // These buttons where chosen because they don't change any important behavior of the Akai controller itself.
                        // Mapping the "16 levels" button or other directly affect what the other buttons do or show confusing things in the display
                        val supportedButtons = listOf(
                            0x00, // full level
                            0x02, // preset
                            0x05, // cancel
                            0x0D, // preview
                            0x15, // note repeat (led toggles internally)
                            0x16, // tap tempo
                            0x17, // time division
                        )
                        val button = data[7]
                        val value = data[8]
                        if (button in supportedButtons) {
                                Signal(type = 0x91, control = button, value = value)
                        } else null
                    }
                    else -> null
                }

                if (signal == null) {
                    println("Ignoring input")
                    return
                }

                println("Parsed as input ${signal}")

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
        sendSysEx(SYSEX_SPECIAL_MODE)
    }

    fun sendMapping(name: String) {
        println("Sending mapping to Akai")
        outDevice.sendSysex(generateMapping(name))
    }

    private val SYSEX_START = ubyteArrayOf(0xF0u, 0x47u, 0x0u, 0x78u)
    private val SYSEX_END = 0xF7.toByte()

    private val SIGNAL_VALID_STATUS = ubyteArrayOf(0x40u, 0x41u, 0x43u)

    val SYSEX_SPECIAL_MODE = ubyteArrayOf(0xF0u, 0x47u, 0x00u, 0x78u, 0x30u, 0x00u, 0x04u, 0x01u, 0x00u, 0x00u, 0x38u, 0xF7u)

    private val MAPPING_START = listOf(0xF0, 0x47, 0x00, 0x78, 0x10, 0x04, 0x59, 0x00)

    // here's probably the 5 button configuration to MIDI control message types, it should be one setting for the whole group of 5 buttons
    // currently it is set correctly
    private val MAPPING_AFTER_NAME = listOf(0x20, 0x78, 0x01, 0x07, 0x01, 0x32, 0x3A, 0x03)

    data class Pad(val value: Int)

    private fun generatePadsBank(pads: List<Pad>): List<Int> {
        assert(pads.size == 16)
        return pads.flatMap { listOf(0x03, 0x00, it.value, 0x00, 0x00, 0x00, 0x00, 0x00) }
    }

    data class Fader(val value: Int)

    private fun generateFaders(faders: List<Fader>): List<Int> {
        assert(faders.size == 6)
        return faders.flatMap { listOf(0x00, 0x01, it.value, 0x00, 0x7F) }
    }

    data class Poti(val value: Int)

    private fun generatePotis(potis: List<Poti>): List<Int> {
        assert(potis.size == 6)
        return potis.flatMap { listOf(0x00, 0x01, it.value, 0x00, 0x7F, 0x7F, 0x7F) }
    }

    private val BANK_A = generatePadsBank((0x24..0x33).map { Pad(value = it) })
    private val BANK_B = generatePadsBank((0x34..0x43).map { Pad(value = it) })
    private val BANK_C = generatePadsBank((0x44..0x53).map { Pad(value = it) })
    private val BANK_D = generatePadsBank((0x54..0x63).map { Pad(value = it) })

    private val FADERS = generateFaders(listOf(0x03, 0x04, 0x05, 0x06, 0x08, 0x09).map { Fader(value = it) })

    // order of potis: bottom left, bottom right, middle left, middle right, top left, top right
    private val POTIS = generatePotis(listOf(0x0D, 0x0E, 0x0F, 0x10, 0x11, 0x12).map { Poti(value = it) })

    private val MAPPING_NAME_LENGTH = 8

    private fun generateMapping(name: String) = listOf(
        MAPPING_START,
        name.forceLength(length = MAPPING_NAME_LENGTH, filler = " ").toByteArray().map { it.toInt() },
        MAPPING_AFTER_NAME,
        BANK_A,
        BANK_B,
        BANK_C,
        BANK_D,
        POTIS,
        FADERS,
        listOf(0x07)
    ).flatten()

    fun MidiDevice.sendSysex(data: List<Int>) {
        val byteArray = data.map { it.toByte() }.toByteArray()
        this.receiver.send(SysexMessage(byteArray, byteArray.size), -1)
    }

    /*fun parseSysExIfNotPad(rawData: ByteArray): Signal? {
        if (rawData.size != 10) return null
        val uData = rawData.toUByteArray()
        if (!uData.containsAtFront(SYSEX_START) || rawData.last() != SYSEX_END) return null

        if (uData[4] !in SIGNAL_VALID_STATUS) {
            println("Invalid type: ${uData[4]}")
            return null
        }

        val type = uData[4].toInt()
        val control = uData[7].toInt()
        val value = uData[8].toInt()

        if (type == TouchButton.SYSEX_TYPE && control in Akai.SYSEX_PAD_NUMBERS) return null

        return Signal(type, control, value)
    }*/

    private fun UByteArray.containsAtFront(data: UByteArray): Boolean {
        if (this.size < data.size) return false
        data.forEachIndexed { index, value ->
            if (get(index) != value) return false
        }
        return true
    }

    private fun String.forceLength(length: Int, filler: String) = (this + filler.repeat(length)).substring(0, length)
}
