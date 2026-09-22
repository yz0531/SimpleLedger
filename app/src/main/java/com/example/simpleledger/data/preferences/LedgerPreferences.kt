package com.example.simpleledger.data.preferences

import android.content.Context
import com.example.simpleledger.domain.model.LedgerAppearance
import com.example.simpleledger.domain.model.LedgerColor
import com.example.simpleledger.domain.model.LedgerMode
import com.example.simpleledger.domain.model.LedgerSkin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class LedgerPreferences(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE,
    )
    private val mutableMode = MutableStateFlow(readMode())
    private val mutableAppearance = MutableStateFlow(readAppearance())

    val mode: StateFlow<LedgerMode> = mutableMode.asStateFlow()
    val appearance: StateFlow<LedgerAppearance> = mutableAppearance.asStateFlow()

    val currentMode: LedgerMode
        get() = mutableMode.value

    fun setMode(mode: LedgerMode) {
        preferences.edit().putString(KEY_LEDGER_MODE, mode.name).apply()
        mutableMode.value = mode
    }

    fun setSkin(skin: LedgerSkin) {
        val current = mutableAppearance.value
        val updated = current.copy(
            skin = skin,
            color = if (skin.hasImage) skin.defaultColor else current.color,
        )
        preferences.edit()
            .putString(KEY_LEDGER_SKIN, updated.skin.name)
            .putString(KEY_LEDGER_COLOR, updated.color.name)
            .apply()
        mutableAppearance.value = updated
    }

    fun setColor(color: LedgerColor) {
        preferences.edit().putString(KEY_LEDGER_COLOR, color.name).apply()
        mutableAppearance.value = mutableAppearance.value.copy(color = color)
    }

    fun setImageOpacity(opacity: Float) {
        val normalized = opacity.coerceIn(0f, 1f)
        preferences.edit().putFloat(KEY_IMAGE_OPACITY, normalized).apply()
        mutableAppearance.value = mutableAppearance.value.copy(imageOpacity = normalized)
    }

    private fun readMode(): LedgerMode = preferences.getString(KEY_LEDGER_MODE, null)
        ?.let { stored -> runCatching { LedgerMode.valueOf(stored) }.getOrNull() }
        ?: LedgerMode.EXPENSE_ONLY

    private fun readAppearance(): LedgerAppearance {
        val skin = preferences.getString(KEY_LEDGER_SKIN, null)
            ?.let { stored -> runCatching { LedgerSkin.valueOf(stored) }.getOrNull() }
            ?: LedgerSkin.JADE
        val color = preferences.getString(KEY_LEDGER_COLOR, null)
            ?.let { stored -> runCatching { LedgerColor.valueOf(stored) }.getOrNull() }
            ?: skin.defaultColor
        val opacity = preferences.getFloat(
            KEY_IMAGE_OPACITY,
            LedgerAppearance.DEFAULT_IMAGE_OPACITY,
        ).coerceIn(0f, 1f)
        return LedgerAppearance(skin = skin, color = color, imageOpacity = opacity)
    }

    private companion object {
        const val PREFERENCES_NAME = "ledger_preferences"
        const val KEY_LEDGER_MODE = "ledger_mode"
        const val KEY_LEDGER_SKIN = "ledger_skin"
        const val KEY_LEDGER_COLOR = "ledger_color"
        const val KEY_IMAGE_OPACITY = "skin_image_opacity"
    }
}
