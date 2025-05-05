package app.danielding.voiceactivation

import android.util.Log
import app.danielding.voiceactivation.processor.FeatureExtractor
import app.danielding.voiceactivation.processor.FeatureExtractor.Companion.reweight

class CircularBuffer(private val capacity: Int) {
    private var sumArray = FloatArray(Globals.NUM_FEATURE_DIMENSIONS)
    private var sumSqArray = FloatArray(Globals.NUM_FEATURE_DIMENSIONS)

    private val buffer = Array<Pair<Double, FloatArray>>(capacity) { 0.0 to FloatArray(Globals.NUM_FEATURE_DIMENSIONS) }
    private var head = 0
    var size = 0
        private set

    fun add(item: Pair<Double, FloatArray>) {
        if (capacity == 0) {
            return
        }
        for (i in 0 until Globals.NUM_FEATURE_DIMENSIONS) {
            sumArray[i] += item.second[i]
            sumSqArray[i] += item.second[i]*item.second[i]
        }
        if (size >= capacity) {
            for (i in 0 until Globals.NUM_FEATURE_DIMENSIONS) {
                sumArray[i] -= buffer[head].second[i]
                sumSqArray[i] -= buffer[head].second[i] * buffer[head].second[i]
            }
        } else {
            size += 1
        }
        buffer[head] = item
        head = (head + 1) % capacity
    }

    fun mean(idx: Int): Float {
        return if (size == 0) 0.0f else sumArray[idx] / size
    }

    fun stdDev(idx: Int): Float {
        if (size == 0) return 1.0f
        val mean = sumArray[idx] / size
        return kotlin.math.sqrt((sumSqArray[idx] / size) - (mean * mean))
    }

    fun isFull(): Boolean = size == capacity

    fun clear() {
        head = 0
        size = 0
    }

    fun getNth(n: Int) : Pair<Double, FloatArray> {
        if (n < 0 || n >= size) {
            throw IndexOutOfBoundsException("Index $n out of bounds for size $size")
        }
        val index = (head - n - 1 + capacity) % capacity
        return buffer[index]
    }
    fun getNthNormalized(n: Int): Pair<Double, FloatArray> {
        val point = getNth(n)
        return point.first to point.second.mapIndexed{ idx, it -> normalize(it, idx) }.toFloatArray()
    }

    private fun normalize(value: Float, idx: Int) : Float {
        var stdDev = stdDev(idx)
        return (value - mean(idx)) / (if (stdDev == 0f) 1f else stdDev)
    }
}