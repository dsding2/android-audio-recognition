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
import app.danielding.voiceactivation.processor.ReferenceController
import app.danielding.voiceactivation.AudioRecordInputStream
import app.danielding.voiceactivation.AudioStorage
import app.danielding.voiceactivation.Globals
import app.danielding.voiceactivation.R
import app.danielding.voiceactivation.VideoStorage
import app.danielding.voiceactivation.processor.CaptureController
import be.tarsos.dsp.AudioDispatcher
import be.tarsos.dsp.AudioEvent
import be.tarsos.dsp.AudioProcessor
import be.tarsos.dsp.io.UniversalAudioInputStream
import be.tarsos.dsp.mfcc.MFCC

class PlaybackActivity : AppCompatActivity() {
    private lateinit var videoView: VideoView
    private lateinit var referenceController: ReferenceController
    private lateinit var audioRecord: AudioRecord
    private lateinit var captureController: CaptureController

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_playback)
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

        videoView = findViewById(R.id.videoView)
        videoView.setOnCompletionListener {
            setVideoToFilename("idle")
        }
        setVideoToFilename("idle")
    }

    override fun onDestroy() {
        super.onDestroy()
        captureController.close()
    }

    private fun onSimilarity(matchedAudio: String) {
        Log.d("on similarity", "$matchedAudio is similar")
        setVideoToFilename(matchedAudio)
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