package app.danielding.voiceactivation.processor

import android.content.Context
import android.util.Log
import app.danielding.voiceactivation.AudioStorage
import app.danielding.voiceactivation.CircularBuffer
import app.danielding.voiceactivation.Globals
import app.danielding.voiceactivation.TuningStorage
import be.tarsos.dsp.AudioDispatcher
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
    private var referenceList = mutableListOf<Pair<Double, FloatArray>>()
    private var liveMfccBuffer: CircularBuffer
    private var lastDistance = 0.0
    private var lastSeenTime = 0.0
    private var clipLength = 0.0
    private var rollingDistAvg = 0.0
    private var similarityDetectedTime = -1.0
    private var alpha = 1.0/100.0

    init {
        extractMFCC(
            UniversalAudioInputStream(
                AudioStorage.getFile(context, audioFilename), Globals.TARSOS_AUDIO_FORMAT
            )
        )
        referenceTimeSeries = buildTimeSeries(referenceList)
//        Log.d("reference time series len", "${referenceTimeSeries.size()}, ${referenceTimeSeries.numOfDimensions()}")
        clipLength = referenceTimeSeries.getTimeAtNthPoint(referenceTimeSeries.size()-1)- referenceTimeSeries.getTimeAtNthPoint(0)
        liveMfccBuffer = CircularBuffer((referenceTimeSeries.size()*1.2).toInt())
        sensitivity = TuningStorage.getValue(context, audioFilename)
        alpha = 1.0/50.0 / clipLength
        Log.d("rolling avg alpha", "$alpha")
    }

    fun onNewCoefficients(point: Pair<Double, FloatArray>) {
        liveMfccBuffer.add(point)
        var liveList = liveMfccBuffer.toList()
        val currentTime = liveList.last().first
        val ts = buildTimeSeries(liveList)

        val seenDistance = FastDTW.compare(referenceTimeSeries, ts, EuclideanDistance()).distance
        if (seenDistance < rollingDistAvg && similarityDetectedTime < 0) {
            similarityDetectedTime = currentTime
        } else if (similarityDetectedTime > 0 && seenDistance >= rollingDistAvg) {
//            Log.d("rolling avg time", "${(currentTime - similarityDetectedTime)/clipLength}")
//            if (similarityDetectedTime < lastSeenTime && currentTime - similarityDetectedTime > clipLength*.6) {
//                onSimilarity(audioFilename)
//            }
            similarityDetectedTime = -1.0

        }
        if (currentTime < lastSeenTime + clipLength *2) {
//            rollingDistAvg = seenDistance
        } else if (seenDistance < rollingDistAvg*sensitivity) {
//            Log.d("rolling avg output", "Similar Audio Clip Detected")
//            sharpSimilarityDetectedTime = currentTime
            onSimilarity(audioFilename)
            lastSeenTime = currentTime
        }
        if (rollingDistAvg > 0) {
            Log.d("rolling avg disp", "${seenDistance/rollingDistAvg}")
        }
        lastDistance = seenDistance
        rollingDistAvg = rollingDistAvg * (1-alpha) + seenDistance * alpha

//        Log.d("similarity", "$seenSimilarity")
    }

    private fun addReferenceCoefficients(point: Pair<Double, FloatArray>) {
        referenceList.add(point)
    }

    private fun extractMFCC(inputStream: UniversalAudioInputStream) {
        val dispatcher =
            AudioDispatcher(inputStream, Globals.SAMPLES_PER_FRAME, Globals.BUFFER_OVERLAP)

//        dispatcher.addAudioProcessor(referenceMFCC)
        val featureExtractor = FeatureExtractor(referenceMFCC, this::addReferenceCoefficients)
        dispatcher.addAudioProcessor(featureExtractor)
        dispatcher.run()
        dispatcher.stop()
    }

    private fun buildTimeSeries(coeffsList: List<Pair<Double, FloatArray>>) : TimeSeries {
        val builder = TimeSeriesBase.builder()
        val normalizedList = FeatureExtractor.zScoreNormalize(coeffsList)
        normalizedList.forEach {
            val doubleArray = it.second.map { it.toDouble() }.toDoubleArray()
            builder.add(it.first, TimeSeriesPoint(doubleArray))
        }
        return builder.build()
    }

    fun renormalize() {
        referenceTimeSeries = buildTimeSeries(referenceList)
    }
}