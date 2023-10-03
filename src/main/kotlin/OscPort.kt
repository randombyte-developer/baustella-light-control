package de.randombyte.baustellalightcontrol

import com.illposed.osc.OSCMessage
import com.illposed.osc.transport.OSCPortOut
import java.net.InetAddress

class OscPort {
    private val oscPort = OSCPortOut(InetAddress.getLoopbackAddress(), 7700) // 7700 is the default port of QLC+

    fun send(oscPath: String, value: Int) {
        oscPort.send(OSCMessage(oscPath, listOf(value)))
    }

    fun close() {
        oscPort.close()
    }
}
