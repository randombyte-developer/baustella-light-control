package de.randombyte.baustellalightcontrol

import kotlinx.serialization.Serializable

@Serializable
data class Config(
    val bindings: Map<String, Byte>
)
