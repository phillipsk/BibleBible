package data.plans

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class ReadingPlanVerseRefDto(
    @SerialName("id") val id: String = "",
    @SerialName("bookId") val bookId: String = "",
    @SerialName("bookNumber") val bookNumber: Int = 1,
    @SerialName("bookName") val bookName: String = "",
    @SerialName("chapter") val chapter: Int = 1,
    @SerialName("verseStart") val verseStart: Int? = null,
    @SerialName("verseEnd") val verseEnd: Int? = null,
    @SerialName("label") val label: String = "",
)

@Serializable
internal data class ReadingPlanDayDto(
    @SerialName("dayNumber") val dayNumber: Int = 1,
    @SerialName("title") val title: String = "",
    @SerialName("reflectionPrompt") val reflectionPrompt: String = "",
    @SerialName("readings") val readings: List<ReadingPlanVerseRefDto> = emptyList(),
)

@Serializable
internal data class ReadingPlanDto(
    @SerialName("id") val id: String = "",
    @SerialName("title") val title: String = "",
    @SerialName("subtitle") val subtitle: String = "",
    @SerialName("category") val category: String = "Theme",
    @SerialName("durationDays") val durationDays: Int = 1,
    @SerialName("sourceAttribution") val sourceAttribution: String = "",
    @SerialName("accentHex") val accentHex: String = "#2E6F73",
    @SerialName("days") val days: List<ReadingPlanDayDto> = emptyList(),
)

@Serializable
internal data class ReadingPlansCatalogResponseDto(
    @SerialName("catalogVersion") val catalogVersion: Int = 1,
    @SerialName("plans") val plans: List<ReadingPlanDto> = emptyList(),
)

internal data class ActivePlanReadingContext(
    val planId: String,
    val planTitle: String,
    val dayNumber: Int,
    val dayTitle: String,
    val reading: ReadingPlanVerseRefDto,
    val reflectionPrompt: String,
    val totalReadingsInPlan: Int,
)

internal data class PlanProgressSummary(
    val plan: ReadingPlanDto,
    val completedReadingIds: Set<String>,
    val completedCount: Int,
    val totalCount: Int,
    val percentComplete: Int,
    val nextReadingDay: ReadingPlanDayDto?,
    val nextReading: ReadingPlanVerseRefDto?,
)
