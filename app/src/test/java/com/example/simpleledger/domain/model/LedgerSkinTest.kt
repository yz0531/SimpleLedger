package com.example.simpleledger.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LedgerSkinTest {
    @Test
    fun everyImageSkinHasItsOwnPalette() {
        val imageSkins = LedgerSkin.entries.filter(LedgerSkin::hasImage)

        assertEquals(imageSkins.size, imageSkins.map(LedgerSkin::defaultColor).distinct().size)
    }

    @Test
    fun foregroundColorsMatchEachImageBrightnessMode() {
        LedgerSkin.entries.filter(LedgerSkin::hasImage).forEach { skin ->
            val foregroundLuminance = relativeLuminance(skin.foregroundArgb)
            val mutedLuminance = relativeLuminance(skin.mutedForegroundArgb)
            if (skin.prefersDarkUi) {
                assertTrue("${skin.name} 主文字应为浅色", foregroundLuminance >= 0.75)
                assertTrue("${skin.name} 次文字应为浅色", mutedLuminance >= 0.80)
            } else {
                assertTrue("${skin.name} 主文字应为深色", foregroundLuminance <= 0.08)
                assertTrue("${skin.name} 次文字应为深色", mutedLuminance <= 0.07)
            }
        }
    }

    private fun relativeLuminance(argb: Long): Double {
        fun channel(shift: Int): Double {
            val value = ((argb shr shift) and 0xFF).toDouble() / 255.0
            return if (value <= 0.04045) value / 12.92 else Math.pow((value + 0.055) / 1.055, 2.4)
        }
        return 0.2126 * channel(16) + 0.7152 * channel(8) + 0.0722 * channel(0)
    }
}
