package de.randombyte.baustellalightcontrol

import kotlinx.serialization.Serializable

@Serializable
data class Config(
    val bindings: List<Pair<String, Byte>>
) {
    companion object {
        fun <T> List<T>.replaceAt(index: Int, newElement: T) = toMutableList().apply {
            removeAt(index)
            add(index, newElement)
        }
    }
}
