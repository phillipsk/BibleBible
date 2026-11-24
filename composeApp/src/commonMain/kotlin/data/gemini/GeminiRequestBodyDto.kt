package data.gemini

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

internal data class SummaryTarget(
    val bookId: String,
    val chapter: Int,
    val version: String = "KJV",
)

@Serializable
internal data class SummaryRequestDto(
    @SerialName("bookId") val bookId: String,
    @SerialName("chapter") val chapter: Int,
    @SerialName("version") val version: String = "KJV",
    @SerialName("model") val model: String? = null,
    @SerialName("refresh") val refresh: Boolean? = null,
)
