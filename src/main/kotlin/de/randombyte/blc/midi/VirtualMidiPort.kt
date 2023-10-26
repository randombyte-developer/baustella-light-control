package de.randombyte.blc.midi

import de.tobiaserichsen.tevm.TeVirtualMIDI
import de.tobiaserichsen.tevm.TeVirtualMIDIException

@OptIn(ExperimentalUnsignedTypes::class)
class VirtualMidiPort(private val port: TeVirtualMIDI) {
    companion object {
        fun openPort() = try {
            val port = TeVirtualMIDI(
                "Baustella Light Control",
                65535,
                TeVirtualMIDI.TE_VM_FLAGS_PARSE_TX or TeVirtualMIDI.TE_VM_FLAGS_INSTANTIATE_TX_ONLY
            )
            VirtualMidiPort(port)
        } catch (ex: UnsatisfiedLinkError) {
            println("Can't find teVirtualMIDI! Is loopMidi installed?")
            null
        } catch (ex: TeVirtualMIDIException) {
            println("Can't open virtual MIDI port: ${ex.reason}")
            null
        }
    }

    fun send(signal: Signal) {
        port.sendCommand(signal.uByteArray.toByteArray())
    }

    fun close() {
        port.shutdown()
    }
}
