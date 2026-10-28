package com.ertyu.engine

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

object ShellExecutor {

    suspend fun execute(command: String, useRoot: Boolean): String = withContext(Dispatchers.IO) {
        try {
            val finalCmd = if (useRoot) "su -c \"$command\"" else command
            val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", finalCmd))
            val output = StringBuilder()
            val error = StringBuilder()

            val outputReader = BufferedReader(InputStreamReader(process.inputStream))
            val errorReader = BufferedReader(InputStreamReader(process.errorStream))

            var line: String?
            while (outputReader.readLine().also { line = it } != null) {
                output.append(line).append("\n")
            }
            while (errorReader.readLine().also { line = it } != null) {
                error.append(line).append("\n")
            }

            val exitCode = process.waitFor()
            if (exitCode != 0 && error.isNotEmpty()) {
                "Error (Code $exitCode):\n$error"
            } else {
                if (error.isNotEmpty()) output.append("\nStderr:\n").append(error)
                output.toString()
            }
        } catch (e: Exception) {
            "执行异常: ${e.message}"
        }
    }
}