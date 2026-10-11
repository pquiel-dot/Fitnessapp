package de.pquiel.workout.audio

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.tts.TextToSpeech
import java.util.Locale

/** Akustische und haptische Signale: Sprachansagen, Pieptöne und Vibration. */
class Cues(context: Context) : TextToSpeech.OnInitListener {
    private val appContext = context.applicationContext
    private var ttsReady = false
    private val tts: TextToSpeech = TextToSpeech(appContext, this)

    private val tone: ToneGenerator? = try {
        ToneGenerator(AudioManager.STREAM_MUSIC, 90)
    } catch (e: RuntimeException) {
        null
    }

    private val vibrator: Vibrator? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            appContext.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            appContext.getSystemService(Vibrator::class.java)
        }

    var soundEnabled = true
    var voiceEnabled = true
    var vibrationEnabled = true

    override fun onInit(status: Int) {
        if (status != TextToSpeech.SUCCESS) return
        val result = tts.setLanguage(Locale.GERMANY)
        ttsReady = result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED
    }

    fun speak(text: String) {
        if (!voiceEnabled || !ttsReady) return
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "cue")
    }

    fun stopSpeaking() {
        if (ttsReady) tts.stop()
    }

    /** Kurzer Piep für den Countdown (3, 2, 1). */
    fun tick() {
        if (soundEnabled) tone?.startTone(ToneGenerator.TONE_PROP_BEEP, 150)
    }

    /** Signal für den Wechsel zum nächsten Abschnitt. */
    fun transition() {
        if (soundEnabled) tone?.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 500)
        vibrate(400)
    }

    fun vibrate(ms: Long) {
        if (!vibrationEnabled) return
        vibrator?.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    fun release() {
        tts.stop()
        tts.shutdown()
        tone?.release()
    }
}
