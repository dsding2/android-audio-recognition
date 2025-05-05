package app.danielding.voiceactivation.processor

import android.media.AudioRecord
import android.os.Process
import android.util.Log
import app.danielding.voiceactivation.AudioRecordInputStream
import app.danielding.voiceactivation.Globals
import be.tarsos.dsp.AudioDispatcher
import be.tarsos.dsp.AudioEvent
import be.tarsos.dsp.AudioProcessor
import be.tarsos.dsp.io.UniversalAudioInputStream
import be.tarsos.dsp.mfcc.MFCC
import kotlin.math.abs


class CaptureController (
    private val audioRecord: AudioRecord,
    private val onMfccRead: (Pair<Double, FloatArray>)->Unit,
    private val onMissedRead: ()->Unit
) : AutoCloseable {
    private lateinit var dispatcher: AudioDispatcher

    init {
        captureAudio()
    }

    override fun close() {
        dispatcher.stop()
    }

    private fun captureAudio() {
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
        val featureExtractor = FeatureExtractor(liveMfcc, true, onMfccRead)

        dispatcher = AudioDispatcher(audioStream, Globals.SAMPLES_PER_FRAME, Globals.BUFFER_OVERLAP)
        dispatcher.addAudioProcessor(featureExtractor)

        val validityChecker = object : AudioProcessor {
            var badReads = 0
            var lastRealTime = System.currentTimeMillis()
            var lastTimestamp = 0.0
            var counter = 0
            var missedReads = 0
            val INTERVAL = 100
            override fun process(audioEvent: AudioEvent): Boolean {
                if (audioEvent.rms == 0.0) {
                    badReads += 1
                    if (badReads > 10) {
                        restart()
                        badReads = 0
                    }
                } else {
                    badReads = 0
                }

                counter += 1
                if (counter > INTERVAL) {
                    counter = 0
                    val currentTime = System.currentTimeMillis()
                    val estReal = (currentTime - lastRealTime) / 1000.0
                    val estRead = audioEvent.timeStamp - lastTimestamp
                    lastRealTime = currentTime
                    lastTimestamp = audioEvent.timeStamp
                    Log.d("overprocessing", "${estReal/estRead}")

                    if (estRead > 0 && (estReal / estRead) > 1.1) {
                        missedReads += 1
                        if (missedReads == 2) {
                            onMissedRead()
                        }
                    }
                }
                return true
            }
            override fun processingFinished() {}
        }
        dispatcher.addAudioProcessor(validityChecker)

        Thread {
            Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO)
            dispatcher.run()
        }.start()
    }

    private fun restart() {
        audioRecord.stop()
        audioRecord.startRecording()
    }
}