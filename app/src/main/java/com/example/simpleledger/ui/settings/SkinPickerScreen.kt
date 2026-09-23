package com.example.simpleledger.ui.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items as lazyRowItems
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.simpleledger.domain.model.LedgerAppearance
import com.example.simpleledger.domain.model.LedgerColor
import com.example.simpleledger.domain.model.LedgerSkin
import com.example.simpleledger.ui.components.CompactTopBar
import com.example.simpleledger.ui.theme.thumbnailResId
import kotlin.math.roundToInt

@Composable
fun SkinPickerScreen(
    appearance: LedgerAppearance,
    onSkinSelected: (LedgerSkin) -> Unit,
    onColorSelected: (LedgerColor) -> Unit,
    onOpacityChanged: (Float) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onBackground,
        topBar = {
            CompactTopBar(
                title = "外观皮肤",
                subtitle = "选择后立即应用到全部页面",
                navigationIcon = Icons.AutoMirrored.Rounded.ArrowBack,
                onNavigationClick = onBack,
            )
        },
    ) { innerPadding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                AppearanceControls(
                    appearance = appearance,
                    onColorSelected = onColorSelected,
                    onOpacityChanged = onOpacityChanged,
                )
            }
            items(LedgerSkin.entries, key = LedgerSkin::name) { skin ->
                SkinPreviewCard(
                    skin = skin,
                    appearance = appearance,
                    selected = skin == appearance.skin,
                    onClick = { onSkinSelected(skin) },
                )
            }
        }
    }
}

@Composable
private fun AppearanceControls(
    appearance: LedgerAppearance,
    onColorSelected: (LedgerColor) -> Unit,
    onOpacityChanged: (Float) -> Unit,
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.92f),
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            if (!appearance.skin.hasImage) {
                Text("基础颜色", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    "基础纯色可以自由选择颜色",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp),
                )
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp),
                ) {
                    lazyRowItems(LedgerColor.entries, key = LedgerColor::name) { color ->
                        ColorSwatch(
                            color = color,
                            selected = color == appearance.color,
                            onClick = { onColorSelected(color) },
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Color(appearance.skin.defaultColor.swatchArgb)),
                    )
                    Column(Modifier.padding(start = 12.dp)) {
                        Text("专属配色", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(
                            "${appearance.skin.displayName} · ${appearance.skin.defaultColor.displayName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            if (appearance.skin.hasImage) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text("图片透明度", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "${(appearance.imageOpacity * 100).roundToInt()}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Slider(
                    value = appearance.imageOpacity,
                    onValueChange = onOpacityChanged,
                    valueRange = 0.25f..1f,
                    steps = 14,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun ColorSwatch(
    color: LedgerColor,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(50)
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(shape)
            .background(Color(color.swatchArgb))
            .then(
                if (selected) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, shape) else Modifier,
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Icon(
                Icons.Rounded.CheckCircle,
                contentDescription = "${color.displayName}，已选择",
                tint = Color.White,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun SkinPreviewCard(
    skin: LedgerSkin,
    appearance: LedgerAppearance,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(22.dp)
    Card(
        onClick = onClick,
        modifier = Modifier.then(
            if (selected) {
                Modifier.border(3.dp, MaterialTheme.colorScheme.primary, shape)
            } else {
                Modifier
            },
        ),
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(188.dp)
                .clip(shape),
        ) {
            val previewColor = Color(
                if (skin.hasImage) skin.defaultColor.swatchArgb else appearance.color.swatchArgb,
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            listOf(previewColor.copy(alpha = 0.82f), previewColor.copy(alpha = 0.46f)),
                        ),
                    ),
            )
            skin.thumbnailResId()?.let { thumbnailResId ->
                Image(
                    painter = painterResource(thumbnailResId),
                    contentDescription = "${skin.displayName}皮肤预览",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    alpha = appearance.imageOpacity,
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.12f),
                                Color.Black.copy(alpha = 0.76f),
                            ),
                        ),
                    ),
            )
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(14.dp),
            ) {
                Text(
                    text = skin.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
                Text(
                    text = skin.description,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.84f),
                )
            }
            if (selected) {
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color.Black.copy(alpha = 0.46f))
                        .padding(horizontal = 9.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Rounded.CheckCircle,
                        contentDescription = "已选择",
                        tint = Color.White,
                    )
                    Text(
                        text = "使用中",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White,
                        modifier = Modifier.padding(start = 4.dp),
                    )
                }
            }
        }
    }
}
