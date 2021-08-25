package de.randombyte.baustellalightcontrol

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import java.io.File

class ConfigHolder<T : Any>(
    private val file: File,
    private val serializer: KSerializer<T>,
    private val default: T
) {

    companion object {
        inline fun <reified T : Any> init(file: File, default: T) = ConfigHolder(file, serializer(), default)
    }

    var config = default

    fun load() {
        if (!file.exists()) save()
        Json.decodeFromString(serializer, file.readText())
    }

    fun save() {
        file.writeText(Json.encodeToString(serializer, config))
    }
}
