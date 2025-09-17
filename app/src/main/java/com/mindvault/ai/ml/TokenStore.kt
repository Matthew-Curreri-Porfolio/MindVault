package com.mindvault.ai.ml

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

object TokenStore {
    private const val PREFS = "auth.prefs"
    private const val KEY_TOKEN = "jwt_token"
    private const val KEY_EMAIL = "email"

    private fun prefs(ctx: Context) = EncryptedSharedPreferences.create(
        ctx,
        PREFS,
        MasterKey.Builder(ctx).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun save(ctx: Context, email: String, token: String) {
        prefs(ctx).edit().putString(KEY_EMAIL, email).putString(KEY_TOKEN, token).apply()
    }

    fun token(ctx: Context): String? = prefs(ctx).getString(KEY_TOKEN, null)
    fun email(ctx: Context): String? = prefs(ctx).getString(KEY_EMAIL, null)
    fun clear(ctx: Context) { prefs(ctx).edit().clear().apply() }
}
