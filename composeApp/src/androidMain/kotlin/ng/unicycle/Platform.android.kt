package ng.unicycle

import android.app.Activity
import android.speech.tts.TextToSpeech
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private class AndroidSpeaker : Speaker {
    private var ready = false
    private var tts: TextToSpeech? = null

    init {
        tts = TextToSpeech(AndroidContext.app) { status -> ready = status == TextToSpeech.SUCCESS }
    }

    override fun speak(text: String, rate: Float) {
        val engine = tts ?: return
        if (!ready) return
        engine.setSpeechRate(rate)
        engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "unicycle")
    }

    override fun stop() { tts?.stop() }
}

actual fun createSpeaker(): Speaker = AndroidSpeaker()

@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) {
    BackHandler(enabled = enabled, onBack = onBack)
}

@Composable
actual fun SystemBarsEffect(darkTheme: Boolean) {
    val view = LocalView.current
    SideEffect {
        val window = (view.context as? Activity)?.window ?: return@SideEffect
        val controller = WindowCompat.getInsetsController(window, view)
        controller.isAppearanceLightStatusBars = !darkTheme
        controller.isAppearanceLightNavigationBars = !darkTheme
    }
}
