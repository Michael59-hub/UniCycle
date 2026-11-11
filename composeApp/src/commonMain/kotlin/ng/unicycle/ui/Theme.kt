package ng.unicycle.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** 8-point spacing scale (4, 8, 12, 16, 24, 32, 40). */
object Space {
    val Xs = 4.dp
    val Sm = 8.dp
    val Md = 12.dp
    val Lg = 16.dp
    val Xl = 24.dp
    val Xxl = 32.dp
    val Xxxl = 40.dp
    val Page = 16.dp
}

data class Palette(
    val bg: Color, val surface: Color, val elevated: Color,
    val primary: Color, val onPrimary: Color,
    val selected: Color, val onSelected: Color,
    val success: Color, val successContainer: Color,
    val amber: Color, val amberContainer: Color,
    val danger: Color, val dangerContainer: Color,
    val text: Color, val muted: Color, val border: Color,
    val isDark: Boolean,
)

val MidnightPalette = Palette(
    bg = Color(0xFF11101C), surface = Color(0xFF1C1A2B), elevated = Color(0xFF252239),
    primary = Color(0xFF9675FF), onPrimary = Color(0xFF11101C),
    selected = Color(0xFF6250C9), onSelected = Color(0xFFFFFFFF),
    success = Color(0xFF36D6B0), successContainer = Color(0xFF123D34),
    amber = Color(0xFFFFBF69), amberContainer = Color(0xFF4A3A1E),
    danger = Color(0xFFFF6978), dangerContainer = Color(0xFF4A1F28),
    text = Color(0xFFF1EFF8), muted = Color(0xFFA5A1B7), border = Color(0xFF37334F),
    isDark = true,
)

val LightPalette = Palette(
    bg = Color(0xFFF7F6FB), surface = Color(0xFFFFFFFF), elevated = Color(0xFFF0EDF8),
    primary = Color(0xFF7054D8), onPrimary = Color(0xFFFFFFFF),
    selected = Color(0xFFE4DDFB), onSelected = Color(0xFF2A1F66),
    success = Color(0xFF16805F), successContainer = Color(0xFFD7F3EA),
    amber = Color(0xFF9A5B00), amberContainer = Color(0xFFFFEFD2),
    danger = Color(0xFFC62F42), dangerContainer = Color(0xFFFFE0E3),
    text = Color(0xFF211D31), muted = Color(0xFF686276), border = Color(0xFFD9D4E8),
    isDark = false,
)

/** Maximum-contrast variants for the "High contrast" accessibility setting. */
fun Palette.highContrast(): Palette = if (isDark) copy(
    text = Color.White, muted = Color(0xFFE6E3F2), border = Color(0xFFC9C2E8),
    primary = Color(0xFFBBA6FF), selected = Color(0xFF4A3AB0),
) else copy(
    text = Color.Black, muted = Color(0xFF2B2640), border = Color(0xFF4A4560),
    primary = Color(0xFF4A31B8), selected = Color(0xFFCFC3FA),
)

val LocalPalette = staticCompositionLocalOf { MidnightPalette }
val LocalReduceMotion = staticCompositionLocalOf { false }

/** Semantic colour tokens. Read inside composables, or hoist into a val first. */
object UC {
    val Bg @Composable get() = LocalPalette.current.bg
    val Surface @Composable get() = LocalPalette.current.surface
    val Elevated @Composable get() = LocalPalette.current.elevated
    val Primary @Composable get() = LocalPalette.current.primary
    val OnPrimary @Composable get() = LocalPalette.current.onPrimary
    val Selected @Composable get() = LocalPalette.current.selected
    val OnSelected @Composable get() = LocalPalette.current.onSelected
    val Success @Composable get() = LocalPalette.current.success
    val SuccessContainer @Composable get() = LocalPalette.current.successContainer
    val Amber @Composable get() = LocalPalette.current.amber
    val AmberContainer @Composable get() = LocalPalette.current.amberContainer
    val Danger @Composable get() = LocalPalette.current.danger
    val DangerContainer @Composable get() = LocalPalette.current.dangerContainer
    val Text @Composable get() = LocalPalette.current.text
    val Muted @Composable get() = LocalPalette.current.muted
    val Border @Composable get() = LocalPalette.current.border
}

/**
 * Typography. The spec asks for Sora (display) and Inter (body). Those font files are not bundled
 * yet, so both fall back to the platform sans-serif. To enable them, add the .ttf files via
 * Compose Multiplatform resources and replace the two families below (see README).
 */
val DisplayFont: FontFamily = FontFamily.SansSerif
val BodyFont: FontFamily = FontFamily.SansSerif

object UcType {
    val Display = TextStyle(fontFamily = DisplayFont, fontSize = 30.sp, lineHeight = 36.sp, fontWeight = FontWeight.Bold)
    val H1 = TextStyle(fontFamily = DisplayFont, fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold)
    val H2 = TextStyle(fontFamily = DisplayFont, fontSize = 17.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold)
    val Body = TextStyle(fontFamily = BodyFont, fontSize = 15.sp, lineHeight = 22.sp)
    val BodyStrong = Body.copy(fontWeight = FontWeight.SemiBold)
    val BodySmall = TextStyle(fontFamily = BodyFont, fontSize = 13.sp, lineHeight = 18.sp)
    val Caption = TextStyle(fontFamily = BodyFont, fontSize = 12.sp, lineHeight = 16.sp)
    val Numeric = TextStyle(fontFamily = DisplayFont, fontSize = 24.sp, lineHeight = 30.sp, fontWeight = FontWeight.Bold, fontFeatureSettings = "tnum")
}

@Composable
fun UniCycleTheme(
    darkTheme: Boolean,
    highContrast: Boolean,
    textScale: Float,
    reduceMotion: Boolean,
    content: @Composable () -> Unit,
) {
    val base = if (darkTheme) MidnightPalette else LightPalette
    val p = if (highContrast) base.highContrast() else base
    val scheme = if (darkTheme) {
        darkColorScheme(
            primary = p.primary, onPrimary = p.onPrimary, background = p.bg, onBackground = p.text,
            surface = p.surface, onSurface = p.text, surfaceVariant = p.elevated, onSurfaceVariant = p.muted,
            outline = p.border, error = p.danger,
        )
    } else {
        lightColorScheme(
            primary = p.primary, onPrimary = p.onPrimary, background = p.bg, onBackground = p.text,
            surface = p.surface, onSurface = p.text, surfaceVariant = p.elevated, onSurfaceVariant = p.muted,
            outline = p.border, error = p.danger,
        )
    }
    val density = LocalDensity.current
    CompositionLocalProvider(
        LocalPalette provides p,
        LocalReduceMotion provides reduceMotion,
        // Respect the system font scale, then apply the in-app text size on top of it.
        LocalDensity provides Density(density.density, density.fontScale * textScale),
    ) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}
