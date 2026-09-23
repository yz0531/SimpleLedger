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
    FOREST("松绿", 0xFF376149, 0xFFE7F1E9, 0xFF0E1711),
    NIGHT("夜蓝", 0xFF48598C, 0xFFEDF0FA, 0xFF0D111C),
    AURORA("极光", 0xFF356A70, 0xFFE5F3F2, 0xFF0B1718),
}

enum class LedgerSkin(
    val displayName: String,
    val description: String,
    val defaultColor: LedgerColor,
    val hasImage: Boolean = true,
    val prefersDarkUi: Boolean = false,
    val foregroundArgb: Long = 0xFF171A18,
    val mutedForegroundArgb: Long = 0xFF47504B,
) {
    BASIC("基础", "简洁纯色", LedgerColor.JADE, hasImage = false),
    JADE(
        "青玉", "雨林蕨叶", LedgerColor.JADE,
        prefersDarkUi = true,
        foregroundArgb = 0xFFF4FFF7,
        mutedForegroundArgb = 0xFFE4F6E8,
    ),
    SUNSET(
        "暖阳", "赤岩公路", LedgerColor.SUNSET,
        prefersDarkUi = true,
        foregroundArgb = 0xFFFFF7F1,
        mutedForegroundArgb = 0xFFFAEAE2,
    ),
    OCEAN(
        "海风", "晨光海岸", LedgerColor.OCEAN,
        foregroundArgb = 0xFF092A3A,
        mutedForegroundArgb = 0xFF24404D,
    ),
    LAVENDER(
        "丁香", "薰衣草原", LedgerColor.LAVENDER,
        prefersDarkUi = true,
        foregroundArgb = 0xFFFFF8FF,
        mutedForegroundArgb = 0xFFF3E9FA,
    ),
    FOREST(
        "森屿", "雾光松林", LedgerColor.FOREST,
        prefersDarkUi = true,
        foregroundArgb = 0xFFF2FFF5,
        mutedForegroundArgb = 0xFFE0F4E4,
    ),
    AMBER(
        "琥珀", "金色沙丘", LedgerColor.AMBER,
        foregroundArgb = 0xFF2F1C0B,
        mutedForegroundArgb = 0xFF50331E,
    ),
    ALPINE(
        "雪岚", "雪峰云海", LedgerColor.ALPINE,
        foregroundArgb = 0xFF102B3A,
        mutedForegroundArgb = 0xFF304A55,
    ),
    ROSE(
        "绯霞", "春日樱花", LedgerColor.ROSE,
        foregroundArgb = 0xFF401625,
        mutedForegroundArgb = 0xFF5C2D3C,
    ),
    NIGHT(
        "夜潮", "银河雪峰", LedgerColor.NIGHT,
        prefersDarkUi = true,
        foregroundArgb = 0xFFF7F8FF,
        mutedForegroundArgb = 0xFFE7EBFA,
    ),
    AURORA(
        "极光", "北境极光", LedgerColor.AURORA,
        prefersDarkUi = true,
        foregroundArgb = 0xFFF0FFFC,
        mutedForegroundArgb = 0xFFDFF5F0,
    ),
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
