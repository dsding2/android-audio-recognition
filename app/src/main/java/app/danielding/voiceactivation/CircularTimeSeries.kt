package app.danielding.voiceactivation

import android.util.Log
import com.fastdtw.timeseries.TimeSeries
import com.fastdtw.timeseries.TimeSeriesPoint
import kotlin.math.min

class CircularTimeSeries (
    private val circularBuffer: CircularBuffer,
    private val size: Int,
    private val reweight: (Float, Int) -> Float
) : TimeSeries {
    override fun size(): Int {
        return min(size, circularBuffer.size)
    }

    override fun numOfDimensions(): Int {
        return Globals.NUM_FEATURE_DIMENSIONS
    }

    override fun getTimeAtNthPoint(n: Int): Double {
//        Log.d("ct series", "$n, ${size()}, ${size() - 1 - n}")
        return getNthPoint(n).first
    }

    override fun getMeasurement(pointIndex: Int, valueIndex: Int): Double {
        return reweight(getNthPoint(pointIndex).second[valueIndex], valueIndex).toDouble()
    }

    override fun getMeasurementVector(pointIndex: Int): DoubleArray? {
        return getNthPoint(pointIndex).second.mapIndexed { idx, it -> reweight(it, idx).toDouble() }.toDoubleArray()
    }

    private fun getNthPoint(n: Int): Pair<Double, FloatArray> {
        return circularBuffer.getNthNormalized(size() - 1 - n)
    }
}