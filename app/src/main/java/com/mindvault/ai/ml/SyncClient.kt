package com.mindvault.ai.ml

import android.util.Base64
import com.mindvault.ai.AppContextHolder
import com.mindvault.ai.crypto.CryptoHelper
import com.mindvault.ai.crypto.KeyStoreManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.*

data class RestoredEntry(
    val entryId: String,
    val createdAt: Long,
    val updatedAt: Long,
    val transcript: String,
    val summary: String,
    val mood: String
)

object SyncClient {
    private val client = OkHttpClient()
    private const val AAD_CONST = "journal-v1"

    suspend fun register(serverBaseUrl: String, email: String, password: String): String? = withContext(Dispatchers.IO) {
        try {
            val url = "${serverBaseUrl.trimEnd('/')}/register"
            val json = JSONObject().apply { put("email", email); put("password", password) }
            val req = Request.Builder().url(url)
                .post(json.toString().toRequestBody("application/json".toMediaType()))
                .build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext null
                val token = JSONObject(resp.body?.string() ?: return@withContext null).optString("access_token", null)
                token
            }
        } catch (e: Exception) { e.printStackTrace(); null }
    }

    suspend fun login(serverBaseUrl: String, email: String, password: String): String? = withContext(Dispatchers.IO) {
        try {
            val url = "${serverBaseUrl.trimEnd('/')}/login"
            val json = JSONObject().apply { put("email", email); put("password", password) }
            val req = Request.Builder().url(url)
                .post(json.toString().toRequestBody("application/json".toMediaType()))
                .build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext null
                val token = JSONObject(resp.body?.string() ?: return@withContext null).optString("access_token", null)
                token
            }
        } catch (e: Exception) { e.printStackTrace(); null }
    }

    /**
     * Encrypt transcript/summary/mood and POST /entry using stored token if available.
     * Returns true on success.
     */
    suspend fun syncEncryptedEntry(
        serverBaseUrl: String,
        transcript: String,
        summary: String,
        mood: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val ctx = AppContextHolder.appContext
            val token = TokenStore.token(ctx) ?: return@withContext false

            // Ensure data key exists
            var dataKey = KeyStoreManager.loadDataKey(ctx)
            if (dataKey == null) {
                dataKey = CryptoHelper.generateDataKey()
                KeyStoreManager.storeDataKey(ctx, dataKey)
            }

            // Build minimal JSON payload and encrypt
            val payloadJson = JSONObject().apply {
                put("t", transcript)
                put("s", summary)
                put("m", mood)
            }.toString().toByteArray()

            val aad = AAD_CONST.toByteArray()
            val ivCt = CryptoHelper.encrypt(dataKey, payloadJson, aad)
            val blobB64 = CryptoHelper.toB64(ivCt)
            val size = ivCt.size
            val now = System.currentTimeMillis()
            val entryId = UUID.randomUUID().toString()

            val bodyJson = JSONObject().apply {
                put("entry_id", entryId)
                put("created_at", now)
                put("updated_at", now)
                put("size", size)
                put("blob_b64", blobB64)
            }

            val url = "${serverBaseUrl.trimEnd('/')}/entry"
            val req = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $token")
                .post(bodyJson.toString().toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(req).execute().use { resp ->
                val ok = resp.isSuccessful
                CryptoHelper.wipe(dataKey)
                ok
            }
        } catch (e: Exception) {
            e.printStackTrace(); false
        }
    }

    /**
     * GET /entries?since=ms, decrypt each blob, return restored entries list.
     */
    suspend fun restoreEntries(
        serverBaseUrl: String,
        sinceMs: Long = 0L
    ): List<RestoredEntry> = withContext(Dispatchers.IO) {
        val ctx = AppContextHolder.appContext
        val token = TokenStore.token(ctx) ?: return@withContext emptyList()

        val dataKey = KeyStoreManager.loadDataKey(ctx) ?: return@withContext emptyList()
        val aad = AAD_CONST.toByteArray()

        try {
            val url = "${serverBaseUrl.trimEnd('/')}/entries?since=$sinceMs"
            val req = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $token")
                .get()
                .build()

            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext emptyList<RestoredEntry>()
                val body = resp.body?.string() ?: return@withContext emptyList<RestoredEntry>()
                val items = JSONObject(body).optJSONArray("items") ?: JSONArray()
                val out = ArrayList<RestoredEntry>(items.length())
                for (i in 0 until items.length()) {
                    val it = items.getJSONObject(i)
                    val entryId = it.getString("entry_id")
                    val createdAt = it.getLong("created_at")
                    val updatedAt = it.getLong("updated_at")
                    val blobB64 = it.getString("blob_b64")
                    val ivCt = Base64.decode(blobB64, Base64.NO_WRAP)
                    val plain = CryptoHelper.decrypt(dataKey, ivCt, aad)
                    val jo = JSONObject(String(plain, Charsets.UTF_8))
                    val transcript = jo.optString("t","")
                    val summary = jo.optString("s","")
                    val mood = jo.optString("m","")
                    out.add(RestoredEntry(entryId, createdAt, updatedAt, transcript, summary, mood))
                }
                out
            }
        } catch (e: Exception) {
            e.printStackTrace(); emptyList()
        } finally {
            CryptoHelper.wipe(dataKey)
        }
    }
}
