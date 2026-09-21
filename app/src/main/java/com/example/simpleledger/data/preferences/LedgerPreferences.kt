package com.example.simpleledger.data.preferences

import android.content.Context
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
    private val mutableSkin = MutableStateFlow(readSkin())

    val mode: StateFlow<LedgerMode> = mutableMode.asStateFlow()
    val skin: StateFlow<LedgerSkin> = mutableSkin.asStateFlow()

    val currentMode: LedgerMode
        get() = mutableMode.value

    fun setMode(mode: LedgerMode) {
        preferences.edit().putString(KEY_LEDGER_MODE, mode.name).apply()
        mutableMode.value = mode
    }

    fun setSkin(skin: LedgerSkin) {
        preferences.edit().putString(KEY_LEDGER_SKIN, skin.name).apply()
        mutableSkin.value = skin
    }

    private fun readMode(): LedgerMode = preferences.getString(KEY_LEDGER_MODE, null)
        ?.let { stored -> runCatching { LedgerMode.valueOf(stored) }.getOrNull() }
        ?: LedgerMode.EXPENSE_ONLY

    private fun readSkin(): LedgerSkin = preferences.getString(KEY_LEDGER_SKIN, null)
        ?.let { stored -> runCatching { LedgerSkin.valueOf(stored) }.getOrNull() }
        ?: LedgerSkin.JADE

    private companion object {
        const val PREFERENCES_NAME = "ledger_preferences"
        const val KEY_LEDGER_MODE = "ledger_mode"
        const val KEY_LEDGER_SKIN = "ledger_skin"
    }
}
