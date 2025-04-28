package app.danielding.voiceactivation.ui

import android.Manifest
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.net.Uri
import android.os.Bundle
import android.os.Process
import android.util.Log
import android.widget.VideoView
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import app.danielding.voiceactivation.AudioRecordInputStream
import app.danielding.voiceactivation.AudioStorage
import app.danielding.voiceactivation.Globals
import app.danielding.voiceactivation.R
import app.danielding.voiceactivation.VideoStorage
import app.danielding.voiceactivation.processor.ReferenceComparison
import be.tarsos.dsp.AudioDispatcher
import be.tarsos.dsp.AudioEvent
import be.tarsos.dsp.AudioProcessor
import be.tarsos.dsp.io.UniversalAudioInputStream
import be.tarsos.dsp.mfcc.MFCC
import java.io.File

class TuningActivity : AppCompatActivity() {
    private lateinit var referenceComparators: Array<ReferenceComparison?>
    private lateinit var dispatcher: AudioDispatcher
    private lateinit var audioRecord: AudioRecord

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_playback)

        val allAudio = AudioStorage.getAll(this)
        referenceComparators = Array<ReferenceComparison?>(allAudio.size) { null }
        for (i in 0 until allAudio.size) {
            referenceComparators[i] =
                ReferenceComparison(this, allAudio[i].name, this::onSimilarity)
        }
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
        captureAudio(audioRecord)

        videoView = findViewById(R.id.videoView)
        videoView.setOnCompletionListener {
            setVideoToFilename("idle")
        }
        setVideoToFilename("idle")
    }

    override fun onDestroy() {
        super.onDestroy()
        dispatcher.stop()
    }

    private fun onSimilarity(matchedAudio: String) {
        Log.d("on similarity", "$matchedAudio is similar")
        setVideoToFilename(matchedAudio)
    }

    private fun captureAudio(audioRecord: AudioRecord) {
        val audioStream = UniversalAudioInputStream(
            AudioRecordInputStream(audioRecord),
            Globals.TARSOS_AUDIO_FORMAT
        )
        val liveMfcc = MFCC(
            Globals.SAMPLES_PER_FRAME,
            Globals.SAMPLE_RATE.toFloat(),
            Globals.MFCC_NUM_COEFFS,
            Globals.MFCC_NUM_FILTERS,
            Globals.MFCC_LOWER_CUTOFF,
            Globals.MFCC_UPPER_CUTOFF,
        )
        val capturingProcessor: AudioProcessor = object : AudioProcessor {
            override fun process(audioEvent: AudioEvent): Boolean {
                broadcastMfcc(audioEvent.timeStamp to liveMfcc.mfcc.clone())
                return true
            }
            override fun processingFinished() {
            }
        }

        dispatcher = AudioDispatcher(audioStream, Globals.SAMPLES_PER_FRAME, Globals.BUFFER_OVERLAP)
        dispatcher.addAudioProcessor(liveMfcc)
        dispatcher.addAudioProcessor(capturingProcessor)
        Thread {
            Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO)
            dispatcher.run()
        }.start()
    }
    private fun broadcastMfcc(point: Pair<Double, FloatArray>) {
        for (rc in referenceComparators) {
            rc?.onNewCoefficients(point)
        }
    }

    private fun setVideoToFilename(filename: String) {
        val file = VideoStorage.getFile(this, filename)
        if (file != null) {
            runOnUiThread {
                videoView.setVideoURI(Uri.fromFile(file))
                videoView.start()
            }
        } else {
            Log.d("PlaybackActivity", "Video $filename not found")
        }
    }
}