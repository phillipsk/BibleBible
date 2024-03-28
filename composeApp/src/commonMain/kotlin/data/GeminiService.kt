package data

import io.ktor.client.call.body
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json


const val GEMINI_PRO = "gemini-pro"
const val GEMINI_PRO_VISION = "gemini-pro-vision"
suspend fun generateContent(content: String, apiKey: String): GeminiResponseDto {
    val parts = mutableListOf<RequestPart>()
    parts.add(RequestPart(text = content))
    val requestBody = RequestBody(contents = listOf(ContentItem(parts = parts)))

    return try {
        val responseText = httpClientGemini.post {
            url("v1beta/models/${GEMINI_PRO}:generateContent")
            parameter("key", apiKey)
            setBody(Json.encodeToString(requestBody))
        }.body<GeminiResponseDto>()
        println("API Response: $responseText")
        responseText
    } catch (e: Exception) {
        println("Error during API request: ${e.message}")
        throw e
    }
}

@Serializable
data class ContentItem(val parts: List<RequestPart>)

@Serializable
data class RequestBody(val contents: List<ContentItem>)

@Serializable
data class RequestPart(
    val text: String? = null,
    val inlineData: RequestInlineData? = null
)

@Serializable
data class RequestInlineData(
    @SerialName("mimeType") val mimeType: String,
    @SerialName("data") val data: String
)

@Serializable
data class GeminiResponseDto(
    val candidates: List<CandidateDto>,
)

@Serializable
data class CandidateDto(
    val content: ContentDto,
)

@Serializable
data class ContentDto(
    val parts: List<PartDto>,
    val role: String
)

@Serializable
data class PartDto(
    val text: String
)