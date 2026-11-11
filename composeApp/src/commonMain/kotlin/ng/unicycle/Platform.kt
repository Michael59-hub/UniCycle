package ng.unicycle

import androidx.compose.runtime.Composable

/** Text-to-speech. Android TextToSpeech / iOS AVSpeechSynthesizer. */
interface Speaker {
    fun speak(text: String, rate: Float)
    fun stop()
}

expect fun createSpeaker(): Speaker

/** System back button (Android). No-op on iOS, which uses on-screen back controls. */
@Composable
expect fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit)

/** Keeps status/navigation bar icon colours readable against the app theme. */
@Composable
expect fun SystemBarsEffect(darkTheme: Boolean)
