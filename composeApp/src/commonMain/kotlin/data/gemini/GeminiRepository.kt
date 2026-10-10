package data.gemini

import data.httpClientGemini
import email.kevinphillips.biblebible.cache.DriverFactory
import email.kevinphillips.biblebible.db.BibleBibleDatabase
import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.http.contentType
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.http.ContentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock


/** Cloud model used for chapter summaries. This path does not use on-device Gemini Nano. */
const val GEMINI_MODEL = "gemini-3.5-flash"

internal class GeminiRequestException(message: String) : Exception(message)

suspend fun generateContent(
    content: String,
    client: HttpClient = httpClientGemini,
): GeminiResponseDto {
    val response = client.post {
        url("https://generativelanguage.googleapis.com/v1beta/models/$GEMINI_MODEL:generateContent")
        contentType(ContentType.Application.Json)
        setBody(RequestBody(contents = listOf(ContentItem(parts = listOf(RequestPart(text = content))))))
    }
    if (!response.status.isSuccess()) {
        // Do not log provider bodies or exception messages that might contain credentials.
        throw GeminiRequestException("AI summary request failed (HTTP ${response.status.value}).")
    }
    return response.body<GeminiResponseDto>().also {
        if (it.summaryText == null) throw GeminiRequestException("AI summary response was empty.")
    }
}

suspend fun checkAnimationLastCalled(): Boolean {
    var showAnimation = false
    try {
        withContext(Dispatchers.IO) {
            DriverFactory.createDriver()?.let { BibleBibleDatabase(driver = it) }?.let { database ->
                val lastCalledQuery = database.bibleBibleDatabaseQueries.getLastCalled()
                val lastCalledTime = lastCalledQuery.executeAsOneOrNull()?.time ?: 0
                val currentTime = Clock.System.now().toEpochMilliseconds()

                // Check if animation was called more than a day ago
                showAnimation = currentTime - lastCalledTime > 24 * 60 * 60 * 1000
                if (showAnimation) {
                    database.bibleBibleDatabaseQueries.insertLastCalled(time = currentTime)
                }
            }
        }
    } catch (e: Exception) {
        Napier.e("Error in checkAnimationLastCalled: ${e.message}", tag = "HomeTopBar")
    }
    return showAnimation
}
