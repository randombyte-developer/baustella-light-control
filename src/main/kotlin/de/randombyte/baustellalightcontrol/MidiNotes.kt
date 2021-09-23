package de.randombyte.baustellalightcontrol

enum class MidiNotes(val byte: Byte, val noteName: String) {
    C(0, "C"),
    C_SHARP(1, "C#"),
    D(2, "D"),
    D_SHARP(3, "D#"),
    E(4, "E"),
    F(5, "F"),
    F_SHARP(6, "F#"),
    G(7, "G"),
    G_SHARP(8, "G#"),
    A(9, "A"),
    A_SHARP(10, "A#"),
    B(11, "B");

    companion object {
        val mapping = values().associate { it.byte to it.noteName }
        val reversedMapping = mapping.map { it.value to it.key }.toMap()
    }
}
