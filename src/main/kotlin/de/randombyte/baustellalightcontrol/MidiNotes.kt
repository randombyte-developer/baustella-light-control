package de.randombyte.baustellalightcontrol

enum class MidiNotes(val byte: Byte, val noteName: String) {
    C(0, "C"),
    C_SHARP(1, "C#"),
    D(2, "D"),
    D_SHARP(3, "D#"),
    E(4, "E"),
    F(5, "F"),
    F_SHARP(6, "F#");

    companion object {
        val mapping = values().associate { it.byte to it.noteName }
        val reversedMapping = mapping.map { it.value to it.key }.toMap()
    }
}
