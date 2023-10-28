package de.randombyte.blc.midi

data class Signal(val type: Int, val control: Int, val value: Int) {
    val uByteArray: ByteArray
        get() = byteArrayOf(type.toByte(), control.toByte(), value.toByte())

    @OptIn(ExperimentalStdlibApi::class)
    override fun toString(): String {
        return "type=0x${type.toByte().toHexString()}, control=0x${control.toByte().toHexString()}, value=0x${value.toByte().toHexString()}"
    }
}
