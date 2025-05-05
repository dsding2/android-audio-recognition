package app.danielding.voiceactivation

import android.app.Application
import android.media.AudioFormat
import android.media.AudioRecord
import be.tarsos.dsp.io.TarsosDSPAudioFormat

class Globals : Application() {
    companion object {
        const val SAMPLE_RATE = 44100
        val BUFFER_SIZE = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )*2
        const val AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT
        val TARSOS_AUDIO_FORMAT = TarsosDSPAudioFormat(SAMPLE_RATE.toFloat(), 16, 1, true, false)
        const val SAMPLES_PER_FRAME = 2048
        const val BUFFER_OVERLAP = SAMPLES_PER_FRAME/2
        const val MFCC_NUM_COEFFS = 13
        const val MFCC_NUM_FILTERS = 30
        const val MFCC_LOWER_CUTOFF = 40f
        const val MFCC_UPPER_CUTOFF = 8000f
        const val NUM_FEATURE_DIMENSIONS = MFCC_NUM_COEFFS * 2 + 1
        const val DEFAULT_TUNING = .71

        const val DEFAULT_MFCC_WEIGHT = 40.0
        const val DEFAULT_DELTA_WEIGHT = 30.0
        const val DEFAULT_VOLUME_WEIGHT = 60.0

    }
}
