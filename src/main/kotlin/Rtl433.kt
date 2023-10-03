package de.randombyte.baustellalightcontrol

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

class Rtl433(val onSignal: (data: String) -> Unit) {
    @Serializable
    data class SignalOutput(val data: String)

    companion object {
        private const val FILE_NAME = "rtl_433_64bit_static.exe"
    }

    private var file: File? = null

    fun init() {
        if (file != null) throw Exception("Can't call init() twice!")

        val tempFile = File.createTempFile("rtl_433_64bit_static", ".exe")
        file = tempFile
        this::class.java.classLoader.getResourceAsStream(FILE_NAME).use { inputStream ->
            if (inputStream == null) throw Exception("Can't find $FILE_NAME in resources!")
            Files.copy(inputStream, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
        tempFile.deleteOnExit()
    }

    fun start() {
        val tempFile = file ?: throw Exception("Can't call start() before initialization!")

        val process = ProcessBuilder(
            tempFile.absolutePath,
            "-R", "0", // disable all device protocols
            "-F", "json", // set output format to JSON
            "-X", "n=EV1527-Remote,m=OOK_PWM,s=369,l=1072,g=1400,r=12840,bits=25,repeats>=3,invert,unique" // set custom decoder
        ).start()

        Runtime.getRuntime().addShutdownHook(Thread {
            if (process.isAlive) process.destroy()
        })

        println("Started")

        Thread {
            process.inputStream.bufferedReader().use { reader ->
                val json = Json {
                    ignoreUnknownKeys = true
                }

                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    val signalOutput = try {
                        json.decodeFromString<SignalOutput>(line!!)
                    } catch (ex: Exception) {
                        println("Ignoring unparsable rtl_433 output: $line")
                        continue
                    }

                    println("Received signal: ${signalOutput.data}")
                    onSignal(signalOutput.data)
                }
            }
        }.start()
    }
}
