package com.example.simpleledger.domain.model

enum class LedgerSkin(
    val displayName: String,
    val description: String,
) {
    JADE("青玉", "雨林蕨叶"),
    SUNSET("暖阳", "赤岩公路"),
    OCEAN("海风", "晨光海岸"),
    LAVENDER("丁香", "薰衣草原"),
    FOREST("森屿", "深绿蕨林"),
    AMBER("琥珀", "暖调峡谷"),
    ALPINE("雪岚", "清冷海湾"),
    ROSE("绯霞", "玫瑰花田"),
    NIGHT("夜潮", "暮色海岸"),
    AURORA("极光", "幻彩花原"),
}
