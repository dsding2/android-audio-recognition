package app.danielding.voiceactivation.processor

import android.content.Context
import app.danielding.voiceactivation.AudioStorage
import app.danielding.voiceactivation.CircularBuffer
import kotlin.math.max
import android.util.Log
import app.danielding.voiceactivation.Globals


class ReferenceController (
    context: Context,
    onSimilarity: (String)->Unit
){
    private var referenceComparators = mutableMapOf<String, ReferenceComparison>()
    private var circularBuffer : CircularBuffer
    private var counter = 0
    private val allAudio = AudioStorage.getAll(context)
    private var muxer = 0
    init {
        var maxLen = 0
        for (i in 0 until allAudio.size) {
            referenceComparators[allAudio[i].name] =
                ReferenceComparison(context, allAudio[i].name, allAudio.size, onSimilarity)
            maxLen = max(maxLen, referenceComparators[allAudio[i].name]?.referenceList?.size ?: 0)
        }
        circularBuffer = CircularBuffer((maxLen * 1.5).toInt() + allAudio.size * 2)
    }

    fun broadcastMfcc(point: Pair<Double, FloatArray>) {
        circularBuffer.add(point)
//        Log.d("AAA", "${circularBuffer.size}")
        if (counter < Globals.SKIPPED_FRAMES) {
            counter += 1
            return
        }
        counter = 0

        referenceComparators[allAudio[muxer].name]?.onNewCoefficients(circularBuffer)
        muxer = (muxer + 1) % allAudio.size
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