package app.danielding.voiceactivation.processor

import android.media.AudioRecord
import android.os.Process
import app.danielding.voiceactivation.AudioRecordInputStream
import app.danielding.voiceactivation.CircularBuffer
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


class SimilarityDetector (
    referenceSeries: TimeSeries,
    private var onSimilarity: ()->Unit,
    private var audioRecord: AudioRecord,
    private val format: TarsosDSPAudioFormat
) : AutoCloseable {
    init {
        setReferenceSeries(referenceSeries)
    }
    private lateinit var referenceSeries: TimeSeries
    private var liveMFCC = MFCC(format.frameSize, format.sampleRate, 13, 20, 40f, 6000f)
    private var mfccBuffer = CircularBuffer(1000)
    private lateinit var dispatcher : AudioDispatcher
    private lateinit var referenceMFCCSeries: TimeSeries
    private var rollingDistAvg = 0.0
    private var referenceLength = 0.0
    private var lastSeenTime = 0.0
    private var recordStartTime = 0.0

    var capturingProcessor: AudioProcessor = object : AudioProcessor {
        override fun process(audioEvent: AudioEvent): Boolean {
            receiveMFCC(audioEvent.timeStamp to liveMFCC.mfcc.clone())
            return true
        }
        override fun processingFinished() {
        }
    }

    fun setReferenceSeries(newReference: TimeSeries) {
        referenceSeries = newReference
        referenceLength = referenceSeries.getTimeAtNthPoint(0) - referenceSeries.getTimeAtNthPoint(referenceSeries.size())
    }

    fun startAudioProcessing() {
        val audioStream = UniversalAudioInputStream(AudioRecordInputStream(audioRecord), format)
//        val audioStream = MyAudioInputStream(audioRecord, liveAudioFormat)
        dispatcher = AudioDispatcher(audioStream, format.frameSize, format.frameSize/2)
        dispatcher.addAudioProcessor(liveMFCC)
        dispatcher.addAudioProcessor(capturingProcessor)
        Thread {
            Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO)
            dispatcher.run()
        }.start()
    }

    override fun close() {
        dispatcher.stop()
    }

    private fun receiveMFCC(point: Pair<Double, FloatArray>) {
        mfccBuffer.add(point)
        var liveList = mfccBuffer.toList()
        val trimmedList = liveList.slice(max(0, liveList.size - (referenceMFCCSeries.size()*1.2).toInt()) until liveList.size)
        val currentTime = trimmedList.last().first
        val ts = buildTimeSeries(trimmedList)

        val seenDistance = FastDTW.compare(referenceMFCCSeries, ts, EuclideanDistance()).distance
        if (currentTime < lastSeenTime + referenceLength*1.5) {
            rollingDistAvg = seenDistance
        } else if (seenDistance < rollingDistAvg*.6) {
            onSimilarity()
            lastSeenTime = currentTime
        }
//        Log.d("rolling avg", "$rollingDistAvg, $seenDistance")
        rollingDistAvg = (rollingDistAvg * 99 + seenDistance)/100
    }

    companion object {
        fun buildTimeSeries(coeffsList: List<Pair<Double, FloatArray>>) : TimeSeries {
            val builder = TimeSeriesBase.builder()
            coeffsList.forEach {
                val doubleArray = it.second.map { it.toDouble() }.toDoubleArray()
                builder.add(it.first, TimeSeriesPoint(doubleArray))
            }
            return builder.build()
        }
    }
}
