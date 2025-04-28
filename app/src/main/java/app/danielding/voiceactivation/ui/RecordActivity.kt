package app.danielding.voiceactivation.ui

import app.danielding.voiceactivation.R
import android.Manifest
import android.content.Intent
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
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import app.danielding.voiceactivation.AudioRecorder
import app.danielding.voiceactivation.AudioStorage
import app.danielding.voiceactivation.Globals
import app.danielding.voiceactivation.VideoStorage
import app.danielding.voiceactivation.TuningStorage
import app.danielding.voiceactivation.ui.components.AudioRow
import app.danielding.voiceactivation.ui.components.VideoPickerButton
import app.danielding.voiceactivation.ui.PlaybackActivity


class RecordActivity : AppCompatActivity() {
    private lateinit var recordButton: Button
    private lateinit var audioLayout: LinearLayout
    private lateinit var audioRecorder: AudioRecorder
    private lateinit var audioRecord: AudioRecord
    private lateinit var pickMedia: ActivityResultLauncher<PickVisualMediaRequest>

    private var recording = false
    private var counter = 0
    private var lastFilename = "idle"
    private val usedNames = mutableSetOf<Int>()

//    private lateinit var playbackIntent : Intent


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_record)
        recordButton = findViewById(R.id.recordButton)
        audioLayout = findViewById(R.id.audioLayout)
        val tuningLinkButton : Button = findViewById(R.id.tuningLinkButton)
        tuningLinkButton.setOnClickListener {
            val intent = Intent(this, TuningActivity::class.java)
            startActivity(intent)
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            return
        }
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
                audioRecorder.startRecording("${getId()}")
                recordButton.text="Stop Recording"
            } else {
                recording = false
                audioRecorder.stopRecording()
                usedNames.add(getId())
                addRow("${getId()}")
                incrementId()
                recordButton.text="Record"
            }
        }

        val deleteButton : Button = findViewById(R.id.clearDataButton)
        deleteButton.setOnClickListener {
            AudioStorage.clear(this)
            VideoStorage.clear(this)
            TuningStorage.clear(this)
            audioLayout.removeAllViews()
        }

        val playbackLinkButton : Button = findViewById(R.id.playbackLinkButton)
        playbackLinkButton.setOnClickListener {
            val intent = Intent(this, PlaybackActivity::class.java)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            startActivity(intent)
        }

        pickMedia = registerForActivityResult(PickVisualMedia()) { uri ->
            if (uri != null) {
//                Log.d("PhotoPicker", "Selected URI: $uri, $lastFilename")
                saveVideo(lastFilename, uri)
            } else {
//                Log.d("PhotoPicker", "No media selected")
            }
        }

        val idleVideoPickerLayout : LinearLayout = findViewById(R.id.idleVideoPickerLayout)
        val idleVideoPickerButton = VideoPickerButton(this, pickMedia, "idle") {
                filename->lastFilename=filename
        }
        idleVideoPickerButton.text = "Set Idle Video"
        idleVideoPickerLayout.addView(idleVideoPickerButton)

        val allFiles = AudioStorage.getAll(this)
        for (file in allFiles) {
            usedNames.add(file.name.toIntOrNull() ?: -1)
            addRow(file.name)
        }
        incrementId()
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
        VideoStorage.saveVideo(this, filename, uri)
    }

    private fun incrementId() {
        while (counter in usedNames) {
            counter += 1
        }
    }
    private fun getId(): Int {
        return counter
    }
}
