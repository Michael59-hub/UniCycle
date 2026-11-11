package ng.unicycle

import androidx.compose.runtime.Composable
import platform.AVFoundation.AVSpeechBoundary
import platform.AVFoundation.AVSpeechSynthesizer
import platform.AVFoundation.AVSpeechUtterance
import platform.AVFoundation.AVSpeechUtteranceDefaultSpeechRate

private class IosSpeaker : Speaker {
    private val synth = AVSpeechSynthesizer()

    override fun speak(text: String, rate: Float) {
        synth.stopSpeakingAtBoundary(AVSpeechBoundary.AVSpeechBoundaryImmediate)
        val utterance = AVSpeechUtterance(string = text)
        utterance.rate = AVSpeechUtteranceDefaultSpeechRate * rate
        synth.speakUtterance(utterance)
    }

    override fun stop() {
        synth.stopSpeakingAtBoundary(AVSpeechBoundary.AVSpeechBoundaryImmediate)
    }
}

actual fun createSpeaker(): Speaker = IosSpeaker()

@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) { /* iOS uses on-screen back */ }

@Composable
actual fun SystemBarsEffect(darkTheme: Boolean) { /* TODO: set status bar style from the iOS host */ }
