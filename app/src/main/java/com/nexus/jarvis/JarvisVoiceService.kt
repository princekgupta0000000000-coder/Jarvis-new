package com.nexus.jarvis

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import androidx.core.app.NotificationCompat
import java.util.Locale

/** Foreground voice loop. Android/OEM battery policies can still restrict background microphone use. */
class JarvisVoiceService : Service() {
    private var recognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null
    private val handler = Handler(Looper.getMainLooper())
    private var armedUntil = 0L
    private var restarting = false

    override fun onCreate() {
        super.onCreate()
        createChannel()
        startForeground(NOTIFICATION_ID, notification())
        tts = TextToSpeech(this) { if (it == TextToSpeech.SUCCESS) tts?.language = Locale.US }
        if (hasMic()) startListeningSoon(300)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (hasMic()) startListeningSoon(200)
        return START_STICKY
    }

    private fun hasMic() = checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

    private fun startListeningSoon(delay: Long) {
        handler.removeCallbacksAndMessages(null)
        handler.postDelayed({ startListening() }, delay)
    }

    private fun startListening() {
        if (restarting || !hasMic() || !SpeechRecognizer.isRecognitionAvailable(this)) return
        recognizer?.destroy()
        recognizer = SpeechRecognizer.createSpeechRecognizer(this)
        recognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) = Unit
            override fun onBeginningOfSpeech() = Unit
            override fun onRmsChanged(rmsdB: Float) = Unit
            override fun onBufferReceived(buffer: ByteArray?) = Unit
            override fun onEndOfSpeech() = Unit
            override fun onPartialResults(partialResults: Bundle?) = Unit
            override fun onEvent(eventType: Int, params: Bundle?) = Unit
            override fun onError(error: Int) { restartListening() }
            override fun onResults(results: Bundle?) {
                handleSpeech(results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty())
            }
        })
        recognizer?.startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-IN")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "en-IN")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        })
    }

    private fun handleSpeech(text: String) {
        val clean = text.trim()
        if (clean.isBlank()) { restartListening(); return }
        val lower = clean.lowercase(Locale.US)
        val wakeIndex = lower.indexOf("hello jarvis")
        val isArmed = System.currentTimeMillis() < armedUntil
        if (wakeIndex >= 0) {
            armedUntil = System.currentTimeMillis() + LISTEN_WINDOW_MS
            val command = clean.substring(wakeIndex + 12).trim()
            if (command.isBlank()) speakAndRestart("Yes, I am listening.") else runCommand(command)
        } else if (isArmed) {
            armedUntil = System.currentTimeMillis() + LISTEN_WINDOW_MS
            runCommand(clean)
        } else restartListening()
    }

    private fun runCommand(command: String) {
        val response = runCatching { JarvisCommandRouter.execute(this, command) }.getOrDefault("I could not complete that action.")
        speakAndRestart(response)
    }

    private fun speakAndRestart(text: String) {
        recognizer?.cancel()
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "jarvis_service")
        handler.postDelayed({ startListening() }, 1500L)
    }

    private fun restartListening() {
        if (restarting) return
        restarting = true
        handler.postDelayed({ restarting = false; startListening() }, 500L)
    }

    private fun createChannel() {
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL, "JARVIS Assistant", NotificationManager.IMPORTANCE_LOW)
        )
    }

    private fun notification(): Notification = NotificationCompat.Builder(this, CHANNEL)
        .setSmallIcon(android.R.drawable.ic_btn_speak_now)
        .setContentTitle("JARVIS • Wake listening ON")
        .setContentText("Say “Hello JARVIS” to wake the assistant")
        .setOngoing(true)
        .setCategory(NotificationCompat.CATEGORY_SERVICE)
        .build()

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        recognizer?.destroy()
        tts?.stop(); tts?.shutdown()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val CHANNEL = "jarvis_assistant"
        const val NOTIFICATION_ID = 1001
        private const val LISTEN_WINDOW_MS = 8_000L
    }
}
