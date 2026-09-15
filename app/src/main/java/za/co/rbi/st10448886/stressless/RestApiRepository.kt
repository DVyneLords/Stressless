package za.co.rbi.st10448886.stressless

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

private const val TAG = "RestApiRepository"
private const val QUOTE_API_URL = "https://api.quotable.io/random"

/**
 * RestApiRepository — calls a public REST API (quotable.io) over plain
 * HTTPS/JSON to fetch a motivational quote shown on the Dashboard.
 * Uses HttpURLConnection + org.json directly (no extra Gradle dependency).
 */
object RestApiRepository {
    suspend fun fetchDailyQuote(): Result<String> = withContext(Dispatchers.IO) {
        try {
            val connection = (URL(QUOTE_API_URL).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8000
                readTimeout = 8000
            }
            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val body = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(body)
                val content = json.optString("content", "")
                val author = json.optString("author", "")
                connection.disconnect()
                Log.d(TAG, "Fetched quote by $author")
                Result.success(if (author.isNotBlank()) "$content — $author" else content)
            } else {
                connection.disconnect()
                Result.failure(Exception("HTTP ${connection.responseCode}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "fetchDailyQuote failed", e)
            Result.failure(e)
        }
    }
}