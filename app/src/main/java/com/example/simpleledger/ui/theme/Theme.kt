package com.example.simpleledger.ui.theme

import android.app.Activity
import android.graphics.Color as AndroidColor
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.example.simpleledger.domain.model.LedgerAppearance
import com.example.simpleledger.domain.model.LedgerColor

private val JadeLight = lightColorScheme(
    primary = Color(0xFF236B5E),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFC4F2E4),
    onPrimaryContainer = Color(0xFF073B32),
    secondary = Color(0xFF8A6A13),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFE8A5),
    onSecondaryContainer = Color(0xFF3C2F00),
    tertiary = Color(0xFF416684),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFD0E7FA),
    onTertiaryContainer = Color(0xFF17374F),
    background = Color(0xFFF5FAF7),
    surface = Color(0xFFF8FCF9),
    surfaceVariant = Color(0xFFDCE7E1),
    error = Color(0xFFB64B3A),
    errorContainer = Color(0xFFFFDAD3),
    onErrorContainer = Color(0xFF410001),
)

private val SunsetLight = lightColorScheme(
    primary = Color(0xFFA94F45),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDAD5),
    onPrimaryContainer = Color(0xFF4B1712),
    secondary = Color(0xFF8A5D10),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFE1A1),
    onSecondaryContainer = Color(0xFF3A2A00),
    tertiary = Color(0xFF75608B),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFECDDFE),
    onTertiaryContainer = Color(0xFF38284A),
    background = Color(0xFFFFF8F4),
    surface = Color(0xFFFFFAF7),
    surfaceVariant = Color(0xFFF1DFDA),
    error = Color(0xFFBA1A1A),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
)

private val OceanLight = lightColorScheme(
    primary = Color(0xFF246587),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFC8E7FF),
    onPrimaryContainer = Color(0xFF07364F),
    secondary = Color(0xFF3D6471),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFC5E9F6),
    onSecondaryContainer = Color(0xFF153A46),
    tertiary = Color(0xFF52649A),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFDCE1FF),
    onTertiaryContainer = Color(0xFF263662),
    background = Color(0xFFF4FAFD),
    surface = Color(0xFFF8FCFF),
    surfaceVariant = Color(0xFFDCE7EC),
    error = Color(0xFFB64B3A),
    errorContainer = Color(0xFFFFDAD3),
    onErrorContainer = Color(0xFF410001),
)

private val LavenderLight = lightColorScheme(
    primary = Color(0xFF72558D),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEBDDFB),
    onPrimaryContainer = Color(0xFF3E2855),
    secondary = Color(0xFF786075),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFD8F7),
    onSecondaryContainer = Color(0xFF452F43),
    tertiary = Color(0xFF8B5D61),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFDADB),
    onTertiaryContainer = Color(0xFF502E32),
    background = Color(0xFFFCF8FF),
    surface = Color(0xFFFFFAFF),
    surfaceVariant = Color(0xFFE9E0EA),
    error = Color(0xFFBA1A1A),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
)

private fun darkScheme(light: ColorScheme): ColorScheme = darkColorScheme(
    primary = light.primaryContainer,
    onPrimary = light.onPrimaryContainer,
    primaryContainer = light.primary.copy(alpha = 0.72f),
    onPrimaryContainer = light.primaryContainer,
    secondary = light.secondaryContainer,
    onSecondary = light.onSecondaryContainer,
    secondaryContainer = light.secondary.copy(alpha = 0.62f),
    onSecondaryContainer = light.secondaryContainer,
    tertiary = light.tertiaryContainer,
    onTertiary = light.onTertiaryContainer,
    tertiaryContainer = light.tertiary.copy(alpha = 0.62f),
    onTertiaryContainer = light.tertiaryContainer,
    background = Color(0xFF111513),
    surface = Color(0xFF151916),
    surfaceVariant = Color(0xFF3F4843),
    error = Color(0xFFFFB4AB),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
)

private fun LedgerColor.lightScheme(): ColorScheme = when (this) {
    LedgerColor.JADE -> JadeLight
    LedgerColor.SUNSET -> SunsetLight
    LedgerColor.OCEAN -> OceanLight
    LedgerColor.LAVENDER -> LavenderLight
    LedgerColor.AMBER -> SunsetLight.copy(
        primary = Color(0xFF98500F),
        primaryContainer = Color(0xFFFFD9B5),
        onPrimaryContainer = Color(0xFF4A2500),
        secondary = Color(0xFF7A5D18),
    )
    LedgerColor.ALPINE -> OceanLight.copy(
        primary = Color(0xFF25667A),
        primaryContainer = Color(0xFFBDEAF4),
        onPrimaryContainer = Color(0xFF063740),
        tertiary = Color(0xFF4D6686),
    )
    LedgerColor.ROSE -> LavenderLight.copy(
        primary = Color(0xFF9A4761),
        primaryContainer = Color(0xFFFFD9E2),
        onPrimaryContainer = Color(0xFF54142B),
        tertiary = Color(0xFF7A5B83),
    )
    LedgerColor.GRAPHITE -> OceanLight.copy(
        primary = Color(0xFF4F626B),
        primaryContainer = Color(0xFFD2E5EC),
        onPrimaryContainer = Color(0xFF1D343D),
        secondary = Color(0xFF596268),
        tertiary = Color(0xFF5F5E70),
    )
    LedgerColor.FOREST -> JadeLight.copy(
        primary = Color(0xFF376149),
        primaryContainer = Color(0xFFC9ECCE),
        onPrimaryContainer = Color(0xFF153B22),
        secondary = Color(0xFF536B57),
        tertiary = Color(0xFF526B63),
    )
    LedgerColor.NIGHT -> OceanLight.copy(
        primary = Color(0xFF48598C),
        primaryContainer = Color(0xFFDCE2FF),
        onPrimaryContainer = Color(0xFF162452),
        secondary = Color(0xFF5B607A),
        tertiary = Color(0xFF765984),
    )
    LedgerColor.AURORA -> OceanLight.copy(
        primary = Color(0xFF356A70),
        primaryContainer = Color(0xFFBDECEF),
        onPrimaryContainer = Color(0xFF063A3E),
        secondary = Color(0xFF66578A),
        secondaryContainer = Color(0xFFE9DDFF),
        onSecondaryContainer = Color(0xFF32265A),
        tertiary = Color(0xFF6B4E72),
        tertiaryContainer = Color(0xFFF4D8F5),
        onTertiaryContainer = Color(0xFF3D2442),
    )
}

private fun ColorScheme.withSkinForeground(appearance: LedgerAppearance): ColorScheme {
    if (!appearance.skin.hasImage) return this
    val foreground = Color(appearance.skin.foregroundArgb)
    val mutedForeground = Color(appearance.skin.mutedForegroundArgb)
    return copy(
        onBackground = foreground,
        onSurface = foreground,
        onSurfaceVariant = mutedForeground,
        outline = mutedForeground.copy(alpha = 0.78f),
        outlineVariant = mutedForeground.copy(alpha = 0.45f),
    )
}

@Composable
fun SimpleLedgerTheme(
    appearance: LedgerAppearance,
    content: @Composable () -> Unit,
) {
    val darkTheme = if (appearance.skin.hasImage) {
        appearance.skin.prefersDarkUi
    } else {
        isSystemInDarkTheme()
    }
    val lightScheme = appearance.color.lightScheme()
    val baseColorScheme = if (darkTheme) darkScheme(lightScheme) else lightScheme
    val colorScheme = baseColorScheme.withSkinForeground(appearance)
    val context = LocalContext.current
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (context as? Activity)?.window ?: return@SideEffect
            window.statusBarColor = AndroidColor.TRANSPARENT
            window.navigationBarColor = AndroidColor.TRANSPARENT
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = LedgerTypography,
        content = content,
    )
}
