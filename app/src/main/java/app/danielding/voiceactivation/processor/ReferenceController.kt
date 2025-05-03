package app.danielding.voiceactivation.processor

import android.Manifest
import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.net.Uri
import android.os.Bundle
import android.os.Process
import android.util.Log
import android.widget.VideoView
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import app.danielding.voiceactivation.AudioRecordInputStream
import app.danielding.voiceactivation.AudioStorage
import app.danielding.voiceactivation.Globals
import app.danielding.voiceactivation.R
import app.danielding.voiceactivation.VideoStorage
import be.tarsos.dsp.AudioDispatcher
import be.tarsos.dsp.AudioEvent
import be.tarsos.dsp.AudioProcessor
import be.tarsos.dsp.io.UniversalAudioInputStream
import be.tarsos.dsp.mfcc.MFCC


class ReferenceController (
    context: Context,
    onSimilarity: (String)->Unit
){
    private var referenceComparators = mutableMapOf<String, ReferenceComparison>()
    init {
        val allAudio = AudioStorage.getAll(context)
        for (i in 0 until allAudio.size) {
            referenceComparators[allAudio[i].name] =
                ReferenceComparison(context, allAudio[i].name, onSimilarity)
        }
    }

    fun broadcastMfcc(point: Pair<Double, FloatArray>) {
        for (rc in referenceComparators) {
            rc.value.onNewCoefficients(point)
        }
    }

    fun getReferenceComparator(filename: String): ReferenceComparison? {
        return referenceComparators[filename]
    }

    fun renormalizeAll() {
        for (rc in referenceComparators) {
            rc.value.renormalize()
        }
    }
}