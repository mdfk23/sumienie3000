package pl.sumienie3000

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import androidx.core.app.NotificationCompat
import java.util.Locale

class ListeningService : Service(), RecognitionListener {

    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null

    private var lastJokeTime = 0L
    private val cooldown = 30_000L

    private val politicalWords = listOf(
        "polityka",
        "politycy",
        "rząd",
        "premier",
        "prezydent",
        "sejm",
        "senat",
        "minister",
        "partia",
        "wybory",
        "tusk",
        "kaczyński",
        "pis",
        "po",
        "konfederacja",
        "lewica",
        "psl",
        "hołownia"
    )

    private val jokes = listOf(
        "Spokojnie. To tylko rozmowa ze znajomymi, nie komisja śledcza.",
        "Oho, zaczynamy naprawiać państwo. Kto ma kawę?",
        "STOP. Mieliśmy się spotkać towarzysko, a nie tworzyć program wyborczy.",
        "Widzę, że demokracja ponownie zaatakowała nasze spotkanie.",
        "Jeszcze chwila i ktoś wyciągnie sondaże. Ratujmy tę rozmowę.",
        "Nie eskalujmy. Ostatnim razem skończyło się na czterdziestu siedmiu minutach i nikt nie zmienił zdania."
    )

    override fun onCreate() {
        super.onCreate()

        createNotificationChannel()

        startForeground(
            1,
            createNotification()
        )

        textToSpeech = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech?.language = Locale("pl", "PL")
            }
        }

        startSpeechRecognition()
    }

    private fun startSpeechRecognition() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            speak("Nie mam dostępu do rozpoznawania mowy. Sumienie jest rozczarowane.")
            return
        }

        speechRecognizer?.destroy()

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechRecognizer?.setRecognitionListener(this)

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )

            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE,
                "pl-PL"
            )

            putExtra(
                RecognizerIntent.EXTRA_PARTIAL_RESULTS,
                true
            )
        }

        speechRecognizer?.startListening(intent)
    }

    private fun checkForPolitics(text: String) {
        val normalized = text.lowercase(Locale("pl", "PL"))

        if (normalized.isNotBlank()) {
            speak("Słyszę: $normalized")
        }

        val detected = politicalWords.any { word ->
            normalized.contains(word)
        }

        if (detected) {
            val now = System.currentTimeMillis()

            if (now - lastJokeTime >= cooldown) {
                lastJokeTime = now

                val joke = jokes.random()
                speak(joke)
            }
        }
    }

    private fun speak(text: String) {
        textToSpeech?.speak(
            text,
            TextToSpeech.QUEUE_FLUSH,
            null,
            "sumienie_joke"
        )
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "sumienie_channel",
                "SUMIENIE 3000",
                NotificationManager.IMPORTANCE_LOW
            )

            val manager =
                getSystemService(NotificationManager::class.java)

            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(
            this,
            "sumienie_channel"
        )
            .setContentTitle("SUMIENIE 3000")
            .setContentText("🎙️ Nasłuchuję rozmowy")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .build()
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {
        return START_STICKY
    }

    override fun onResults(results: Bundle?) {
        val matches = results?.getStringArrayList(
            SpeechRecognizer.RESULTS_RECOGNITION
        )

        matches?.firstOrNull()?.let {
            checkForPolitics(it)
        }

        startSpeechRecognition()
    }

    override fun onPartialResults(partialResults: Bundle?) {
        val matches = partialResults?.getStringArrayList(
            SpeechRecognizer.RESULTS_RECOGNITION
        )

        matches?.firstOrNull()?.let {
            checkForPolitics(it)
        }
    }

    override fun onError(error: Int) {
        startSpeechRecognition()
    }

    override fun onReadyForSpeech(params: Bundle?) {
    }

    override fun onBeginningOfSpeech() {
    }

    override fun onRmsChanged(rmsdB: Float) {
    }

    override fun onBufferReceived(buffer: ByteArray?) {
    }

    override fun onEndOfSpeech() {
    }

    override fun onEvent(
        eventType: Int,
        params: Bundle?
    ) {
    }

    override fun onDestroy() {
        speechRecognizer?.destroy()
        speechRecognizer = null

        textToSpeech?.stop()
        textToSpeech?.shutdown()
        textToSpeech = null

        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
