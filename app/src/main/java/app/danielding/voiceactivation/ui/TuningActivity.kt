package app.danielding.voiceactivation.ui

import android.Manifest
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Bundle
import android.util.Log
import android.widget.LinearLayout
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import app.danielding.voiceactivation.AudioStorage
import app.danielding.voiceactivation.Globals
import app.danielding.voiceactivation.R
import app.danielding.voiceactivation.processor.CaptureController
import app.danielding.voiceactivation.processor.ReferenceController
import app.danielding.voiceactivation.ui.components.TuningRow

class TuningActivity : AppCompatActivity() {
    private lateinit var referenceController: ReferenceController
    private lateinit var audioRecord: AudioRecord
    private lateinit var captureController: CaptureController
    private lateinit var tuningAudioLayout: LinearLayout
    private var rowDict = mutableMapOf<String, TuningRow>()

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tuning)
        referenceController = ReferenceController(this, this::onSimilarity)

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            Globals.SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            Globals.BUFFER_SIZE
        )
        if (audioRecord.state != AudioRecord.STATE_INITIALIZED) {
            Log.d("debug audio", "init failed")
            return
        }
        captureController = CaptureController(audioRecord, referenceController::broadcastMfcc)
        tuningAudioLayout = findViewById(R.id.tuningAudioLayout)
        val allFiles = AudioStorage.getAll(this)
        for (file in allFiles) {
            addRow(file.name)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        captureController.close()
    }

    private fun onSimilarity(matchedAudio: String) {
        rowDict[matchedAudio]?.onSimilarity()
    }

    private fun addRow(filename: String) {
        val newRow = TuningRow(this, filename, referenceController.getReferenceComparator(filename))
        rowDict[filename] = newRow
        tuningAudioLayout.addView(newRow)
    }
}