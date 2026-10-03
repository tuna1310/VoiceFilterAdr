package com.antigravity.noichuan.data.remote

import com.antigravity.noichuan.data.model.Word
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.net.HttpURLConnection
import java.net.URL

interface RemoteContentDataSource {
    suspend fun fetchRemoteVersion(): Int?
    suspend fun fetchRemoteWords(): List<Word>?
    suspend fun fetchRemoteTongueTwisters(): List<Word>?
}

class SupabaseRemoteContentDataSource(
    private val supabaseUrl: String = "",
    private val supabaseAnonKey: String = "",
    private val json: Json = Json { ignoreUnknownKeys = true }
) : RemoteContentDataSource {

    companion object {
        private const val TIMEOUT_MS = 5000
    }

    override suspend fun fetchRemoteVersion(): Int? = withContext(Dispatchers.IO) {
        if (!isConfigured()) return@withContext null

        try {
            val endpoint = "${supabaseUrl.trimEnd('/')}/rest/v1/content_versions?key=eq.content&select=version&limit=1"
            val response = executeGetRequest(endpoint) ?: return@withContext null
            val element = json.parseToJsonElement(response)
            val array = element.jsonArray
            if (array.isNotEmpty()) {
                array[0].jsonObject["version"]?.jsonPrimitive?.content?.toIntOrNull()
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun fetchRemoteWords(): List<Word>? = withContext(Dispatchers.IO) {
        if (!isConfigured()) return@withContext null

        try {
            val endpoint = "${supabaseUrl.trimEnd('/')}/rest/v1/words?select=*"
            val response = executeGetRequest(endpoint) ?: return@withContext null
            json.decodeFromString<List<Word>>(response)
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun fetchRemoteTongueTwisters(): List<Word>? = withContext(Dispatchers.IO) {
        if (!isConfigured()) return@withContext null

        try {
            val endpoint = "${supabaseUrl.trimEnd('/')}/rest/v1/tongue_twisters?select=*"
            val response = executeGetRequest(endpoint) ?: return@withContext null
            json.decodeFromString<List<Word>>(response)
        } catch (e: Exception) {
            null
        }
    }

    private fun isConfigured(): Boolean {
        return supabaseUrl.isNotBlank() &&
                supabaseAnonKey.isNotBlank() &&
                !supabaseUrl.contains("your-project") &&
                supabaseUrl.startsWith("http")
    }

    private fun executeGetRequest(endpoint: String): String? {
        val conn = (URL(endpoint).openConnection() as? HttpURLConnection) ?: return null
        return try {
            conn.connectTimeout = TIMEOUT_MS
            conn.readTimeout = TIMEOUT_MS
            conn.requestMethod = "GET"
            conn.setRequestProperty("apikey", supabaseAnonKey)
            conn.setRequestProperty("Authorization", "Bearer $supabaseAnonKey")
            conn.setRequestProperty("Accept", "application/json")

            val code = conn.responseCode
            if (code in 200..299) {
                conn.inputStream.bufferedReader().use { it.readText() }
            } else {
                null
            }
        } catch (e: Exception) {
            null
        } finally {
            conn.disconnect()
        }
    }
}
