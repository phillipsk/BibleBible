package data.gemini

import data.GeminiModel
import data.httpClientGemini
import email.kevinphillips.biblebible.BuildKonfig
import email.kevinphillips.biblebible.cache.DriverFactory
import email.kevinphillips.biblebible.db.BibleBibleDatabase
import io.github.aakira.napier.Napier
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


const val GEMINI_PRO = "gemini-pro"
const val GEMINI_PRO_VISION = "gemini-pro-vision"
suspend fun generateContent(content: String) {
    val parts = mutableListOf<RequestPart>()
    parts.add(RequestPart(text = content))
    val requestBody = RequestBody(contents = listOf(ContentItem(parts = parts)))
    Napier.d("Request Body: $requestBody", tag = "GeminiServiceImp")

    try {
        val responseText: GeminiResponseDto
        withContext(Dispatchers.IO) {
            responseText = httpClientGemini.post {
                url("v1beta/models/$GEMINI_PRO:generateContent")
                parameter("key", BuildKonfig.GEMINI_API_KEY)
                setBody(Json.encodeToString(requestBody))
            }.body<GeminiResponseDto>()
        }
        GeminiModel.updateGeminiData(responseText)
    } catch (e: Exception) {
        Napier.e("Error during API request: ${e.message}", tag = "GeminiServiceImp")
    }
}

suspend fun checkAnimationLastCalled(): Boolean? {
    DriverFactory.createDriver()?.let { BibleBibleDatabase(driver = it) }?.let { database ->
        val lastCalledTime: Long
        val currentTime: Long
        val showAnimation: Boolean
        try {
            withContext(Dispatchers.IO) {
                val lastCalledQuery = database.bibleBibleDatabaseQueries.getLastCalled()
                lastCalledTime = lastCalledQuery.executeAsOneOrNull()?.time ?: 0
                currentTime = Clock.System.now().toEpochMilliseconds()

                // Check if animation was called more than a day ago
                showAnimation = currentTime - lastCalledTime > 24 * 60 * 60 * 1000
                if (showAnimation) {
                    database.bibleBibleDatabaseQueries.insertLastCalled(time = currentTime)
                }
            }
            withContext(Dispatchers.Main) {
                return@withContext showAnimation
            }

        } catch (e: Exception) {
            Napier.e("Error in checkAnimationLastCalled: ${e.message}", tag = "HomeTopBar")
        }

    }
    return null
}