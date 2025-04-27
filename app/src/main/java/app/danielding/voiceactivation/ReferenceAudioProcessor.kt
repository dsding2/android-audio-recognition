package app.danielding.voiceactivation

import android.R
import android.content.Context
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import be.tarsos.dsp.AudioDispatcher
import be.tarsos.dsp.AudioEvent
import be.tarsos.dsp.AudioProcessor
import be.tarsos.dsp.io.TarsosDSPAudioFormat
import be.tarsos.dsp.mfcc.MFCC
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import android.util.Log
import be.tarsos.dsp.io.UniversalAudioInputStream
import com.fastdtw.timeseries.TimeSeries
import com.fastdtw.timeseries.TimeSeriesBase
import com.fastdtw.timeseries.TimeSeriesItem
import com.fastdtw.timeseries.TimeSeriesPoint
import java.io.InputStream


fun extractMFCC(inputStream: UniversalAudioInputStream, samplesPerFrame: Int, mfcc: MFCC): TimeSeries {
    // skip header in .wav
    inputStream.skip(44)
    val dispatcher =
        AudioDispatcher(inputStream, samplesPerFrame, samplesPerFrame/2)

    val mfccList: MutableList<FloatArray> = ArrayList<FloatArray>()

    var outTimeSeries = TimeSeriesBase.builder()
    dispatcher.addAudioProcessor(mfcc)
    dispatcher.addAudioProcessor(object : AudioProcessor {
        override fun process(audioEvent: AudioEvent): Boolean {
            val doubleArray = mfcc.mfcc.map { it.toDouble() }.toDoubleArray()
            outTimeSeries.add(audioEvent.timeStamp, TimeSeriesPoint(doubleArray))
            return true
        }

        override fun processingFinished() {
        }
    })

    dispatcher.run()

    return outTimeSeries.build()
}

fun printRow(arr: FloatArray, idx: Int) {
    var out = ""
    for (i in 0 until arr.size) {
        out += "${arr[i]} "
    }
    Log.d("row $idx", out)
}