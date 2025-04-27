package app.danielding.voiceactivation

import be.tarsos.dsp.io.TarsosDSPAudioFormat
import be.tarsos.dsp.io.TarsosDSPAudioInputStream
import java.io.InputStream


class InputStreamWrapper(
    private val inputStream: InputStream,
    private val format: TarsosDSPAudioFormat
) : TarsosDSPAudioInputStream {
    private var isClosed = false


    override fun read(b: ByteArray?, off: Int, len: Int): Int {
        return inputStream.read(b, off, len)
    }

    override fun skip(bytesToSkip: Long): Long {
        return inputStream.skip(bytesToSkip)
    }

    override fun close() {
        if (!isClosed) {
            inputStream.close()
            isClosed = true
        }
    }

    override fun getFormat(): TarsosDSPAudioFormat {
        return format
    }

    override fun getFrameLength(): Long {
        return -1
    }
}