package com.storyforge.ai.util

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Locale

data class VoiceState(
    val available: Boolean = false,
    val listening: Boolean = false,
    val partial: String = "",
    val error: String? = null
)

class VoiceInputController(context: Context) {
    private val appContext = context.applicationContext
    private var recognizer: SpeechRecognizer? = null
    private val _state = MutableStateFlow(VoiceState(available = SpeechRecognizer.isRecognitionAvailable(appContext)))
    val state: StateFlow<VoiceState> = _state

    fun start(language: String? = null, onFinal: (String) -> Unit) {
        if (!SpeechRecognizer.isRecognitionAvailable(appContext)) {
            _state.value = VoiceState(false, error = "Voice input isn't available on this device. Type your idea instead.")
            return
        }
        stop()
        val speech = SpeechRecognizer.createSpeechRecognizer(appContext)
        recognizer = speech
        speech.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) { _state.value = VoiceState(true, true) }
            override fun onBeginningOfSpeech() = Unit
            override fun onRmsChanged(rmsdB: Float) = Unit
            override fun onBufferReceived(buffer: ByteArray?) = Unit
            override fun onEndOfSpeech() { _state.value = _state.value.copy(listening = false) }
            override fun onError(error: Int) {
                _state.value = VoiceState(true, false, error = humanError(error))
                release()
            }
            override fun onResults(results: Bundle?) {
                val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
                _state.value = VoiceState(true, false, partial = text)
                if (text.isNotBlank()) onFinal(text)
                release()
            }
            override fun onPartialResults(partialResults: Bundle?) {
                val text = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
                _state.value = _state.value.copy(partial = text, listening = true)
            }
            override fun onEvent(eventType: Int, params: Bundle?) = Unit
        })

        val locale = languageToLocale(language) ?: Locale.getDefault()
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, locale.toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        runCatching { speech.startListening(intent) }.onFailure {
            _state.value = VoiceState(true, error = "Could not start the microphone. Check permission and try again.")
            release()
        }
    }

    fun stop() {
        runCatching { recognizer?.stopListening() }
        release()
        _state.value = _state.value.copy(listening = false)
    }

    fun clearError() { _state.value = _state.value.copy(error = null) }
    fun destroy() { stop() }

    private fun languageToLocale(language: String?): Locale? = when (language?.lowercase()) {
        "hindi" -> Locale.forLanguageTag("hi-IN")
        "marathi" -> Locale.forLanguageTag("mr-IN")
        "spanish" -> Locale.forLanguageTag("es-ES")
        "french" -> Locale.forLanguageTag("fr-FR")
        "english" -> Locale.forLanguageTag("en-IN")
        else -> null
    }

    private fun release() {
        runCatching { recognizer?.destroy() }
        recognizer = null
    }

    private fun humanError(code: Int): String = when (code) {
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission is required for voice input."
        SpeechRecognizer.ERROR_NO_MATCH -> "I didn't catch that. Try again or type the idea."
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech detected. Tap the mic and speak."
        SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Voice recognition needs a network connection on this device."
        else -> "Voice input failed. You can keep typing."
    }
}
