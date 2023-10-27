package de.randombyte.blc.midi

@OptIn(ExperimentalUnsignedTypes::class)
object SysEx {
    private val SYSEX_START = ubyteArrayOf(0xF0u, 0x47u, 0x0u, 0x78u)
    private const val SYSEX_END = 0xF7.toByte()

    private val SIGNAL_VALID_STATUS = ubyteArrayOf(0x40u, 0x41u, 0x43u)

    val SYSEX_SPECIAL_MODE = ubyteArrayOf(0xF0u, 0x47u, 0x00u, 0x78u, 0x30u, 0x00u, 0x04u, 0x01u, 0x00u, 0x00u, 0x38u, 0xF7u)

    private val MAPPING_START = listOf(0xF0, 0x47, 0x00, 0x78, 0x10, 0x04, 0x59, 0x00)

    private val MAPPING_AFTER_NAME = listOf(0x20, 0x78, 0x01, 0x07, 0x01, 0x32, 0x3A, 0x03)

    private val BANK_A = generateBank((0x24..0x33).map { Pad(value = it) })
    private val BANK_B = generateBank((0x34..0x43).map { Pad(value = it) })
    private val BANK_C = generateBank((0x44..0x53).map { Pad(value = it) })
    private val BANK_D = generateBank((0x54..0x63).map { Pad(value = it) })

    private val MAPPING_AFTER_BANKS = listOf(
        0x00, 0x01, 0x0F, 0x00, 0x7F, 0x7F, 0x7F, 0x00,
        0x01, 0x10, 0x00, 0x7F, 0x7F, 0x7F, 0x00, 0x01,
        0x0D, 0x00, 0x7F, 0x7F, 0x7F, 0x00, 0x01, 0x0E,
        0x00, 0x7F, 0x7F, 0x7F, 0x00, 0x01, 0x0B, 0x00,
        0x7F, 0x7F, 0x7F, 0x00, 0x01, 0x0C, 0x00, 0x7F,
        0x7F, 0x7F, 0x00, 0x01, 0x01, 0x00, 0x7F, 0x00,
        0x01, 0x02, 0x00, 0x7F, 0x00, 0x01, 0x03, 0x00,
        0x7F, 0x00, 0x01, 0x04, 0x00, 0x7F, 0x00, 0x01,
        0x05, 0x00, 0x7F, 0x00, 0x01, 0x06, 0x00, 0x7F, 0xF7
    )

    data class Pad(val value: Int)

    private fun generateBank(pads: List<Pad>): List<Int> {
        assert(pads.size == 16)
        return pads.flatMap { listOf(0x03, 0x00, it.value, 0x00, 0x01, 0x00, 0x00, 0x00) }
    }

    private const val MAPPING_NAME_LENGTH = 8

    fun generate(name: String) = listOf(
        MAPPING_START,
        name.forceLength(length = MAPPING_NAME_LENGTH, filler = " ").toByteArray().map { it.toInt() },
        MAPPING_AFTER_NAME,
        BANK_A,
        BANK_B,
        BANK_C,
        BANK_D,
        MAPPING_AFTER_BANKS
    ).flatten().map { it.toByte() }.toByteArray()

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