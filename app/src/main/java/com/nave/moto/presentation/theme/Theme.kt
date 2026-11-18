package com.nave.moto.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

private val Dark = darkColorScheme(
    primary = DarkPrimary, onPrimary = DarkOnPrimary, primaryContainer = DarkPrimaryContainer, onPrimaryContainer = DarkOnPrimaryContainer,
    secondary = DarkSecondary, onSecondary = DarkOnSecondary, secondaryContainer = DarkSecondaryContainer, onSecondaryContainer = DarkOnSecondaryContainer,
    background = DarkSurface, onBackground = DarkOnSurface, surface = DarkSurface, onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceContainer, onSurfaceVariant = DarkOnSurfaceVariant,
    surfaceContainerLowest = DarkSurfaceLowest, surfaceContainerLow = DarkSurfaceLow,
    surfaceContainer = DarkSurfaceContainer, surfaceContainerHigh = DarkSurfaceHigh, surfaceContainerHighest = DarkSurfaceHigh,
    outline = DarkOutline, outlineVariant = DarkOutlineVariant,
    error = DarkError, onError = DarkOnError, errorContainer = DarkErrorContainer, onErrorContainer = DarkOnErrorContainer
)

private val Light = lightColorScheme(
    primary = LightPrimary, onPrimary = LightOnPrimary, primaryContainer = LightPrimaryContainer, onPrimaryContainer = LightOnPrimaryContainer,
    secondary = LightSecondary, onSecondary = LightOnSecondary, secondaryContainer = LightSecondaryContainer, onSecondaryContainer = LightOnSecondaryContainer,
    background = LightSurface, onBackground = LightOnSurface, surface = LightSurface, onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceContainer, onSurfaceVariant = LightOnSurfaceVariant,
    surfaceContainerLowest = LightSurfaceLowest, surfaceContainerLow = LightSurfaceLow,
    surfaceContainer = LightSurfaceContainer, surfaceContainerHigh = LightSurfaceHigh, surfaceContainerHighest = LightSurfaceHigh,
    outline = LightOutline, outlineVariant = LightOutlineVariant,
    error = LightError, onError = LightOnError, errorContainer = LightErrorContainer, onErrorContainer = LightOnErrorContainer
)

/** true = español. Se usa con tr("es", "en"). */
val LocalEs = compositionLocalOf { true }

@Composable
fun tr(es: String, en: String): String = if (LocalEs.current) es else en

/** Modo sol / alto contraste: mismos tonos de marca, superficies y textos al extremo. */
private val DarkHC = Dark.copy(
    surface = Color(0xFF000000), background = Color(0xFF000000), surfaceContainerLowest = Color(0xFF000000),
    surfaceContainerLow = Color(0xFF121212), surfaceContainer = Color(0xFF1A1A1A), surfaceContainerHigh = Color(0xFF2E2E2E), surfaceContainerHighest = Color(0xFF2E2E2E),
    onSurface = Color(0xFFFFFFFF), onBackground = Color(0xFFFFFFFF), onSurfaceVariant = Color(0xFFE6E6E6),
    outline = Color(0xFFBDBDBD), outlineVariant = Color(0xFF8A8A8A),
    primary = Color(0xFFFF7A66), secondary = Color(0xFFFFD84D), secondaryContainer = Color(0xFFFFD84D), onSecondaryContainer = Color(0xFF000000),
    error = Color(0xFFFFB4AB)
)
private val LightHC = Light.copy(
    surface = Color(0xFFFFFFFF), background = Color(0xFFFFFFFF), surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF2F2F2), surfaceContainer = Color(0xFFE6E6E6), surfaceContainerHigh = Color(0xFFD6D6D6), surfaceContainerHighest = Color(0xFFD6D6D6),
    onSurface = Color(0xFF000000), onBackground = Color(0xFF000000), onSurfaceVariant = Color(0xFF1F1F1F),
    outline = Color(0xFF444444), outlineVariant = Color(0xFF666666),
    primary = Color(0xFF8A1200), secondary = Color(0xFF4F4200), secondaryContainer = Color(0xFFFFE680), onSecondaryContainer = Color(0xFF000000)
)

/** Háptica activada (ajuste). */
val LocalHaptics = compositionLocalOf { true }

@Composable
fun NaveMotoTheme(darkTheme: Boolean = true, highContrast: Boolean = false, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = when { darkTheme && highContrast -> DarkHC; darkTheme -> Dark; highContrast -> LightHC; else -> Light },
        typography = NaveTypography,
        shapes = NaveShapes,
        content = content
    )
}
