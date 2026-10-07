package com.startuga.turnotrack.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.startuga.turnotrack.data.ThemeMode

private val Blue600 = Color(0xFF2563EB)

private val LightColors = lightColorScheme(
    primary = Blue600,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDBEAFE),
    onPrimaryContainer = Color(0xFF1E3A8A),
    secondary = Color(0xFF475569),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE2E8F0),
    onSecondaryContainer = Color(0xFF0F172A),
    tertiary = Color(0xFF7E22CE),
    background = Color(0xFFF8FAFC),
    onBackground = Color(0xFF0F172A),
    surface = Color(0xFFF8FAFC),
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFE2E8F0),
    onSurfaceVariant = Color(0xFF475569),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color.White,
    surfaceContainer = Color(0xFFF1F5F9),
    surfaceContainerHigh = Color(0xFFEAEFF5),
    surfaceContainerHighest = Color(0xFFE2E8F0),
    outline = Color(0xFF94A3B8),
    outlineVariant = Color(0xFFE2E8F0),
    error = Color(0xFFDC2626),
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF7F1D1D),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF60A5FA),
    onPrimary = Color(0xFF0B1A33),
    primaryContainer = Color(0xFF1E3A8A),
    onPrimaryContainer = Color(0xFFDBEAFE),
    secondary = Color(0xFF94A3B8),
    onSecondary = Color(0xFF0F172A),
    secondaryContainer = Color(0xFF334155),
    onSecondaryContainer = Color(0xFFE2E8F0),
    tertiary = Color(0xFFC084FC),
    background = Color(0xFF111827),
    onBackground = Color(0xFFF3F4F6),
    surface = Color(0xFF111827),
    onSurface = Color(0xFFF3F4F6),
    surfaceVariant = Color(0xFF374151),
    onSurfaceVariant = Color(0xFF9CA3AF),
    surfaceContainerLowest = Color(0xFF0B1220),
    surfaceContainerLow = Color(0xFF1F2937),
    surfaceContainer = Color(0xFF1F2937),
    surfaceContainerHigh = Color(0xFF283445),
    surfaceContainerHighest = Color(0xFF323F52),
    outline = Color(0xFF6B7280),
    outlineVariant = Color(0xFF374151),
    error = Color(0xFFF87171),
    errorContainer = Color(0xFF7F1D1D),
    onErrorContainer = Color(0xFFFEE2E2),
)

/** Preto puro para ecrãs OLED (como o tema "oled" da web app). */
private val OledColors = DarkColors.copy(
    background = Color.Black,
    surface = Color.Black,
    surfaceContainerLowest = Color.Black,
    surfaceContainerLow = Color(0xFF0A0A0A),
    surfaceContainer = Color(0xFF0F0F0F),
    surfaceContainerHigh = Color(0xFF171717),
    surfaceContainerHighest = Color(0xFF212121),
    surfaceVariant = Color(0xFF1F1F1F),
    outlineVariant = Color(0xFF262626),
)

/** Cores por tipo de registo — as mesmas da web app (azul, rosa, verde, vermelho, amarelo…). */
data class ShiftColors(
    val regular: Pair<Color, Color>,
    val holiday: Pair<Color, Color>,
    val dayOff: Pair<Color, Color>,
    val absent: Pair<Color, Color>,
    val vacation: Pair<Color, Color>,
    val late: Pair<Color, Color>,
    val overtime: Pair<Color, Color>,
    val barWorked: Color = Color(0xFF3B82F6),
    val barHoliday: Color = Color(0xFFEC4899),
    val barOvertime: Color = Color(0xFFA855F7),
)
// Pair = (fundo, texto)

private val LightShiftColors = ShiftColors(
    regular = Color(0xFFDBEAFE) to Color(0xFF1D4ED8),
    holiday = Color(0xFFFCE7F3) to Color(0xFFBE185D),
    dayOff = Color(0xFFDCFCE7) to Color(0xFF15803D),
    absent = Color(0xFFFEE2E2) to Color(0xFFB91C1C),
    vacation = Color(0xFFFEF3C7) to Color(0xFFB45309),
    late = Color(0xFFFFEDD5) to Color(0xFFC2410C),
    overtime = Color(0xFFF3E8FF) to Color(0xFF7E22CE),
)

private val DarkShiftColors = ShiftColors(
    regular = Color(0xFF1E3A8A) to Color(0xFFBFDBFE),
    holiday = Color(0xFF831843) to Color(0xFFFBCFE8),
    dayOff = Color(0xFF14532D) to Color(0xFFBBF7D0),
    absent = Color(0xFF7F1D1D) to Color(0xFFFECACA),
    vacation = Color(0xFF713F12) to Color(0xFFFDE68A),
    late = Color(0xFF7C2D12) to Color(0xFFFED7AA),
    overtime = Color(0xFF581C87) to Color(0xFFE9D5FF),
)

val LocalShiftColors = staticCompositionLocalOf { LightShiftColors }

@Composable
fun TurnoTrackTheme(mode: ThemeMode, content: @Composable () -> Unit) {
    val systemDark = isSystemInDarkTheme()
    val dark = when (mode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK, ThemeMode.OLED -> true
    }
    val colors: ColorScheme = when {
        mode == ThemeMode.OLED -> OledColors
        dark -> DarkColors
        else -> LightColors
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            window.decorView.setBackgroundColor(colors.background.toArgb())
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !dark
                isAppearanceLightNavigationBars = !dark
            }
        }
    }

    CompositionLocalProvider(LocalShiftColors provides if (dark) DarkShiftColors else LightShiftColors) {
        MaterialTheme(colorScheme = colors, content = content)
    }
}
