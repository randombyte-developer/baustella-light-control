package de.randombyte.baustellalightcontrol

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
public class Config(
    val bindings: Map<String, Byte>
)
