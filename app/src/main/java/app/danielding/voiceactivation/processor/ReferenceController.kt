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
    private var referenceComparators : Array<ReferenceComparison?>
    init {
        val allAudio = AudioStorage.getAll(context)
        referenceComparators = Array<ReferenceComparison?>(allAudio.size) { null }
        for (i in 0 until allAudio.size) {
            referenceComparators[i] =
                ReferenceComparison(context, allAudio[i].name, onSimilarity)
        }
    }

    private fun broadcastMfcc(point: Pair<Double, FloatArray>) {
        for (rc in referenceComparators) {
            rc?.onNewCoefficients(point)
        }
    }
}