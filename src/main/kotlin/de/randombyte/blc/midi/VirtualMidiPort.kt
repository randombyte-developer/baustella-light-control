package de.randombyte.blc.midi

import de.tobiaserichsen.tevm.TeVirtualMIDI

class VirtualMidiPort {
    private val port = TeVirtualMIDI("Baustella Light Control", 65535, TeVirtualMIDI.TE_VM_FLAGS_PARSE_TX or TeVirtualMIDI.TE_VM_FLAGS_INSTANTIATE_TX_ONLY)

    fun send(oscPath: String, value: Int) {
        midiPort.sendCommand(byteArrayOf())
    }

    fun close() {
        midiPort.shutdown()
    }
}