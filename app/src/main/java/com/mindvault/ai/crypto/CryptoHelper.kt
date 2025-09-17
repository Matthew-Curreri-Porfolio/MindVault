package com.mindvault.ai.crypto

import android.content.Context
import android.util.Base64
import okio.ByteString
import okio.ByteString.Companion.of
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * CryptoHelper handles E2E encryption for sync payloads.
 *
 * - A single 256-bit "data key" encrypts/decrypts entry JSON/audio blobs with AES-GCM.
 * - The data key itself is stored wrapped (encrypted) by Android Keystore via KeyStoreManager.
 * - You can also export/import the data key wrapped with a password for multi-device restore.
 */
object CryptoHelper {
    private const val AES = "AES"
    private const val AES_GCM = "AES/GCM/NoPadding"
    private const val GCM_TAG_BITS = 128
    private const val IV_BYTES = 12
    private val rng = SecureRandom()

    /** Generate a fresh random 256-bit AES key (plaintext in memory). */
    fun generateDataKey(): ByteArray {
        val k = ByteArray(32)
        rng.nextBytes(k)
        return k
    }

    /** Encrypt bytes with AES-GCM using dataKey. Returns iv || ciphertext (both raw). */
    fun encrypt(dataKey: ByteArray, plaintext: ByteArray, aad: ByteArray? = null): ByteArray {
        val iv = ByteArray(IV_BYTES).also { rng.nextBytes(it) }
        val sk: SecretKey = SecretKeySpec(dataKey, AES)
        val cipher = Cipher.getInstance(AES_GCM)
        cipher.init(Cipher.ENCRYPT_MODE, sk, GCMParameterSpec(GCM_TAG_BITS, iv))
        if (aad != null) cipher.updateAAD(aad)
        val ct = cipher.doFinal(plaintext)
        return iv + ct
    }

    /** Decrypt bytes produced by [encrypt]. Input must be iv || ciphertext. */
    fun decrypt(dataKey: ByteArray, ivPlusCiphertext: ByteArray, aad: ByteArray? = null): ByteArray {
        require(ivPlusCiphertext.size > IV_BYTES) { "ciphertext too short" }
        val iv = ivPlusCiphertext.copyOfRange(0, IV_BYTES)
        val ct = ivPlusCiphertext.copyOfRange(IV_BYTES, ivPlusCiphertext.size)
        val sk: SecretKey = SecretKeySpec(dataKey, AES)
        val cipher = Cipher.getInstance(AES_GCM)
        cipher.init(Cipher.DECRYPT_MODE, sk, GCMParameterSpec(GCM_TAG_BITS, iv))
        if (aad != null) cipher.updateAAD(aad)
        return cipher.doFinal(ct)
    }

    /** Convenience: base64 encode for transport. */
    fun toB64(raw: ByteArray): String = Base64.encodeToString(raw, Base64.NO_WRAP)
    fun fromB64(b64: String): ByteArray = Base64.decode(b64, Base64.NO_WRAP)

    /** Zero a byte array (best effort) */
    fun wipe(bytes: ByteArray) {
        for (i in bytes.indices) bytes[i] = 0
    }
}
