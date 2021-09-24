package de.randombyte.baustellalightcontrol

enum class MidiNotes(val byte: Byte, val noteName: String) {
    C(0, "129"),
    C_SHARP(1, "130"),
    D(2, "131"),
    D_SHARP(3, "132"),
    E(4, "133"),
    F(5, "134"),
    F_SHARP(6, "135"),
    G(7, "136"),
    G_SHARP(8, "137"),
    A(9, "138"),
    A_SHARP(10, "139"),
    B(11, "140");

    companion object {
        val mapping = values().associate { it.byte to it.noteName }
        val reversedMapping = mapping.map { it.value to it.key }.toMap()
    }
}
