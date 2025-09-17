package com.mindvault.ai.ml

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

object SettingsStore {
    private const val PREFS = "settings.prefs"
    private const val KEY_SERVER = "server"
    private const val KEY_EMAIL = "email"
    private const val KEY_PASSWORD = "password"

    private fun prefs(ctx: Context) = EncryptedSharedPreferences.create(
        ctx,
        PREFS,
        MasterKey.Builder(ctx).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun save(ctx: Context, server: String, email: String, password: String) {
        prefs(ctx).edit()
            .putString(KEY_SERVER, server)
            .putString(KEY_EMAIL, email)
            .putString(KEY_PASSWORD, password)
            .apply()
    }

    fun server(ctx: Context): String? = prefs(ctx).getString(KEY_SERVER, null)
    fun email(ctx: Context): String? = prefs(ctx).getString(KEY_EMAIL, null)
    fun password(ctx: Context): String? = prefs(ctx).getString(KEY_PASSWORD, null)
}
