package data.gemini

import kotlinx.serialization.Serializable

@Serializable
data class GeminiResponseDto(
    val candidates: List<CandidateDto>? = null,
)

@Serializable
data class CandidateDto(
    val content: ContentDto? = null
)

@Serializable
data class ContentDto(
    val parts: List<PartDto>? = null,
    val role: String? = null
)

@Serializable
data class PartDto(
    val text: String? = null
)