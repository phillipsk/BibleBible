package data.gemini

import data.GeminiModel
import data.httpClientGemini
import email.kevinphillips.biblebible.BuildKonfig
import email.kevinphillips.biblebible.cache.DriverFactory
import email.kevinphillips.biblebible.db.BibleBibleDatabase
import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json


/**
 * The latest high-speed Flash model. 
 * As of May 2026, Gemini 3.5 Flash is the state-of-the-art efficiency model.
 */
const val GEMINI_MODEL = "gemini-3.5-flash"

suspend fun generateContent(content: String, client: HttpClient = httpClientGemini) {
    val parts = mutableListOf<RequestPart>()
    parts.add(RequestPart(text = content))
    val requestBody = RequestBody(contents = listOf(ContentItem(parts = parts)))
    Napier.d("Request Body: $requestBody", tag = "GeminiServiceImp")

    try {
        val responseText: GeminiResponseDto
        withContext(Dispatchers.IO) {
            responseText = client.post {
                url("v1beta/models/$GEMINI_MODEL:generateContent")
                parameter("key", BuildKonfig.GEMINI_API_KEY)
                setBody(Json.encodeToString(requestBody))
            }.body<GeminiResponseDto>()
        }
        GeminiModel.updateGeminiData(responseText)
    } catch (e: Exception) {
        Napier.e("Error during API request: ${e.message}", tag = "GeminiServiceImp")
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