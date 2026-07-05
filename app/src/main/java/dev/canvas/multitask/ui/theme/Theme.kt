package dev.canvas.multitask.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// ─── Brand Colors ────────────────────────────────────────────────────────────

// Primary: Deep indigo → electric blue gradient feel
val CanvasPrimary = Color(0xFF6366F1)       // Indigo 500
val CanvasPrimaryLight = Color(0xFF818CF8)  // Indigo 400
val CanvasPrimaryDark = Color(0xFF4F46E5)   // Indigo 600
val CanvasOnPrimary = Color(0xFFFFFFFF)

// Secondary: Cyan accent for interactive elements
val CanvasSecondary = Color(0xFF06B6D4)     // Cyan 500
val CanvasSecondaryLight = Color(0xFF22D3EE)
val CanvasOnSecondary = Color(0xFF000000)

// Tertiary: Warm amber for highlights / warnings
val CanvasTertiary = Color(0xFFF59E0B)      // Amber 500
val CanvasOnTertiary = Color(0xFF000000)

// Surfaces — Dark mode as default (power-user audience)
val CanvasSurfaceDark = Color(0xFF0F172A)       // Slate 900
val CanvasSurfaceContainerDark = Color(0xFF1E293B) // Slate 800
val CanvasOnSurfaceDark = Color(0xFFF1F5F9)     // Slate 100
val CanvasOnSurfaceVariantDark = Color(0xFF94A3B8)  // Slate 400

// Light mode surfaces
val CanvasSurfaceLight = Color(0xFFF8FAFC)      // Slate 50
val CanvasSurfaceContainerLight = Color(0xFFFFFFFF)
val CanvasOnSurfaceLight = Color(0xFF0F172A)    // Slate 900
val CanvasOnSurfaceVariantLight = Color(0xFF64748B) // Slate 500

// Error
val CanvasError = Color(0xFFEF4444)
val CanvasOnError = Color(0xFFFFFFFF)

// ─── Color Schemes ───────────────────────────────────────────────────────────

private val DarkColorScheme = darkColorScheme(
    primary = CanvasPrimary,
    onPrimary = CanvasOnPrimary,
    primaryContainer = CanvasPrimaryDark,
    onPrimaryContainer = CanvasPrimaryLight,
    secondary = CanvasSecondary,
    onSecondary = CanvasOnSecondary,
    secondaryContainer = Color(0xFF164E63),
    onSecondaryContainer = CanvasSecondaryLight,
    tertiary = CanvasTertiary,
    onTertiary = CanvasOnTertiary,
    background = CanvasSurfaceDark,
    onBackground = CanvasOnSurfaceDark,
    surface = CanvasSurfaceDark,
    onSurface = CanvasOnSurfaceDark,
    surfaceVariant = CanvasSurfaceContainerDark,
    onSurfaceVariant = CanvasOnSurfaceVariantDark,
    error = CanvasError,
    onError = CanvasOnError,
    outline = Color(0xFF475569),
)

private val LightColorScheme = lightColorScheme(
    primary = CanvasPrimary,
    onPrimary = CanvasOnPrimary,
    primaryContainer = Color(0xFFE0E7FF),
    onPrimaryContainer = CanvasPrimaryDark,
    secondary = CanvasSecondary,
    onSecondary = CanvasOnSecondary,
    secondaryContainer = Color(0xFFCFFAFE),
    onSecondaryContainer = Color(0xFF164E63),
    tertiary = CanvasTertiary,
    onTertiary = CanvasOnTertiary,
    background = CanvasSurfaceLight,
    onBackground = CanvasOnSurfaceLight,
    surface = CanvasSurfaceLight,
    onSurface = CanvasOnSurfaceLight,
    surfaceVariant = CanvasSurfaceContainerLight,
    onSurfaceVariant = CanvasOnSurfaceVariantLight,
    error = CanvasError,
    onError = CanvasOnError,
    outline = Color(0xFFCBD5E1),
)

// ─── Theme Composable ────────────────────────────────────────────────────────

@Composable
fun CanvasTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        // Use dynamic Material You colors on Android 12+ if enabled
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context)
            else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = CanvasTypography,
        content = content
    )
}
