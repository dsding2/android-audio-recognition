package app.danielding.voiceactivation.ui

import app.danielding.voiceactivation.R
import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.LinearLayout
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import app.danielding.voiceactivation.AudioRecorder
import app.danielding.voiceactivation.AudioStorage
import app.danielding.voiceactivation.Globals
import app.danielding.voiceactivation.VideoStorage
import app.danielding.voiceactivation.ui.components.AudioRow
import app.danielding.voiceactivation.ui.components.VideoPickerButton


class RecordActivity : ComponentActivity() {
    private lateinit var recordButton: Button
    private lateinit var audioLayout: LinearLayout
    private lateinit var audioRecorder: AudioRecorder
    private lateinit var audioRecord: AudioRecord
    private lateinit var pickMedia: ActivityResultLauncher<PickVisualMediaRequest>

    private var recording = false
    private var counter = 0
    private var lastFilename = "idle"


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_record)
        recordButton = findViewById(R.id.recordButton)
        audioLayout = findViewById(R.id.audioLayout)

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            Globals.SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            Globals.BUFFER_SIZE
        )
        audioRecorder = AudioRecorder(this, audioRecord)
        recordButton.setOnClickListener {
            if (!recording) {
                recording = true
                audioRecorder.startRecording("$counter.pcm")
                recordButton.text="Stop Recording"
            } else {
                recording = false
                audioRecorder.stopRecording()
                addRow("$counter.pcm")
                counter += 1
                recordButton.text="Record"
            }
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 1)
        }

        pickMedia = registerForActivityResult(PickVisualMedia()) { uri ->
            if (uri != null) {
                Log.d("PhotoPicker", "Selected URI: $uri, $lastFilename")
                saveVideo(lastFilename, uri)
            } else {
                Log.d("PhotoPicker", "No media selected")
            }
        }

        val idleVideoPickerLayout : LinearLayout = findViewById(R.id.idleVideoPickerLayout)
        val idleVideoPickerButton = VideoPickerButton(this, pickMedia, "idle") {
                filename->lastFilename=filename
        }
        idleVideoPickerLayout.addView(idleVideoPickerButton)

        val allFiles = AudioStorage.getAll(this)
        counter = allFiles.size
        for (file in allFiles) {
            addRow(file.name)
        }
    }

    override fun onPause() {
        super.onPause()
        audioRecorder.killRecording()
        recording = false
    }

    override fun onDestroy() {
        super.onDestroy()
        audioRecord.release()
    }

    private fun addRow(filename: String) {
        val audioRow = AudioRow(this, filename, pickMedia) { filename-> lastFilename=filename }
        // Set LayoutParams for the row
        val layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        layoutParams.topMargin = 16 // Optional margin
        audioRow.layoutParams = layoutParams

        // Add the row to the layout container
        audioLayout.addView(audioRow)
    }

    private fun saveVideo(filename: String, uri: Uri) {
        VideoStorage.putData(this, filename, uri)
    }
}
