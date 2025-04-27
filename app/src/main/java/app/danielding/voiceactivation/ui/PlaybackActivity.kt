package app.danielding.voiceactivation.ui

import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Bundle
import android.widget.VideoView
import androidx.activity.ComponentActivity
import app.danielding.voiceactivation.R
import app.danielding.voiceactivation.VideoStorage

class PlaybackActivity : ComponentActivity() {
    private lateinit var videoView: VideoView
    private lateinit var fileVideoMap: Map<String, Uri>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_playback)
        fileVideoMap = VideoStorage.getData(this)

        // Lock the screen orientation to portrait mode
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        // Initialize VideoView
        videoView = findViewById(R.id.videoView)

        // Enable looping on the video
        videoView.setOnCompletionListener {
            // Automatically restart the video when it completes
            videoView.start()
        }

        // Play the first video
        playVideo(fileVideoMap["idle"])
    }

    private fun playVideo(uri: Uri?) {
        if (uri == null) {
            return
        }
        videoView.setVideoURI(uri)
        videoView.start()
    }
}