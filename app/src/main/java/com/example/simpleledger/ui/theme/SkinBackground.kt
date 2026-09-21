package com.example.simpleledger.ui.theme

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.example.simpleledger.R
import com.example.simpleledger.domain.model.LedgerSkin

@DrawableRes
internal fun LedgerSkin.backgroundResId(): Int = when (this) {
    LedgerSkin.JADE -> R.drawable.skin_jade
    LedgerSkin.SUNSET -> R.drawable.skin_sunset
    LedgerSkin.OCEAN -> R.drawable.skin_ocean
    LedgerSkin.LAVENDER -> R.drawable.skin_lavender
}

@Composable
fun SkinBackground(
    skin: LedgerSkin,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val darkTheme = isSystemInDarkTheme()
    Box(modifier = modifier.fillMaxSize()) {
        Image(
            painter = painterResource(skin.backgroundResId()),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    if (darkTheme) {
                        Brush.verticalGradient(
                            listOf(
                                Color(0xD9141817),
                                Color(0xE6111514),
                            ),
                        )
                    } else {
                        Brush.verticalGradient(
                            listOf(
                                Color.White.copy(alpha = 0.76f),
                                Color.White.copy(alpha = 0.88f),
                                Color(0xFFFFFBF3).copy(alpha = 0.92f),
                            ),
                        )
                    },
                ),
        )
        content()
    }
}
