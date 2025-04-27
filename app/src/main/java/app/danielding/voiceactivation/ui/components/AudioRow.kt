package app.danielding.voiceactivation.ui.components

import android.content.Context
import android.widget.LinearLayout
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia

// Custom AudioRow class to contain both play and delete buttons
class AudioRow(context: Context, filename: String, launcher: ActivityResultLauncher<PickVisualMediaRequest>, onVideoSelect: (String)->Unit) : LinearLayout(context) {
    init {
        orientation = HORIZONTAL
        val layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        this.layoutParams = layoutParams


        // Create the play button (AudioButton) and add it to the row
        val playButton = AudioButton(context, filename)
        val deleteButton = DeleteButton(context, filename, this)
        val videoPickerButton = VideoPickerButton(context, launcher, filename, onVideoSelect)

        // Add both buttons to the row
        addView(playButton)
        addView(deleteButton)
        addView(videoPickerButton)
    }
}
