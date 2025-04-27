package app.danielding.voiceactivation

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import app.danielding.voiceactivation.ui.PlaybackActivity
import app.danielding.voiceactivation.ui.RecordActivity

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        val switchButton: Button = findViewById(R.id.button)
        switchButton.setOnClickListener {
            val intent = Intent(this, AudioProcessing::class.java)
            startActivity(intent)
        }

        val recordLinkButton: Button = findViewById(R.id.recordLinkButton)
        recordLinkButton.setOnClickListener {
            startActivity(Intent(this, RecordActivity::class.java))
        }

        val playbackLinkButton: Button = findViewById(R.id.playbackLinkButton)
        playbackLinkButton.setOnClickListener {
            startActivity(Intent(this, PlaybackActivity::class.java))
        }

        // Request perms
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {  // Android 14+
            // Check for the new permissions
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VIDEO)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_MEDIA_VIDEO), 1)
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 1)
            }
        }
    }

}