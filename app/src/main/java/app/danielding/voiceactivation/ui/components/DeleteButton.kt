package app.danielding.voiceactivation.ui.components
import android.content.Context
import android.widget.Button
import android.widget.LinearLayout
import androidx.appcompat.widget.AppCompatButton
import app.danielding.voiceactivation.AudioStorage
import app.danielding.voiceactivation.VideoStorage
import java.io.File

// Custom DeleteButton class to delete audio file and its associated row
class DeleteButton(context: Context, private val audioFilename: String, private val parentRow: AudioRow) : AppCompatButton(context) {

    init {
        text = "Delete Audio: $audioFilename"
        setOnClickListener {
            deleteAudioFile()
        }
    }

    private fun deleteAudioFile() {
        try {
            // Delete the associated audio file from internal storage (filesDir)
            AudioStorage.deleteFile(context, audioFilename)
            VideoStorage.deleteVideo(context, audioFilename)

            // Remove the parent AudioRow from its parent container (LinearLayout)
            val parentLayout = parentRow.parent as LinearLayout
            parentLayout.removeView(parentRow)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
