package app.danielding.voiceactivation

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

object AudioStorage {
    private val subdirName = "audio"
    fun writeDataToFile(context: Context, filename: String, data: ByteArray) {
        val subdir = File(context.filesDir, subdirName)
        if (!subdir.exists()) {
            val created = subdir.mkdirs()  // Create the subdirectory if it doesn't exist
            if (!created) return
        }
        val outputFile = File(subdir, filename)

        try {
            FileOutputStream(outputFile).use { fileOutputStream ->
                fileOutputStream.write(data)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun getAll(context: Context): List<File> {
        val subdir = File(context.filesDir, subdirName)

        // Check if the subdirectory exists and return a list of files
        return if (subdir.exists() && subdir.isDirectory) {
            subdir.listFiles()?.toList() ?: emptyList()
        } else {
            emptyList()
        }
    }

    fun getFile(context: Context, filename: String): File {
        val subdir = File(context.filesDir, subdirName)
        return File(subdir, filename)
    }
}