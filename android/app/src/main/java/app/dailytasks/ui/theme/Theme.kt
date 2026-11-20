package app.dailytasks.ui.theme

import android.os.Build
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// Tokens from the mockups (Tokens artboard), same as web/src/styles.css.

@Immutable
data class AppColors(
    val bg: Color,
    val surface: Color,
    val surface2: Color,
    val text: Color,
    val text2: Color,
    val outline: Color,
    val divider: Color,
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val ringTrack: Color,
    val done: Color,
    val onDone: Color,
    val streak: Color,
    val amberTint: Color,
    val missed: Color,
    val dark: Boolean,
)

val LightColors = AppColors(
    bg = Color(0xFFF7FAF9), surface = Color(0xFFFFFFFF), surface2 = Color(0xFFEEF4F2),
    text = Color(0xFF0F172A), text2 = Color(0xFF475569), outline = Color(0xFF94A3B8), divider = Color(0xFFE2E8F0),
    primary = Color(0xFF0F766E), onPrimary = Color.White, primaryContainer = Color(0xFFCCFBF1), onPrimaryContainer = Color(0xFF134E4A),
    ringTrack = Color(0xFF99F6E4), done = Color(0xFF047857), onDone = Color.White,
    streak = Color(0xFFB45309), amberTint = Color(0xFFFEF3C7), missed = Color(0xFFB91C1C), dark = false,
)

val DarkColors = AppColors(
    bg = Color(0xFF0E1514), surface = Color(0xFF1A2321), surface2 = Color(0xFF151D1C),
    text = Color(0xFFE2E8F0), text2 = Color(0xFFA3B1C2), outline = Color(0xFF64748B), divider = Color(0xFF26302E),
    primary = Color(0xFF5EEAD4), onPrimary = Color(0xFF00201D), primaryContainer = Color(0xFF134E4A), onPrimaryContainer = Color(0xFFCCFBF1),
    ringTrack = Color(0xFF1F5F59), done = Color(0xFF6EE7B7), onDone = Color(0xFF022C22),
    streak = Color(0xFFFCD34D), amberTint = Color(0xFF3A2A10), missed = Color(0xFFFCA5A5), dark = true,
)

val LocalAppColors = staticCompositionLocalOf { LightColors }

/** Task colors: (tint, foreground) per theme. Same keys as web/src/ui/palette.ts. */
val TaskColorKeys = listOf("teal", "green", "blue", "purple", "amber", "rose")

private val taskLight = mapOf(
    "teal" to (0xFFCCFBF1 to 0xFF0F766E), "green" to (0xFFDCFCE7 to 0xFF047857), "blue" to (0xFFDBEAFE to 0xFF1D4ED8),
    "purple" to (0xFFEDE9FE to 0xFF6D28D9), "amber" to (0xFFFEF3C7 to 0xFFB45309), "rose" to (0xFFFFE4E6 to 0xFFBE123C),
)
private val taskDark = mapOf(
    "teal" to (0xFF134E4A to 0xFF5EEAD4), "green" to (0xFF123224 to 0xFF6EE7B7), "blue" to (0xFF1E2A44 to 0xFF93C5FD),
    "purple" to (0xFF2A2145 to 0xFFC4B5FD), "amber" to (0xFF3A2A10 to 0xFFFCD34D), "rose" to (0xFF3F1620 to 0xFFFDA4AF),
)

fun taskColors(key: String, dark: Boolean): Pair<Color, Color> {
    val (t, f) = (if (dark) taskDark else taskLight)[key] ?: (if (dark) taskDark else taskLight).getValue("teal")
    return Color(t) to Color(f)
}

fun taskSwatch(key: String): Color = Color(taskLight[key]?.second ?: 0xFF0F766E)

@Composable
fun DailyTasksTheme(dark: Boolean, dynamicColor: Boolean, content: @Composable () -> Unit) {
    var colors = if (dark) DarkColors else LightColors
    var scheme: ColorScheme = if (dark) {
        darkColorScheme(
            primary = colors.primary, onPrimary = colors.onPrimary, primaryContainer = colors.primaryContainer,
            onPrimaryContainer = colors.onPrimaryContainer, background = colors.bg, surface = colors.surface,
            onBackground = colors.text, onSurface = colors.text, onSurfaceVariant = colors.text2, outline = colors.outline,
            surfaceContainer = colors.surface2, outlineVariant = colors.divider, error = colors.missed,
        )
    } else {
        lightColorScheme(
            primary = colors.primary, onPrimary = colors.onPrimary, primaryContainer = colors.primaryContainer,
            onPrimaryContainer = colors.onPrimaryContainer, background = colors.bg, surface = colors.surface,
            onBackground = colors.text, onSurface = colors.text, onSurfaceVariant = colors.text2, outline = colors.outline,
            surfaceContainer = colors.surface2, outlineVariant = colors.divider, error = colors.missed,
        )
    }
    // Android 12+ wallpaper colors, if turned on in Settings. Done, streak
    // and missed keep their fixed colors because they carry meaning.
    if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val ctx = LocalContext.current
        scheme = if (dark) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        colors = colors.copy(
            primary = scheme.primary, onPrimary = scheme.onPrimary,
            primaryContainer = scheme.primaryContainer, onPrimaryContainer = scheme.onPrimaryContainer,
            bg = scheme.background, surface = scheme.surfaceContainerLowest, surface2 = scheme.surfaceContainer,
            ringTrack = scheme.primary.copy(alpha = 0.3f),
        )
    }
    CompositionLocalProvider(LocalAppColors provides colors) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}
