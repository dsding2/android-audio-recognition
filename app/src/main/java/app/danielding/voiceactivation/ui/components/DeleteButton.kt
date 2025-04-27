package app.danielding.voiceactivation.ui.components
import android.content.Context
import android.widget.Button
import android.widget.LinearLayout
import java.io.File

// Custom DeleteButton class to delete audio file and its associated row
class DeleteButton(context: Context, private val audioFileName: String, private val parentRow: AudioRow) : Button(context) {

    init {
        text = "Delete Audio: $audioFileName"
        setOnClickListener {
            deleteAudioFile()
        }
    }

    private fun deleteAudioFile() {
        try {
            // Delete the associated audio file from internal storage (filesDir)
            val audioFile = File(context.filesDir, audioFileName)
            if (audioFile.exists()) {
                audioFile.delete()
                println("Audio file deleted: $audioFileName")
            }

            // Remove the parent AudioRow from its parent container (LinearLayout)
            val parentLayout = parentRow.parent as LinearLayout
            parentLayout.removeView(parentRow)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
