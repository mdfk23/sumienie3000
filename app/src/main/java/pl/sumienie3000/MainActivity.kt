package pl.sumienie3000

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var statusText: TextView
    private lateinit var button: Button

    private val microphoneRequestCode = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)
        button = findViewById(R.id.startButton)

     button.setOnClickListener {
    if (button.text.toString().contains("WŁĄCZ")) {
        if (isMicrophonePermissionGranted()) {
            startListening()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                microphoneRequestCode
            )
        }
    } else {
        stopListening()
    }
}

    private fun isMicrophonePermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun startListening() {
        val intent = Intent(this, ListeningService::class.java)
        ContextCompat.startForegroundService(this, intent)

        statusText.text = "🟢 SUMIENIE 3000 SŁUCHA"
        button.text = "🛑 WYŁĄCZ SUMIENIE"
    }

    private fun stopListening() {
    val intent = Intent(this, ListeningService::class.java)
    stopService(intent)

    statusText.text = "⚫ SUMIENIE ŚPI"
    button.text = "🎙️ WŁĄCZ SUMIENIE"
}
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == microphoneRequestCode &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            startListening()
        }
    }
}
