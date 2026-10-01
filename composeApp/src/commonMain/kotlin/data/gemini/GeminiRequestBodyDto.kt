package data.gemini

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ContentItem(val parts: List<RequestPart>)

@Serializable
data class RequestBody(
    val contents: List<ContentItem>,
    val generationConfig: GenerationConfig = GenerationConfig(),
)

@Serializable
data class GenerationConfig(
    val maxOutputTokens: Int = 2048,
    val candidateCount: Int = 1,
)

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
