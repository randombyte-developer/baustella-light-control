package de.randombyte.blc

import java.io.File
import kotlin.jvm.optionals.getOrNull

object QlcPlus {
    private const val QLC_PLUS_EXECUTABLE = "C:\\QLC+5\\qlcplus.exe"

    fun isRunning(): Boolean {
        val countQlcPlusProcesses = ProcessHandle
            .allProcesses()
            .filter { it.info().command().getOrNull() == QLC_PLUS_EXECUTABLE }
            .count()
        return countQlcPlusProcesses > 0
    }

    fun findTheOnlyProjectFile(): File? {
        val home = System.getProperty("user.home")
        val documents = File(home).resolve("Documents")
        println("Documents directory: ${documents.absolutePath}")
        return documents
            .walk()
            .maxDepth(1)
            .filter { it.isFile }
            .filter { it.extension == "qxw" }
            .singleOrNull()
    }

    fun start(projectFile: File) {
        // starts the process in the background, the JVM doesn't kill it at shutdown
        ProcessBuilder(
            QLC_PLUS_EXECUTABLE,
            "--open", projectFile.absolutePath,
            "--operate" // start in "Live" mode
        ).start()
    }
}