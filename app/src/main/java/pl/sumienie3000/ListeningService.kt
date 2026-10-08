package pl.sumienie3000

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.speech.tts.TextToSpeech
import androidx.core.app.NotificationCompat
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.RecognitionListener
import org.vosk.android.SpeechService
import org.vosk.android.StorageService
import java.util.Locale

class ListeningService : Service(), RecognitionListener {

    private var model: Model? = null
    private var recognizer: Recognizer? = null
    private var speechService: SpeechService? = null
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
            createNotification("🎙️ Uruchamiam SUMIENIE 3000...")
        )

        textToSpeech = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech?.language = Locale("pl", "PL")
            }
        }

        loadModel()
    }

    private fun loadModel() {
        updateNotification("📦 Ładuję polski model Vosk...")

        StorageService.unpack(
            this,
            "vosk-model-small-pl-0.22",
            "model",
            { loadedModel ->
                model = loadedModel

                updateNotification(
                    "🟢 Model gotowy — SUMIENIE słucha"
                )

                startVosk()
            },
            {
                updateNotification(
                    "❌ Nie udało się załadować modelu"
                )

                speak(
                    "Nie mogę załadować mojego mózgu. To źle wróży."
                )
            }
        )
    }

    private fun startVosk() {
        try {
            recognizer = Recognizer(
                model,
                16000.0f
            )

            speechService = SpeechService(
                recognizer,
                16000.0f
            )

            speechService?.startListening(this)

            updateNotification(
                "🎙️ Słucham lokalnie — bez chmury"
            )
        } catch (e: Exception) {
            updateNotification(
                "❌ Błąd Vosk: ${e.javaClass.simpleName}"
            )
        }
    }

    private fun extractText(hypothesis: String): String {
        return try {
            val json = JSONObject(hypothesis)

            if (json.has("text")) {
                json.optString("text", "")
            } else if (json.has("partial")) {
                json.optString("partial", "")
            } else {
                ""
            }
        } catch (e: Exception) {
            ""
        }
    }

    private fun checkForPolitics(text: String) {
        val normalized = text.lowercase(Locale("pl", "PL"))

        if (normalized.isBlank()) {
            return
        }

        updateNotification("👂 $normalized")

        val detected = politicalWords.any { word ->
            normalized.contains(word)
        }

        if (detected) {
            val now = System.currentTimeMillis()

            if (now - lastJokeTime >= cooldown) {
                lastJokeTime = now

                updateNotification(
                    "🚨 POLITYKA WYKRYTA — SUMIENIE INTERWENIUJE"
                )

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

    private fun createNotification(text: String): Notification {
        return NotificationCompat.Builder(
            this,
            "sumienie_channel"
        )
            .setContentTitle("SUMIENIE 3000")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(text: String) {
        val manager =
            getSystemService(NotificationManager::class.java)

        manager.notify(
            1,
            createNotification(text)
        )
    }

    override fun onPartialResult(hypothesis: String?) {
        if (!hypothesis.isNullOrBlank()) {
            val text = extractText(hypothesis)

            if (text.isNotBlank()) {
                updateNotification("👂 $text")
            }
        }
    }

    override fun onResult(hypothesis: String?) {
        if (!hypothesis.isNullOrBlank()) {
            val text = extractText(hypothesis)

            if (text.isNotBlank()) {
                checkForPolitics(text)
            }
        }
    }

    override fun onFinalResult(hypothesis: String?) {
        if (!hypothesis.isNullOrBlank()) {
            val text = extractText(hypothesis)

            if (text.isNotBlank()) {
                checkForPolitics(text)
            }
        }
    }

    override fun onError(exception: Exception?) {
        updateNotification(
            "⚠️ Błąd Vosk — próbuję ponownie"
        )
    }

    override fun onTimeout() {
        updateNotification(
            "⏳ Ponawiam nasłuchiwanie..."
        )

        speechService?.startListening(this)
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        speechService?.stop()
        speechService?.shutdown()
        speechService = null

        recognizer?.close()
        recognizer = null

        model?.close()
        model = null

        textToSpeech?.stop()
        textToSpeech?.shutdown()
        textToSpeech = null

        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
