package app.danielding.voiceactivation.processor

import android.Manifest
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.net.Uri
import android.os.Bundle
import android.os.Process
import android.util.Log
import android.widget.VideoView
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.autofill.Autofill
import app.danielding.voiceactivation.AudioRecordInputStream
import app.danielding.voiceactivation.AudioStorage
import app.danielding.voiceactivation.Globals
import app.danielding.voiceactivation.R
import app.danielding.voiceactivation.VideoStorage
import be.tarsos.dsp.AudioDispatcher
import be.tarsos.dsp.AudioEvent
import be.tarsos.dsp.AudioProcessor
import be.tarsos.dsp.io.UniversalAudioInputStream
import be.tarsos.dsp.mfcc.MFCC


class CaptureController (
    audioRecord: AudioRecord,
    private val onMfccRead: (Pair<Double, FloatArray>)->Unit
) : AutoCloseable {
    private lateinit var dispatcher: AudioDispatcher
    init {
        captureAudio(audioRecord)
    }

    override fun close() {
        dispatcher.stop()
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
                onMfccRead(audioEvent.timeStamp to liveMfcc.mfcc.clone())
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
}