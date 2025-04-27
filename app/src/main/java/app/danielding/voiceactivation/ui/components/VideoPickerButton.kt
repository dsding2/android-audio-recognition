package app.danielding.voiceactivation.ui.components

import android.content.Context
import android.content.Intent
import android.util.AttributeSet
import android.widget.Button
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.appcompat.widget.AppCompatButton

class VideoPickerButton (
    context: Context,
    private val launcher: ActivityResultLauncher<PickVisualMediaRequest>,
    private val filename: String,
    private val onVideoPicked: ((String)->Unit)
) : AppCompatButton(context) {
    init {
        text = "Pick a Video"
        setOnClickListener {
            onVideoPicked(filename)
            launcher.launch(PickVisualMediaRequest(PickVisualMedia.VideoOnly))
        }
    }
}
