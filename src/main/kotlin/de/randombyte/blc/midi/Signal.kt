package de.randombyte.blc.midi

@OptIn(ExperimentalUnsignedTypes::class)
data class Signal(val type: UByte, val control: UByte, val value: UByte) {
    val uByteArray: UByteArray
        get() = ubyteArrayOf(type, control, value)

    companion object {
        fun fromUByteArray(uByteArray: UByteArray): Signal {
            assert(uByteArray.size == 3)
            return Signal(uByteArray[0], uByteArray[1], uByteArray[2])
        }
    }
}
