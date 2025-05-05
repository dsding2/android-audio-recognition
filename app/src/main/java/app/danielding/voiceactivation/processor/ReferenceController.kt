package app.danielding.voiceactivation.processor

import android.content.Context
import app.danielding.voiceactivation.AudioStorage
import app.danielding.voiceactivation.CircularBuffer
import kotlin.math.max
import android.util.Log


class ReferenceController (
    context: Context,
    onSimilarity: (String)->Unit
){
    private var referenceComparators = mutableMapOf<String, ReferenceComparison>()
    private var circularBuffer : CircularBuffer
    private var counter = 0
    init {
        val allAudio = AudioStorage.getAll(context)
        var maxLen = 0
        for (i in 0 until allAudio.size) {
            referenceComparators[allAudio[i].name] =
                ReferenceComparison(context, allAudio[i].name, onSimilarity)
            maxLen = max(maxLen, referenceComparators[allAudio[i].name]?.referenceList?.size ?: 0)
        }
        circularBuffer = CircularBuffer(maxLen * 2)
    }

    fun broadcastMfcc(point: Pair<Double, FloatArray>) {
        circularBuffer.add(point)
//        Log.d("AAA", "${circularBuffer.size}")
        if (counter < 3) {
            counter += 1
            return
        }
        counter = 0
        for (rc in referenceComparators) {
            rc.value.onNewCoefficients(circularBuffer)
//            rc.value.onNewCoefficients(ReferenceComparison.buildTimeSeries(circularBuffer.toList()))
        }
    }

    fun getReferenceComparator(filename: String): ReferenceComparison? {
        return referenceComparators[filename]
    }

    fun normalizeAll() {
        for (rc in referenceComparators) {
            rc.value.normalize()
        }
    }
}