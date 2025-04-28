package app.danielding.voiceactivation

import android.content.Context
import android.net.Uri
import android.util.Log

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import androidx.core.net.toUri
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream

object VideoStorage {
    private val subdirName = "video"
    fun getFile(context: Context, filename: String): File? {
        return try {
            val subdir = File(context.filesDir, subdirName)
            return File(subdir, filename)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun saveVideo(context: Context, audioFilename: String, videoUri: Uri): Boolean {
        val filename = audioFilename
        val subdir = File(context.filesDir, subdirName)
        if (!subdir.exists()) {
            val created = subdir.mkdirs()  // Create the subdirectory if it doesn't exist
            if (!created) return false
        }

        deleteVideo(context, audioFilename)
        val videoFile = File(subdir, filename)
        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(videoUri)
            inputStream?.let {
                val fileOutputStream = FileOutputStream(videoFile)
                val buffer = ByteArray(1024)
                var length: Int
                while (it.read(buffer).also { n -> length = n } > 0) {
                    fileOutputStream.write(buffer, 0, length)
                }
                it.close()
                fileOutputStream.close()
                true
            } == true
        } catch (e: IOException) {
            Log.e("VideoStorage", "Error saving video", e)
            false
        }
    }

    fun deleteVideo(context: Context, filename: String) {
        getFile(context, filename)?.delete()
    }

    fun clear(context: Context) {
        val directory = File(context.filesDir, subdirName)

        if (directory.exists() && directory.isDirectory) {
            // Get all files in the directory (excluding subdirectories)
            directory.listFiles()?.forEach { it.delete() }
        }
    }
}
