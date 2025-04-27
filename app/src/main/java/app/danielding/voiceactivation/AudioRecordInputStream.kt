package app.danielding.voiceactivation

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import java.io.IOException
import java.io.InputStream

class AudioRecordInputStream(private val audioRecord: AudioRecord) : InputStream() {

    private val bufferSize = 2048  // Size of the buffer
    private val buffer = ByteArray(bufferSize)
    private var isRecording = false

    init {
        // Start recording when the stream is created
        audioRecord.startRecording()
        isRecording = true
    }

    override fun read(): Int {
        // Since we're working with audio, we'll return one byte at a time
        val bytesRead = audioRecord.read(buffer, 0, buffer.size)
        return if (bytesRead > 0) {
            buffer[0].toInt() and 0xFF // Return the first byte in the buffer
        } else {
            -1 // End of stream
        }
    }

    override fun read(b: ByteArray, off: Int, len: Int): Int {
        // Read audio data into the provided byte array
        val bytesRead = audioRecord.read(b, off, len)
        return if (bytesRead > 0) {
            bytesRead // Return the number of bytes read
        } else {
            -1 // End of stream or error
        }
    }

    override fun close() {
        super.close()
        // Stop the AudioRecord when the InputStream is closed
        audioRecord.stop()
        audioRecord.release()
        isRecording = false
    }

    fun isRecording(): Boolean {
        return isRecording
    }
}

