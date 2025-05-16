package app.danielding.voiceactivation.processor

import android.content.Context
import app.danielding.voiceactivation.AudioStorage
import app.danielding.voiceactivation.CircularBuffer
import app.danielding.voiceactivation.CircularTimeSeries
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
import kotlin.math.sin
import kotlin.math.sqrt

class ReferenceComparison(
    private val context: Context,
    private val filename: String,
    private val numComparisons: Int,
    private val onSimilarity: (String)->Unit,
    var onCheck: ((Double)->Unit)? = null
) {
    var sensitivity = 1.0
    private var referenceTimeSeries: TimeSeries? = null
    private val referenceMFCC = MFCC(
        Globals.SAMPLES_PER_FRAME,
        Globals.SAMPLE_RATE.toFloat(),
        Globals.MFCC_NUM_COEFFS,
        Globals.MFCC_NUM_FILTERS,
        Globals.MFCC_LOWER_CUTOFF,
        Globals.MFCC_UPPER_CUTOFF,
    )
    var referenceList = mutableListOf<Pair<Double, FloatArray>>()
    private var lastDistance = 0.0
    private var lastSeenTime = 0.0
    private var clipLength = 0.0
    private var rollingDistAvg = 0.0
    private var similarityDetectedTime = -1.0
    private var alpha = 1.0/100.0
    var mfccWeight = TuningStorage.getWeight(context, filename, TuningStorage.WeightType.MFCC).toFloat()
    var deltaWeight = TuningStorage.getWeight(context, filename, TuningStorage.WeightType.DELTA).toFloat()
    var volumeWeight = TuningStorage.getWeight(context, filename, TuningStorage.WeightType.VOLUME).toFloat()

    init {
        extractMFCC(
            UniversalAudioInputStream(
                AudioStorage.getFile(context, filename), Globals.TARSOS_AUDIO_FORMAT
            )
        )
        sensitivity = TuningStorage.getValue(context, filename)
        if (referenceList.isNotEmpty()) {
            referenceTimeSeries = buildTimeSeries(referenceList)
            clipLength = referenceTimeSeries!!.getTimeAtNthPoint(referenceTimeSeries!!.size()-1)- referenceTimeSeries!!.getTimeAtNthPoint(0)
            sensitivity = TuningStorage.getValue(context, filename)
            alpha = (numComparisons * Globals.SKIPPED_FRAMES) * 2.0/50.0 / clipLength
        }
    }

    fun onNewCoefficients(circularBuffer: CircularBuffer) {
//        Log.d("WEIGHTS", "$mfccWeight, $deltaWeight, $volumeWeight")
        if (referenceList.isEmpty()) {
            return
        }
        val timeSeries = CircularTimeSeries(circularBuffer, (referenceList.size*1.1 + Globals.SKIPPED_FRAMES*numComparisons).toInt(), this::reweight)
        val currentTime = timeSeries.getTimeAtNthPoint(0)
        val seenDistance = FastDTW.compare(referenceTimeSeries, timeSeries, EuclideanDistance()).distance
        if (seenDistance < rollingDistAvg && similarityDetectedTime < 0) {
            similarityDetectedTime = currentTime
        } else if (similarityDetectedTime > 0 && seenDistance >= rollingDistAvg) {
            similarityDetectedTime = -1.0
        }
        if (currentTime < lastSeenTime + clipLength *2) {
//            rollingDistAvg = seenDistance
        } else if (seenDistance < rollingDistAvg*sensitivity) {
            onSimilarity(filename)
            lastSeenTime = currentTime
        }

        if (rollingDistAvg > 0 && onCheck != null) {
            onCheck?.invoke(seenDistance/rollingDistAvg)
        }
//        Log.d("AA", "$seenDistance, $rollingDistAvg, ${seenDistance/rollingDistAvg}")
        lastDistance = seenDistance
        rollingDistAvg = rollingDistAvg * (1-alpha) + seenDistance * alpha
    }

    private fun addReferenceCoefficients(point: Pair<Double, FloatArray>) {
        referenceList.add(point)
    }

    private fun extractMFCC(inputStream: UniversalAudioInputStream) {
        val dispatcher =
            AudioDispatcher(inputStream, Globals.SAMPLES_PER_FRAME, Globals.BUFFER_OVERLAP)

//        dispatcher.addAudioProcessor(referenceMFCC)
        val featureExtractor = FeatureExtractor(referenceMFCC, false, this::addReferenceCoefficients)
        dispatcher.addAudioProcessor(featureExtractor)
        dispatcher.run()
        dispatcher.stop()
    }

    fun buildTimeSeries(coeffsList: List<Pair<Double, FloatArray>>) : TimeSeries {
        val builder = TimeSeriesBase.builder()
        val normalizedList = zScoreNormalize(coeffsList)
        normalizedList.forEach {
            val doubleArray = it.second.map { it.toDouble() }.toDoubleArray()
            builder.add(it.first, TimeSeriesPoint(doubleArray))
        }
        return builder.build()
    }

    fun normalize() {
        mfccWeight = TuningStorage.getWeight(context, filename, TuningStorage.WeightType.MFCC).toFloat()
        deltaWeight = TuningStorage.getWeight(context, filename, TuningStorage.WeightType.DELTA).toFloat()
        volumeWeight = TuningStorage.getWeight(context, filename, TuningStorage.WeightType.VOLUME).toFloat()

        referenceTimeSeries = buildTimeSeries(referenceList)
    }

    private fun reweight(data: Float, idx: Int, l: Int = 22): Float {
        if (idx < Globals.MFCC_NUM_COEFFS) {
            val lift = 1 + (l / 2.0f) * sin(Math.PI * idx / l).toFloat()
            return data * lift * mfccWeight // Apply the lift
        }
        if (idx < Globals.MFCC_NUM_COEFFS*2) {
            val lift = 1 + (l / 2.0f) * sin(Math.PI * (idx - Globals.MFCC_NUM_COEFFS) / l).toFloat()
            return data * lift * deltaWeight // Apply the lift
        }
        return data * volumeWeight * 5f
    }
    private fun zScoreNormalize(data: List<Pair<Double, FloatArray>>): List<Pair<Double, FloatArray>> {
        if (data.isEmpty()) return data

        val numArrays = data.size
        val arrayLength = data[0].second.size

        val means = FloatArray(arrayLength) { i ->
            data.sumOf { it.second[i].toDouble() }.toFloat() / numArrays
        }

        val stdDevs = FloatArray(arrayLength) { i ->
            val mean = means[i]
            val variance = data.sumOf { ((it.second[i] - mean) * (it.second[i] - mean)).toDouble() } / numArrays
            sqrt(variance).toFloat()
        }

        return data.map { arr ->
            arr.first to FloatArray(arrayLength) { i ->
                val std = stdDevs[i]
                if (std == 0f) 0f else reweight((arr.second[i] - means[i]) / std, i)
            }
        }
    }
}