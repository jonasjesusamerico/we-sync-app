package com.github.jonasjesusamerico.we_sync_app.auth

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.core.content.edit
import java.io.IOException
import java.security.GeneralSecurityException
import java.security.KeyStore
import java.security.ProviderException
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * [TokenStore] that encrypts the refresh token with an AES-256-GCM key held in the Android Keystore
 * (non-exportable) and keeps only the ciphertext in a private prefs file excluded from backups
 * (`res/xml/backup_rules.xml`, `data_extraction_rules.xml`).
 *
 * Any decryption failure (key invalidated, data restored on another device) is treated as "no session".
 * Must be called off the main thread: it touches disk and the Keystore.
 */
class KeystoreTokenStore(context: Context) : TokenStore {
    private val appContext = context.applicationContext
    private val prefs by lazy { appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }

    @Synchronized
    override fun readRefreshToken(): String? {
        val encoded = prefs.getString(KEY_REFRESH_TOKEN, null) ?: return null
        return try {
            val payload = Base64.decode(encoded, Base64.NO_WRAP)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(TAG_BITS, payload, 0, IV_BYTES))
            String(cipher.doFinal(payload, IV_BYTES, payload.size - IV_BYTES), Charsets.UTF_8)
        } catch (e: Exception) {
            if (!e.isKeystoreFailure() && e !is IllegalArgumentException) throw e
            clear()
            null
        }
    }

    @Synchronized
    override fun saveRefreshToken(token: String) {
        val payload = try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, key())
            cipher.iv + cipher.doFinal(token.toByteArray(Charsets.UTF_8))
        } catch (e: Exception) {
            if (!e.isKeystoreFailure()) throw e
            throw TokenStoreException(e)
        }
        // commit, not apply: the API already revoked the previous token, so the new one must be on disk.
        prefs.edit(commit = true) { putString(KEY_REFRESH_TOKEN, Base64.encodeToString(payload, Base64.NO_WRAP)) }
    }

    @Synchronized
    override fun clear() {
        prefs.edit(commit = true) { remove(KEY_REFRESH_TOKEN) }
    }

    private fun key(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )
        return generator.generateKey()
    }

    // Keystore implementations vary by device and may also throw ProviderException or IOException.
    private fun Exception.isKeystoreFailure() =
        this is GeneralSecurityException || this is ProviderException || this is IOException

    private companion object {
        // Also referenced by the backup exclusion rules.
        const val PREFS_NAME = "auth_session"
        const val KEY_REFRESH_TOKEN = "refresh_token"
        const val KEY_ALIAS = "we_sync_auth_session"
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val IV_BYTES = 12
        const val TAG_BITS = 128
    }
}
