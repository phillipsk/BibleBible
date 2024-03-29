package data.gemini

import data.bibleIQ.BibleIQDataModel
import data.httpClientGemini
import email.kevinphillips.biblebible.BuildKonfig
import io.github.aakira.napier.Napier
import io.ktor.client.call.body
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
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
        Napier.d("API Response: $responseText", tag = "GeminiServiceImp")
        BibleIQDataModel.updateGeminiData(responseText)
    } catch (e: SocketTimeoutException) {
        Napier.e("Error during API request: ${e.message}", tag = "GeminiServiceImp")
        BibleIQDataModel.updateErrorSnackBar(e.message?.take(100) ?: "Could not connect to the network")
    } catch (e: Exception) {
        Napier.e("Error during API request: ${e.message}", tag = "GeminiServiceImp")
        BibleIQDataModel.updateErrorSnackBar(e.message ?: "Could not connect to the network")
    }
}