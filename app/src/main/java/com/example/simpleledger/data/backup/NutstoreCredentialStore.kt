package com.example.simpleledger.data.backup

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class NutstoreCredentials(
    val username: String,
    val password: String,
)

data class NutstoreSettings(
    val username: String = "",
    val hasCredentials: Boolean = false,
    val automaticBackupEnabled: Boolean = true,
    val lastBackupAtEpochMs: Long? = null,
)

class NutstoreCredentialStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE,
    )
    private val mutableSettings = MutableStateFlow(readSettings())

    val settings: StateFlow<NutstoreSettings> = mutableSettings.asStateFlow()

    fun saveCredentials(username: String, password: String) {
        val normalizedUsername = username.trim()
        require(normalizedUsername.isNotEmpty()) { "坚果云账号不能为空" }
        require(password.isNotEmpty()) { "坚果云第三方应用密码不能为空" }
        val encryptedPassword = encrypt(password, normalizedUsername)
        preferences.edit()
            .putString(KEY_USERNAME, normalizedUsername)
            .putString(KEY_PASSWORD, encryptedPassword)
            .remove(KEY_LAST_FINGERPRINT)
            .remove(KEY_LAST_BACKUP_AT)
            .apply()
        mutableSettings.value = readSettings()
    }

    fun credentials(): NutstoreCredentials? {
        val username = preferences.getString(KEY_USERNAME, null)?.trim().orEmpty()
        val encryptedPassword = preferences.getString(KEY_PASSWORD, null).orEmpty()
        if (username.isEmpty() || encryptedPassword.isEmpty()) return null
        return runCatching {
            NutstoreCredentials(username, decrypt(encryptedPassword, username))
        }.getOrNull()
    }

    fun setAutomaticBackupEnabled(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_AUTOMATIC_BACKUP, enabled).apply()
        mutableSettings.value = readSettings()
    }

    fun lastFingerprint(): String? = preferences.getString(KEY_LAST_FINGERPRINT, null)

    fun markBackupSucceeded(fingerprint: String, timestampEpochMs: Long) {
        preferences.edit()
            .putString(KEY_LAST_FINGERPRINT, fingerprint)
            .putLong(KEY_LAST_BACKUP_AT, timestampEpochMs)
            .apply()
        mutableSettings.value = readSettings()
    }

    private fun readSettings(): NutstoreSettings {
        val username = preferences.getString(KEY_USERNAME, null)?.trim().orEmpty()
        val hasPassword = !preferences.getString(KEY_PASSWORD, null).isNullOrEmpty()
        val lastBackup = preferences.getLong(KEY_LAST_BACKUP_AT, -1L).takeIf { it >= 0L }
        return NutstoreSettings(
            username = username,
            hasCredentials = username.isNotEmpty() && hasPassword,
            automaticBackupEnabled = preferences.getBoolean(KEY_AUTOMATIC_BACKUP, true),
            lastBackupAtEpochMs = lastBackup,
        )
    }

    private fun encrypt(plainText: String, username: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        cipher.updateAAD(username.toByteArray(Charsets.UTF_8))
        val encrypted = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        val packed = ByteArray(cipher.iv.size + encrypted.size)
        cipher.iv.copyInto(packed)
        encrypted.copyInto(packed, destinationOffset = cipher.iv.size)
        return Base64.encodeToString(packed, Base64.NO_WRAP)
    }

    private fun decrypt(encoded: String, username: String): String {
        val packed = Base64.decode(encoded, Base64.NO_WRAP)
        require(packed.size > IV_BYTES) { "加密凭据无效" }
        val iv = packed.copyOfRange(0, IV_BYTES)
        val encrypted = packed.copyOfRange(IV_BYTES, packed.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(GCM_TAG_BITS, iv))
        cipher.updateAAD(username.toByteArray(Charsets.UTF_8))
        return cipher.doFinal(encrypted).toString(Charsets.UTF_8)
    }

    private fun secretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER)
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build(),
        )
        return generator.generateKey()
    }

    private companion object {
        const val PREFERENCES_NAME = "nutstore_backup"
        const val KEY_USERNAME = "username"
        const val KEY_PASSWORD = "encrypted_password"
        const val KEY_AUTOMATIC_BACKUP = "automatic_backup"
        const val KEY_LAST_FINGERPRINT = "last_fingerprint"
        const val KEY_LAST_BACKUP_AT = "last_backup_at"
        const val KEY_ALIAS = "simple_ledger_nutstore_password"
        const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val IV_BYTES = 12
        const val GCM_TAG_BITS = 128
    }
}
