package de.randombyte.blc.midi

import javax.sound.midi.MidiDevice
import javax.sound.midi.MidiUnavailableException
import javax.sound.midi.SysexMessage

open class MidiHandler(val inDevice: MidiDevice, val outDevice: MidiDevice) {
    fun open(): Boolean {
        try {
            if (!inDevice.isOpen) inDevice.open()
            if (!outDevice.isOpen) outDevice.open()
        } catch (ex: MidiUnavailableException) {
            println("Can't open MIDI device: ${ex.message}")
            return false
        }
        return true
    }

    fun close() {
        println("Closing devices")
        if (inDevice.isOpen) inDevice.close()
        if (outDevice.isOpen) outDevice.close()
    }

    @OptIn(ExperimentalUnsignedTypes::class)
    protected fun sendSysEx(data: UByteArray) {
        outDevice.receiver.send(SysexMessage(data.toByteArray(), data.size), -1)
    }
}
