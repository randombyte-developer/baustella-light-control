package de.randombyte.baustellalightcontrol

enum class MidiNotes(val byte: Byte, val noteName: String) {
    C3(60, "C3"),
    D3(61, "D3"),
    E3(62, "E3"),
    F4(63, "F3"),
    G4(63, "G3"),
    A4(63, "A3"),
    B4(63, "B3");

    companion object {
        val mapping = values().associate { it.byte to it.noteName }
        val reversedMapping = mapping.map { it.value to it.key }.toMap()
    }
}
