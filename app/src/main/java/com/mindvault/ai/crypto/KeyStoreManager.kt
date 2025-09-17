package com.mindvault.ai.crypto

import android.content.Context
import android.os.Build
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import android.util.Base64

/**
 * Stores the E2E data key wrapped (encrypted) with an Android Keystore master key.
 * Also supports export/import of the data key using a password-derived key (PBKDF2).
 */
object KeyStoreManager {
    private const val PREFS = "keystore.prefs"
    private const val KEY_BLOB = "wrapped_data_key"
    private const val SALT_KEY = "pw_salt"
    private const val PW_WRAP = "pw_wrapped_data_key"
    private const val KDF_ITERS = 200_000
    private const val KDF_KEYLEN_BITS = 256

    private fun masterKey(context: Context): MasterKey {
        // AES256_GCM inside TEE when available
        return MasterKey.Builder(context, MasterKey.DEFAULT_MASTER_KEY_ALIAS)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }

    private fun securePrefs(context: Context) =
        EncryptedSharedPreferences.create(
            context,
            PREFS,
            masterKey(context),
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

    /** Persist the raw 32-byte data key, wrapped by the Keystore master key via EncryptedSharedPreferences. */
    fun storeDataKey(context: Context, dataKey: ByteArray) {
        val p = securePrefs(context).edit()
        p.putString(KEY_BLOB, Base64.encodeToString(dataKey, Base64.NO_WRAP))
        p.apply()
    }

    /** Load the raw 32-byte data key if present, else null. */
    fun loadDataKey(context: Context): ByteArray? {
        val b64 = securePrefs(context).getString(KEY_BLOB, null) ?: return null
        return Base64.decode(b64, Base64.NO_WRAP)
    }

    /** Delete stored key. */
    fun clearDataKey(context: Context) {
        securePrefs(context).edit().remove(KEY_BLOB).apply()
    }

    // -------- Password escrow (for multi-device restore) --------

    /** Export: wrap current data key with a password-derived key (PBKDF2) and store salt + wrapped in EncryptedSharedPreferences. */
    fun exportWithPassword(context: Context, password: CharArray): Boolean {
        val dk = loadDataKey(context) ?: return false
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val wrapKey = deriveKey(password, salt)
        val wrapped = xorWrap(dk, wrapKey) // simple one-time pad style wrap; payload still AES-GCM encrypted at rest for entries
        securePrefs(context).edit()
            .putString(SALT_KEY, Base64.encodeToString(salt, Base64.NO_WRAP))
            .putString(PW_WRAP, Base64.encodeToString(wrapped, Base64.NO_WRAP))
            .apply()
        dk.fill(0)
        wrapKey.fill(0)
        password.fill('\u0000')
        return true
    }

    /** Import: recover data key from stored password wrap. */
    fun importWithPassword(context: Context, password: CharArray): Boolean {
        val prefs = securePrefs(context)
        val saltB64 = prefs.getString(SALT_KEY, null) ?: return false
        val wrapB64 = prefs.getString(PW_WRAP, null) ?: return false
        val salt = Base64.decode(saltB64, Base64.NO_WRAP)
        val wrapped = Base64.decode(wrapB64, Base64.NO_WRAP)
        val wrapKey = deriveKey(password, salt)
        val dk = xorUnwrap(wrapped, wrapKey)
        storeDataKey(context, dk)
        dk.fill(0); wrapKey.fill(0); password.fill('\u0000')
        return true
    }

    private fun deriveKey(password: CharArray, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(password, salt, KDF_ITERS, KDF_KEYLEN_BITS)
        val skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        return skf.generateSecret(spec).encoded
    }

    private fun xorWrap(data: ByteArray, key: ByteArray): ByteArray {
        val out = ByteArray(data.size)
        for (i in data.indices) out[i] = (data[i].toInt() xor key[i % key.size].toInt()).toByte()
        return out
    }
    private fun xorUnwrap(wrapped: ByteArray, key: ByteArray): ByteArray = xorWrap(wrapped, key)
}
