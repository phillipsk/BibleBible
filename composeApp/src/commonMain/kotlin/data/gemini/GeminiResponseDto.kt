package data.gemini

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class GeminiResponseDto(
    @SerialName("candidates") val candidates: List<CandidateDto>? = null,
)

@Serializable
internal data class CandidateDto(
    @SerialName("content") val content: ContentDto? = null,
)

@Serializable
internal data class ContentDto(
    @SerialName("parts") val parts: List<PartDto>? = null,
    @SerialName("role") val role: String? = null,
)

@Serializable
internal data class PartDto(
    @SerialName("text") val text: String? = null,
)
