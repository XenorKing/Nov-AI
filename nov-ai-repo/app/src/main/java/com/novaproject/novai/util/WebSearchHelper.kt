package com.novaproject.novai.util

import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton

class SearchRateLimitException : Exception("В данный момент поиск по сайтам недоступен. Попробуй позже.")

@Singleton
class WebSearchHelper @Inject constructor(
    private val client: OkHttpClient,
    private val gson: Gson
) {
    private companion object {
        private const val SERPER_KEY = "683de9e1fe0aff106790b2b6e55a8fc9de6a36a2"
    }

    suspend fun search(query: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            Result.success(searchWithSerper(query))
        } catch (e: SearchRateLimitException) {
            Result.failure(e)
        } catch (_: Exception) {
            try {
                Result.success(searchWithDuckDuckGo(query))
            } catch (_: Exception) {
                Result.success("")
            }
        }
    }

    private fun searchWithSerper(query: String): String {
        val escaped = query.replace("\"", "\\\"")
        val body = """{"q":"$escaped","num":5}""".toRequestBody("application/json".toMediaType())
        val req = Request.Builder()
            .url("https://google.serper.dev/search")
            .post(body)
            .addHeader("X-API-KEY", SERPER_KEY)
            .addHeader("Content-Type", "application/json")
            .build()
        val response = client.newCall(req).execute()
        val code = response.code
        val json = response.body?.string() ?: ""
        response.close()
        if (code == 429) throw SearchRateLimitException()
        if (code !in 200..299) return ""
        @Suppress("UNCHECKED_CAST")
        val data = runCatching { gson.fromJson(json, Map::class.java) }.getOrNull() ?: return ""
        val organic = (data["organic"] as? List<*>)?.filterIsInstance<Map<*, *>>() ?: return ""
        return organic.take(4).mapIndexed { i, result ->
            val title = result["title"] as? String ?: ""
            val snippet = result["snippet"] as? String ?: ""
            "${i + 1}. $title\n   $snippet"
        }.joinToString("\n\n")
    }

    private fun searchWithDuckDuckGo(query: String): String {
        val encoded = URLEncoder.encode(query, "UTF-8")
        val req = Request.Builder()
            .url("https://api.duckduckgo.com/?q=$encoded&format=json&no_html=1&skip_disambig=1")
            .get()
            .addHeader("User-Agent", "NovAI/1.0")
            .build()
        val response = client.newCall(req).execute()
        val json = response.body?.string() ?: return ""
        response.close()
        val data = runCatching { gson.fromJson(json, Map::class.java) }.getOrNull() ?: return ""
        val abstract = (data["AbstractText"] as? String)?.takeIf { it.isNotBlank() }
        @Suppress("UNCHECKED_CAST")
        val related = (data["RelatedTopics"] as? List<*>)?.filterIsInstance<Map<*, *>>()
        val topics = related?.take(3)?.mapNotNull { it["Text"] as? String }?.joinToString("\n") ?: ""
        return abstract ?: topics
    }
}
