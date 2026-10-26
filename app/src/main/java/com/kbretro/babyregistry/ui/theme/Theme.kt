package com.kbretro.babyregistry.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Paleta propia: violeta suave con acentos rosa y turquesa, superficies ligeramente lavanda.
private val LightColors = lightColorScheme(
    primary = Color(0xFF6C4CD8),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEAE2FF),
    onPrimaryContainer = Color(0xFF23005C),
    secondary = Color(0xFFD9577F),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFE1EA),
    onSecondaryContainer = Color(0xFF3E0019),
    tertiary = Color(0xFF2A9D8C),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFD2F5EE),
    onTertiaryContainer = Color(0xFF00201B),
    background = Color(0xFFFBF8FF),
    onBackground = Color(0xFF1C1A22),
    surface = Color(0xFFFBF8FF),
    onSurface = Color(0xFF1C1A22),
    surfaceVariant = Color(0xFFE7E0F2),
    onSurfaceVariant = Color(0xFF5D5670),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF6F2FD),
    surfaceContainer = Color(0xFFF1ECFA),
    surfaceContainerHigh = Color(0xFFEBE6F6),
    surfaceContainerHighest = Color(0xFFE5E0F1),
    outline = Color(0xFF8E87A0),
    outlineVariant = Color(0xFFD6CFE4),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFCDBDFF),
    onPrimary = Color(0xFF36198F),
    primaryContainer = Color(0xFF4E33B5),
    onPrimaryContainer = Color(0xFFEAE2FF),
    secondary = Color(0xFFFFB1C6),
    onSecondary = Color(0xFF5F1131),
    secondaryContainer = Color(0xFF7D2947),
    onSecondaryContainer = Color(0xFFFFD9E3),
    tertiary = Color(0xFF7CDCC9),
    onTertiary = Color(0xFF00382F),
    tertiaryContainer = Color(0xFF005045),
    onTertiaryContainer = Color(0xFFA8F2E3),
    background = Color(0xFF13111A),
    onBackground = Color(0xFFE6E0EE),
    surface = Color(0xFF13111A),
    onSurface = Color(0xFFE6E0EE),
    surfaceVariant = Color(0xFF49454F),
    onSurfaceVariant = Color(0xFFCBC3DA),
    surfaceContainerLowest = Color(0xFF0E0C14),
    surfaceContainerLow = Color(0xFF1B1823),
    surfaceContainer = Color(0xFF201D28),
    surfaceContainerHigh = Color(0xFF2A2733),
    surfaceContainerHighest = Color(0xFF35313E),
    outline = Color(0xFF948DA3),
    outlineVariant = Color(0xFF49454F),
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

private val AppTypography = Typography().let { base ->
    base.copy(
        displaySmall = base.displaySmall.copy(fontWeight = FontWeight.SemiBold),
        headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.SemiBold),
        headlineSmall = base.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
        titleLarge = base.titleLarge.copy(fontWeight = FontWeight.SemiBold),
        titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        titleSmall = base.titleSmall.copy(fontWeight = FontWeight.SemiBold),
        labelLarge = base.labelLarge.copy(fontWeight = FontWeight.SemiBold),
        labelMedium = base.labelMedium.copy(fontWeight = FontWeight.Medium, letterSpacing = 0.2.sp),
    )
}

@Composable
fun BabyRegistryTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        shapes = AppShapes,
        typography = AppTypography,
        content = content,
    )
}
