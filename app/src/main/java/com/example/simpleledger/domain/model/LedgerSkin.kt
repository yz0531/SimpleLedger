package com.example.simpleledger.domain.model

enum class LedgerColor(
    val displayName: String,
    val swatchArgb: Long,
    val lightBackgroundArgb: Long,
    val darkBackgroundArgb: Long,
) {
    JADE("青玉", 0xFF236B5E, 0xFFE8F4EE, 0xFF111513),
    OCEAN("海蓝", 0xFF246587, 0xFFE8F3F8, 0xFF10161A),
    SUNSET("暖橘", 0xFFA94F45, 0xFFFFEEE8, 0xFF1A1210),
    LAVENDER("丁香", 0xFF72558D, 0xFFF2EBF8, 0xFF17121B),
    ROSE("玫瑰", 0xFF9A4761, 0xFFFFEBF0, 0xFF1B1115),
    AMBER("琥珀", 0xFF98500F, 0xFFFFF0DC, 0xFF1B140D),
    ALPINE("雪青", 0xFF25667A, 0xFFE7F3F5, 0xFF101719),
    GRAPHITE("石墨", 0xFF4F626B, 0xFFEDF1F2, 0xFF111416),
}

enum class LedgerSkin(
    val displayName: String,
    val description: String,
    val defaultColor: LedgerColor,
    val hasImage: Boolean = true,
) {
    BASIC("基础", "简洁纯色", LedgerColor.JADE, hasImage = false),
    JADE("青玉", "雨林蕨叶", LedgerColor.JADE),
    SUNSET("暖阳", "赤岩公路", LedgerColor.SUNSET),
    OCEAN("海风", "晨光海岸", LedgerColor.OCEAN),
    LAVENDER("丁香", "薰衣草原", LedgerColor.LAVENDER),
    FOREST("森屿", "深绿蕨林", LedgerColor.JADE),
    AMBER("琥珀", "暖调峡谷", LedgerColor.AMBER),
    ALPINE("雪岚", "清冷海湾", LedgerColor.ALPINE),
    ROSE("绯霞", "玫瑰花田", LedgerColor.ROSE),
    NIGHT("夜潮", "暮色海岸", LedgerColor.GRAPHITE),
    AURORA("极光", "幻彩花原", LedgerColor.LAVENDER),
}

data class LedgerAppearance(
    val skin: LedgerSkin = LedgerSkin.JADE,
    val color: LedgerColor = LedgerColor.JADE,
    val imageOpacity: Float = DEFAULT_IMAGE_OPACITY,
) {
    init {
        require(imageOpacity in 0f..1f) { "imageOpacity must be between 0 and 1" }
    }

    companion object {
        const val DEFAULT_IMAGE_OPACITY = 0.90f
    }
}
