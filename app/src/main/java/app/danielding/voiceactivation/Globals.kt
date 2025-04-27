package app.danielding.voiceactivation

import android.app.Application
import android.media.AudioFormat
import android.media.AudioRecord

class Globals : Application() {
    companion object {
        const val SAMPLE_RATE = 44100
        val BUFFER_SIZE = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )*2
        const val AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT
    }
}
