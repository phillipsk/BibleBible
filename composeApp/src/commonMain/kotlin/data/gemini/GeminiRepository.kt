package data.gemini

import data.GeminiModel
import data.httpClientGemini
import email.kevinphillips.biblebible.cache.DriverFactory
import email.kevinphillips.biblebible.db.BibleBibleDatabase
import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock


/** Cloud model used for chapter summaries. */
const val GEMINI_MODEL = "gemini-3.5-flash"

suspend fun generateContent(content: String, client: HttpClient = httpClientGemini) {
    val parts = mutableListOf<RequestPart>()
    parts.add(RequestPart(text = content))
    val requestBody = RequestBody(
        contents = listOf(ContentItem(parts = parts)),
        generationConfig = GenerationConfig(maxOutputTokens = 2048, candidateCount = 1),
    )
    GeminiModel.updateGeminiData(GeminiResponseDto())

    try {
        val response = withContext(Dispatchers.IO) {
            client.post {
                expectSuccess = false
                url("https://generativelanguage.googleapis.com/v1beta/models/$GEMINI_MODEL:generateContent")
                contentType(ContentType.Application.Json)
                setBody(requestBody)
            }
        }
        currentCoroutineContext().ensureActive()
        if (!response.status.isSuccess()) {
            Napier.e("Gemini request failed (HTTP ${response.status.value})", tag = "GeminiServiceImp")
            GeminiModel.updateGeminiError(when (response.status.value) {
                402 -> "AI summary unavailable: check Gemini prepaid credits in Google AI Studio."
                401, 403 -> "Gemini rejected the API key or its permissions. Check the key and API restrictions."
                429 -> "Gemini quota or rate limit reached. Check quota and billing, or try again later."
                else -> "Gemini could not load the summary (HTTP ${response.status.value}). Please try again later."
            })
            return
        }
        val responseText = response.body<GeminiResponseDto>()
        currentCoroutineContext().ensureActive()
        if (responseText.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text.isNullOrBlank()) {
            GeminiModel.updateGeminiError("Gemini returned no summary. Please try again.")
        } else {
            GeminiModel.updateGeminiData(responseText)
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        currentCoroutineContext().ensureActive()
        Napier.e("Gemini request failed (${e::class.simpleName})", tag = "GeminiServiceImp")
        GeminiModel.updateGeminiError("AI summary could not load. Check your connection and try again.")
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
