package app.danielding.voiceactivation.processor

import android.util.Log
import app.danielding.voiceactivation.Globals
import be.tarsos.dsp.AudioEvent
import be.tarsos.dsp.AudioProcessor
import be.tarsos.dsp.mfcc.MFCC
import kotlin.collections.ArrayDeque
import kotlin.math.sin
import kotlin.math.sqrt

class FeatureExtractor(
    private val mfcc: MFCC,
    private val isLive : Boolean = false,
    private val onMfccRead: (Pair<Double, FloatArray>)->Unit
) : AudioProcessor {
    private var mfccBuffer = ArrayDeque<FloatArray>()
    private val deltaOrder = 2
    private val denominator = (1..deltaOrder).sumOf { it * it }.toFloat()


    override fun process(audioEvent: AudioEvent): Boolean {
        mfcc.process(audioEvent)
        val currentMfcc = mfcc.mfcc.clone()
//        onMfccRead(audioEvent.timeStamp to currentMfcc)
//        return true

        // Add current frame to buffer
        mfccBuffer.addLast(currentMfcc)

        // If we don’t yet have enough past frames, skip delta computation
        if (mfccBuffer.size < deltaOrder + 1) return true

        // Compute causal delta using current and previous N frames
        val delta = FloatArray(currentMfcc.size)

        for (i in currentMfcc.indices) {
            var numerator = 0.0f
            for (n in 1..deltaOrder) {
                val past = mfccBuffer.elementAt(mfccBuffer.size - 1 - n)
                numerator += n * (currentMfcc[i] - past[i])
            }
            delta[i] = (numerator / denominator)
        }

        // Send result to your handler
//        Log.d("AAAA", "${audioEvent.rms.toFloat()}")
        if (!isLive) {
            onMfccRead(audioEvent.timeStamp to (currentMfcc + delta + audioEvent.rms.toFloat()))
        } else {
            onMfccRead(System.currentTimeMillis()/1000.0 to (currentMfcc + delta + audioEvent.rms.toFloat()))
        }

        // Maintain buffer size
        if (mfccBuffer.size > deltaOrder) {
            mfccBuffer.removeFirst()
        }
        return true
    }

    override fun processingFinished() {
        mfcc.processingFinished()
    }

//    private fun lifter(mfcc: FloatArray, l: Int = 22): FloatArray {
//        val out = FloatArray(mfcc.size)
//        for (i in mfcc.indices) {
//            val lift = 1 + (l / 2.0f) * sin(Math.PI * i / l).toFloat()
//            out[i] = mfcc[i] * lift // Apply the lift
//        }
//        return out
//    }

    companion object {
        var mfccWeight = 40f
        var deltaWeight = 30f
        var volumeWeight = 60f
        fun reweight(data: Float, idx: Int, l: Int = 22): Float {
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
        fun zScoreNormalize(data: List<Pair<Double, FloatArray>>): List<Pair<Double, FloatArray>> {
            if (data.isEmpty()) return data

            val numArrays = data.size
            val arrayLength = data[0].second.size

            // Step 1: Compute mean for each index
            val means = FloatArray(arrayLength) { i ->
                data.sumOf { it.second[i].toDouble() }.toFloat() / numArrays
            }

//            // Step 2: Compute std deviation for each index
            val stdDevs = FloatArray(arrayLength) { i ->
                val mean = means[i]
                val variance = data.sumOf { ((it.second[i] - mean) * (it.second[i] - mean)).toDouble() } / numArrays
                sqrt(variance).toFloat()
            }

            // Step 3: Normalize using Z-score
            return data.map { arr ->
                arr.first to FloatArray(arrayLength) { i ->
                    val std = stdDevs[i]
                    if (std == 0f) 0f else reweight((arr.second[i] - means[i]) / std, i)
                }
            }
        }
        fun singleZScoreNormalize(data: Float, idx: Int, mean: Float, stdDev: Float) : Float {
            val safeStdDev = if (stdDev == 0f) 1f else stdDev
            return reweight((data - mean) /safeStdDev, idx)
        }
    }
}
