package app.danielding.voiceactivation

import android.content.Context
import android.media.AudioRecord
import android.util.Log
import java.io.ByteArrayOutputStream

class AudioRecorder(
    private val context: Context,
    private val audioRecord: AudioRecord
) {
    private var recordingThread: Thread? = null
    private var isRecording = false
    var outputFilename = "none"
    private val dataBuffer = ByteArray(Globals.BUFFER_SIZE)
    private val audioDataBuffer = ByteArrayOutputStream()

    fun startRecording(filename: String) {
        outputFilename = filename
        if (audioRecord.state != AudioRecord.STATE_INITIALIZED) {
            Log.d("FAILED", "failed")
            return
        }
        isRecording = true
        audioRecord.startRecording()
        Thread {
            try {
                while (isRecording) {
                    val bytesRead = audioRecord.read(dataBuffer, 0, dataBuffer.size)
                    if (bytesRead > 0) {
                        if (dataBuffer.slice(0 until bytesRead).all{it==0.toByte()}) {
                            Log.d("recorder", "read error")
                        } else {
                            audioDataBuffer.write(dataBuffer, 0, bytesRead)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()  // Handle any errors that might occur
            }
        }.start()
    }
    fun killRecording() {
        if (isRecording) {
            audioRecord.stop()
            recordingThread?.join()
            audioDataBuffer.reset()
        }
        isRecording = false
    }

    fun stopRecording() {
        isRecording = false
        audioRecord.stop()
        recordingThread?.join()
        AudioStorage.writeDataToFile(context, outputFilename, audioDataBuffer.toByteArray())
        audioDataBuffer.reset()
    }
}
