package app.danielding.voiceactivation.processor

import android.content.Context
import android.util.Log
import app.danielding.voiceactivation.AudioStorage
import app.danielding.voiceactivation.CircularBuffer
import app.danielding.voiceactivation.Globals
import app.danielding.voiceactivation.R
import app.danielding.voiceactivation.TuningStorage
import be.tarsos.dsp.AudioDispatcher
import be.tarsos.dsp.AudioEvent
import be.tarsos.dsp.AudioProcessor
import be.tarsos.dsp.io.UniversalAudioInputStream
import be.tarsos.dsp.mfcc.MFCC
import com.fastdtw.dtw.FastDTW
import com.fastdtw.timeseries.TimeSeries
import com.fastdtw.timeseries.TimeSeriesBase
import com.fastdtw.timeseries.TimeSeriesPoint
import com.fastdtw.util.EuclideanDistance

class ReferenceComparison(
    context: Context,
    private val audioFilename: String,
    private val onSimilarity: (String)->Unit
) {
    var sensitivity = 1.0
    private var referenceTimeSeries: TimeSeries
    private val referenceMFCC = MFCC(
        Globals.SAMPLES_PER_FRAME,
        Globals.SAMPLE_RATE.toFloat(),
        Globals.MFCC_NUM_COEFFS,
        Globals.MFCC_NUM_FILTERS,
        Globals.MFCC_LOWER_CUTOFF,
        Globals.MFCC_UPPER_CUTOFF,
    )
    private var liveMfccBuffer: CircularBuffer
    private var lastDistance = 0.0
    private var lastSeenTime = 0.0
    private var clipLength = 0.0
    private var rollingDistAvg = 0.0

    init {
        Log.d("extracting file", audioFilename)
        referenceTimeSeries = extractMFCC(
            UniversalAudioInputStream(
                AudioStorage.getFile(context, audioFilename), Globals.TARSOS_AUDIO_FORMAT
            )
        )
        clipLength = referenceTimeSeries.getTimeAtNthPoint(referenceTimeSeries.size()-1)- referenceTimeSeries.getTimeAtNthPoint(0)
        liveMfccBuffer = CircularBuffer((referenceTimeSeries.size()*1.2).toInt())
        sensitivity = TuningStorage.getValue(context, audioFilename)
    }

    fun onNewCoefficients(point: Pair<Double, FloatArray>) {
        liveMfccBuffer.add(point)
        var liveList = liveMfccBuffer.toList()
        val currentTime = liveList.last().first
        val ts = buildTimeSeries(liveList)

        val seenDistance = FastDTW.compare(referenceTimeSeries, ts, EuclideanDistance()).distance
        if (currentTime < lastSeenTime + clipLength*1.5) {
            rollingDistAvg = seenDistance
        } else if (seenDistance < rollingDistAvg*.5*sensitivity) {
//            Log.d("Output!!", "Similar Audio Clip Detected")
            onSimilarity(audioFilename)
            lastSeenTime = currentTime
        }
//        Log.d("rolling avg", "$rollingDistAvg, $seenDistance")
        lastDistance = seenDistance
        rollingDistAvg = (rollingDistAvg * 99 + seenDistance)/100

//        Log.d("similarity", "$seenSimilarity")
    }

    fun getRollingAvg(): Double {
        return rollingDistAvg
    }
    fun getLastDistance(): Double {
        return lastDistance
    }

    fun getAudioFilename(): String {
        return audioFilename
    }

    private fun extractMFCC(inputStream: UniversalAudioInputStream): TimeSeries {
        val dispatcher =
            AudioDispatcher(inputStream, Globals.SAMPLES_PER_FRAME, Globals.BUFFER_OVERLAP)

        var outTimeSeries = TimeSeriesBase.builder()
        dispatcher.addAudioProcessor(referenceMFCC)
        dispatcher.addAudioProcessor(object : AudioProcessor {
            override fun process(audioEvent: AudioEvent): Boolean {
                val doubleArray = referenceMFCC.mfcc.map { it.toDouble() }.toDoubleArray()
                outTimeSeries.add(audioEvent.timeStamp, TimeSeriesPoint(doubleArray))
                return true
            }
            override fun processingFinished() {
            }
        })
        dispatcher.run()
        dispatcher.stop()
        return outTimeSeries.build()
    }

    private fun buildTimeSeries(coeffsList: List<Pair<Double, FloatArray>>) : TimeSeries {
        val builder = TimeSeriesBase.builder()
        coeffsList.forEach {
            val doubleArray = it.second.map { it.toDouble() }.toDoubleArray()
            builder.add(it.first, TimeSeriesPoint(doubleArray))
        }
        return builder.build()
    }
}