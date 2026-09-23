package com.example.simpleledger.ui.theme

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.example.simpleledger.R
import com.example.simpleledger.domain.model.LedgerAppearance
import com.example.simpleledger.domain.model.LedgerSkin

@DrawableRes
internal fun LedgerSkin.backgroundResId(): Int? = when (this) {
    LedgerSkin.BASIC -> null
    LedgerSkin.JADE -> R.drawable.skin_jade
    LedgerSkin.SUNSET -> R.drawable.skin_sunset
    LedgerSkin.OCEAN -> R.drawable.skin_ocean
    LedgerSkin.LAVENDER -> R.drawable.skin_lavender
    LedgerSkin.FOREST -> R.drawable.skin_forest
    LedgerSkin.AMBER -> R.drawable.skin_amber
    LedgerSkin.ALPINE -> R.drawable.skin_alpine
    LedgerSkin.ROSE -> R.drawable.skin_rose
    LedgerSkin.NIGHT -> R.drawable.skin_night
    LedgerSkin.AURORA -> R.drawable.skin_aurora
}

@DrawableRes
internal fun LedgerSkin.thumbnailResId(): Int? = when (this) {
    LedgerSkin.BASIC -> null
    LedgerSkin.JADE -> R.drawable.skin_jade_thumb
    LedgerSkin.SUNSET -> R.drawable.skin_sunset_thumb
    LedgerSkin.OCEAN -> R.drawable.skin_ocean_thumb
    LedgerSkin.LAVENDER -> R.drawable.skin_lavender_thumb
    LedgerSkin.FOREST -> R.drawable.skin_forest_thumb
    LedgerSkin.AMBER -> R.drawable.skin_amber_thumb
    LedgerSkin.ALPINE -> R.drawable.skin_alpine_thumb
    LedgerSkin.ROSE -> R.drawable.skin_rose_thumb
    LedgerSkin.NIGHT -> R.drawable.skin_night_thumb
    LedgerSkin.AURORA -> R.drawable.skin_aurora_thumb
}

@Composable
fun SkinBackground(
    appearance: LedgerAppearance,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val darkTheme = if (appearance.skin.hasImage) {
        appearance.skin.prefersDarkUi
    } else {
        isSystemInDarkTheme()
    }
    val baseColor = Color(
        if (darkTheme) appearance.color.darkBackgroundArgb else appearance.color.lightBackgroundArgb,
    )
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(baseColor),
    ) {
        appearance.skin.backgroundResId()?.let { backgroundResId ->
            Image(
                painter = painterResource(backgroundResId),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                alpha = appearance.imageOpacity,
            )
        }
        if (!appearance.skin.hasImage) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Transparent,
                                MaterialTheme.colorScheme.background.copy(alpha = 0.20f),
                            ),
                        ),
                    ),
            )
        }
        content()
    }
}
