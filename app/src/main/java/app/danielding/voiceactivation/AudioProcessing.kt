package app.danielding.voiceactivation

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Process
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import be.tarsos.dsp.AudioDispatcher
import be.tarsos.dsp.AudioEvent
import be.tarsos.dsp.AudioProcessor
import be.tarsos.dsp.io.TarsosDSPAudioFormat
import be.tarsos.dsp.io.UniversalAudioInputStream
import be.tarsos.dsp.mfcc.MFCC
import com.fastdtw.dtw.FastDTW
import com.fastdtw.timeseries.TimeSeries
import com.fastdtw.timeseries.TimeSeriesBase
import com.fastdtw.timeseries.TimeSeriesPoint
import com.fastdtw.util.EuclideanDistance
import kotlin.math.max


class AudioProcessing : ComponentActivity() {
    private lateinit var heatmapView: MFCCHeatmap
    private lateinit var debugText: TextView
    private lateinit var counterText: TextView
    private val sampleRate = 44100
    private val bufferSize = AudioRecord.getMinBufferSize(
        sampleRate,
        AudioFormat.CHANNEL_IN_MONO,
        AudioFormat.ENCODING_PCM_16BIT
    )*2
//    private val bufferSize = 2048
    private val samplesPerFrame = 2048
    private val liveAudioFormat = TarsosDSPAudioFormat(sampleRate.toFloat(), 16, 1, true, false)
    private val referenceAudioFormat = TarsosDSPAudioFormat(sampleRate.toFloat(), 16, 1, true, false)
    private var liveMFCC = MFCC(samplesPerFrame, sampleRate.toFloat(), 13, 20, 40f, 6000f)
    private var referenceMFCC = MFCC(samplesPerFrame, sampleRate.toFloat(), 13, 20, 40f, 6000f)
    private var mfccBufferSize = 100
    private var mfccBuffer = CircularBuffer(mfccBufferSize)
    private lateinit var dispatcher : AudioDispatcher
    private lateinit var referenceMFCCSeries: TimeSeries
    private var rollingDistAvg = 0.0
    private var wantToRecord = false
    private var wantToStopRecording = false
    private var recording = false
    private var clipLength = 0.0
    private var lastSeenTime = 0.0
    private var recordStartTime = 0.0
    private var counter = 0

    var capturingProcessor: AudioProcessor = object : AudioProcessor {
        override fun process(audioEvent: AudioEvent): Boolean {
            receiveMFCC(audioEvent.timeStamp to liveMFCC.mfcc.clone())
            return true
        }

        override fun processingFinished() {
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.audio_debug)
        heatmapView = findViewById(R.id.MFCCView)
        debugText = findViewById(R.id.amplitudeTextView)
        counterText = findViewById(R.id.counterText)

        val button: Button = findViewById(R.id.recordButton2)


        // Set an OnClickListener to trigger action when the button is clicked
        button.setOnClickListener {
            if (!recording) {
                wantToRecord = true
                wantToStopRecording = false
                button.text="Stop Recording"
            } else {
                button.text="Record"
                wantToRecord = false
                wantToStopRecording = true
            }
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 1)
        } else {
            referenceMFCCSeries = extractMFCC(UniversalAudioInputStream(resources.openRawResource(R.raw.trimmed), referenceAudioFormat), samplesPerFrame, referenceMFCC)
            clipLength = referenceMFCCSeries.getTimeAtNthPoint(referenceMFCCSeries.size()-1) - referenceMFCCSeries.getTimeAtNthPoint(0)
            Log.d("reference length", "${referenceMFCCSeries.size()}")
            startAudioProcessing()
        }
    }

    private fun startAudioProcessing() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            return
        }
        val audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize
        )
        if (audioRecord.state != AudioRecord.STATE_INITIALIZED) {
            Log.d("debug audio", "init failed")
            return
        }
//        debugAudioCapture(audioRecord)
        tarsosAudioCapture(audioRecord)
    }

    private fun tarsosAudioCapture(audioRecord: AudioRecord) {
        val audioStream = UniversalAudioInputStream(AudioRecordInputStream(audioRecord), liveAudioFormat)
//        val audioStream = MyAudioInputStream(audioRecord, liveAudioFormat)
        dispatcher = AudioDispatcher(audioStream, samplesPerFrame, samplesPerFrame/2)
        dispatcher.addAudioProcessor(liveMFCC)
        dispatcher.addAudioProcessor(capturingProcessor)
        Thread {
            Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO)
            dispatcher.run()
        }.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        dispatcher.stop()
    }

    private fun receiveMFCC(point: Pair<Double, FloatArray>) {
        mfccBuffer.add(point)
        var liveList = mfccBuffer.toList()
        val trimmedList = liveList.slice(max(0, liveList.size - (referenceMFCCSeries.size()*1.2).toInt()) until liveList.size)
        val currentTime = trimmedList.last().first
        if (recording) {
            writeText(String.format("%.5f", currentTime - recordStartTime))
        } else {
            writeText(String.format("%.5f", clipLength))
        }
        heatmapView.readMFCCCoefficients(trimmedList, recording)
        val ts = buildTimeSeries(trimmedList)

        if (wantToRecord) {
            wantToRecord = false
            recording = true
            recordStartTime = currentTime
        } else if (recording && wantToStopRecording) {
            recording = false
            wantToStopRecording = false
            referenceMFCCSeries = ts
            clipLength = currentTime - recordStartTime
        }
        val seenDistance = FastDTW.compare(referenceMFCCSeries, ts, EuclideanDistance()).distance
        if (currentTime < lastSeenTime + clipLength*1.5) {
            rollingDistAvg = seenDistance
        } else if (seenDistance < rollingDistAvg*.6) {
            Log.d("Output!!", "Similar Audio Clip Detected")
            counter += 1
            writeOtherText("$counter")
            lastSeenTime = currentTime
        }
//        Log.d("rolling avg", "$rollingDistAvg, $seenDistance")
        rollingDistAvg = (rollingDistAvg * 99 + seenDistance)/100

//        Log.d("similarity", "$seenSimilarity")
    }

    private fun buildTimeSeries(coeffsList: List<Pair<Double, FloatArray>>) : TimeSeries {
        val builder = TimeSeriesBase.builder()
        coeffsList.forEach {
            val doubleArray = it.second.map { it.toDouble() }.toDoubleArray()
            builder.add(it.first, TimeSeriesPoint(doubleArray))
        }
        return builder.build()
    }

    private fun writeText(msg: String) {
        runOnUiThread { debugText.text = msg }
    }
    private fun writeOtherText(msg: String) {
        runOnUiThread { counterText.text = msg }
    }
}
