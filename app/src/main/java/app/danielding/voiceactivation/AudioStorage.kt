package app.danielding.voiceactivation

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream

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
    fun deleteFile(context: Context, filename: String) {
        val subdir = File(context.filesDir, subdirName)
        val audioFile = File(subdir, filename)
        if (audioFile.exists()) {
            audioFile.delete()
        }
    }

    fun clear(context: Context) {
        val allFiles = getAll(context)
        for (file in allFiles) {
            deleteFile(context, file.name)
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
    fun getFileObj(context: Context, filename: String): File? {
        return try {
            val subdir = File(context.filesDir, subdirName)
            return File(subdir, filename)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    fun getFile(context: Context, filename: String): InputStream? {
        return try {
            val subdir = File(context.filesDir, subdirName)
            return FileInputStream(File(subdir, filename))
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    fun getFile(context: Context, file: File): InputStream? {
        return try {
            context.openFileInput(file.path)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}