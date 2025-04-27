package app.danielding.voiceactivation

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.util.Log
import be.tarsos.dsp.io.TarsosDSPAudioFormat
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import kotlin.math.min

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

    private fun playAudio(data: ByteArray) {
        var audioTrack = AudioTrack.Builder()
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(Globals.AUDIO_ENCODING)
                    .setSampleRate(Globals.SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(Globals.BUFFER_SIZE)
            .build()

        audioTrack.play()
        Thread {
            readFileToAudioTrack(audioTrack, data)
        }.start()
    }
    // Function to stop audio playback
    private fun stopAudio(audioTrack: AudioTrack) {
        audioTrack.stop()
        audioTrack.release()
    }

    private fun readFileToAudioTrack(audioTrack: AudioTrack, data: ByteArray) {
        var offset = 0
        while (offset < data.size) {
            val written = audioTrack.write(data, offset, min(Globals.BUFFER_SIZE, data.size - offset))
            offset += written
            Log.d("button res", "$written")
        }
        stopAudio(audioTrack)
    }
}
