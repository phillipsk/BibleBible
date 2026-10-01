package data.gemini

import kotlinx.serialization.Serializable

@Serializable
data class GeminiResponseDto(
    val candidates: List<CandidateDto>? = null,
    val usageMetadata: UsageMetadataDto? = null,
) {
    val summaryText: String?
        get() = candidates?.firstOrNull()?.content?.parts
            ?.mapNotNull { it.text }?.joinToString("")?.takeIf { it.isNotBlank() }
}

@Serializable
data class UsageMetadataDto(
    val promptTokenCount: Int? = null,
    val candidatesTokenCount: Int? = null,
    val totalTokenCount: Int? = null,
    val thoughtsTokenCount: Int? = null,
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
