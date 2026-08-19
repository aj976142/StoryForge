package com.storyforge.ai.data.local

import android.content.Context
import android.util.Base64
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Keystore-backed storage for user-owned API credentials. The secret never enters DataStore or Git. */
class SecretStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("storyforge_secrets", Context.MODE_PRIVATE)
    private val keyAlias = "storyforge_api_key"
    private val transformation = "AES/GCM/NoPadding"

    fun saveApiKey(value: String) {
        if (value.isBlank()) {
            clearApiKey()
            return
        }
        val cipher = Cipher.getInstance(transformation)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val encrypted = cipher.doFinal(value.toByteArray(StandardCharsets.UTF_8))
        prefs.edit()
            .putString("iv", Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
            .putString("ciphertext", Base64.encodeToString(encrypted, Base64.NO_WRAP))
            .apply()
    }

    fun getApiKey(): String? = runCatching {
        val iv = prefs.getString("iv", null) ?: return null
        val ciphertext = prefs.getString("ciphertext", null) ?: return null
        val cipher = Cipher.getInstance(transformation)
        cipher.init(
            Cipher.DECRYPT_MODE,
            getOrCreateKey(),
            GCMParameterSpec(128, Base64.decode(iv, Base64.NO_WRAP))
        )
        String(cipher.doFinal(Base64.decode(ciphertext, Base64.NO_WRAP)), StandardCharsets.UTF_8)
    }.getOrNull()

    fun clearApiKey() = prefs.edit().clear().apply()

    private fun getOrCreateKey(): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (ks.getKey(keyAlias, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance("AES", "AndroidKeyStore")
        generator.init(android.security.keystore.KeyGenParameterSpec.Builder(
            keyAlias,
            android.security.keystore.KeyProperties.PURPOSE_ENCRYPT or android.security.keystore.KeyProperties.PURPOSE_DECRYPT
        ).setBlockModes(android.security.keystore.KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(android.security.keystore.KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build())
        return generator.generateKey()
    }
}
