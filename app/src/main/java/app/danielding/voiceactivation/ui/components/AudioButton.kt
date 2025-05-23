package app.danielding.voiceactivation.ui.components

import android.content.Context
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import androidx.appcompat.widget.AppCompatButton
import app.danielding.voiceactivation.AudioStorage
import app.danielding.voiceactivation.Globals
import java.io.IOException

// Custom Button class
class AudioButton(
    context: Context,
    filename: String
) : AppCompatButton(context) {
    private var rawAudio = ByteArray(0)

    init {
        rawAudio = loadFileToByteArray(context, filename)
//        audioTrack.write(rawAudio, 0, rawAudio.size)
        text = "Play $filename"
        setOnClickListener {
            playAudio(filename)
        }
    }

    private fun playAudio(filename: String) {
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

        // Start playback
        Log.d("play button", "${audioTrack.state == AudioTrack.STATE_INITIALIZED}")
        // Write the PCM data to the AudioTrack for playback


        // Optionally, stop and release AudioTrack after playback
//        audioTrack.setPlaybackPositionUpdateListener(object : AudioTrack.OnPlaybackPositionUpdateListener {
//            override fun onMarkerReached(track: AudioTrack?) {
//                // Called when playback is finished
//                stopAudio()
//            }
//
//            override fun onPeriodicNotification(track: AudioTrack?) {
//                // You can use this for periodic updates if needed
//            }
//        })
        audioTrack.play()
        Thread {
            readFileToAudioTrack(context, audioTrack, filename)
        }.start()

    }

    // Function to stop audio playback
    private fun stopAudio(audioTrack: AudioTrack) {
        audioTrack.stop()
        audioTrack.release()
    }

    private fun readFileToAudioTrack(context: Context, audioTrack: AudioTrack, filename: String) {
        val audioBytes = AudioStorage.getFile(context, filename)?.readBytes() ?: ByteArray(0)
        var offset = 0
        while (offset < audioBytes.size) {
            val written = audioTrack.write(audioBytes, offset, Globals.BUFFER_SIZE)
            offset += written
            Log.d("button res", "$written")
        }
        stopAudio(audioTrack)
    }
    private fun loadFileToByteArray(context: Context, fileName: String): ByteArray {
        return try {
            // Open the file and read it into a ByteArray
            val file = AudioStorage.getFileObj(context, fileName)
            val fileInputStream = AudioStorage.getFile(context, fileName)
            val byteArray = ByteArray(file?.length()?.toInt() ?: 0)
            fileInputStream?.read(byteArray)
            fileInputStream?.close() // Close the stream after reading
            byteArray
        } catch (e: IOException) {
            e.printStackTrace()
            ByteArray(0)
        }
    }
}