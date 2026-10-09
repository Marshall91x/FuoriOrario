package it.manu.fuoriorario.core.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

private val LocalFuoriOrarioColors = staticCompositionLocalOf { LightColors }

/** Light/dark follows the system, like the prototype's `prefers-color-scheme`. */
@Composable
fun FuoriOrarioTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val c = if (dark) DarkColors else LightColors
    val scheme = (if (dark) darkColorScheme() else lightColorScheme()).copy(
        primary = c.accent,
        onPrimary = c.accentInk,
        primaryContainer = c.accentSoft,
        background = c.bg,
        onBackground = c.ink,
        surface = c.surface,
        onSurface = c.ink,
        surfaceVariant = c.surface2,
        onSurfaceVariant = c.muted,
        outline = c.line,
        outlineVariant = c.line,
        error = c.accent
    )
    // LocalContentColor: Text without an explicit color uses ink, not Material's default black.
    CompositionLocalProvider(LocalFuoriOrarioColors provides c, LocalContentColor provides c.ink) {
        MaterialTheme(colorScheme = scheme, typography = foTypography(), content = content)
    }
}

object FuoriOrarioTheme {
    val colors: FuoriOrarioColors
        @Composable @ReadOnlyComposable
        get() = LocalFuoriOrarioColors.current
}
