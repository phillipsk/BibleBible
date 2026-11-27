package data.search

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class SearchDirectReferenceDto(
    @SerialName("bookId") val bookId: String = "",
    @SerialName("bookNumber") val bookNumber: Int = 1,
    @SerialName("bookName") val bookName: String = "",
    @SerialName("chapter") val chapter: Int = 1,
    @SerialName("verseStart") val verseStart: Int = 1,
    @SerialName("verseEnd") val verseEnd: Int = 1,
    @SerialName("reference") val reference: String = "",
    @SerialName("excerpt") val excerpt: String = "",
)

@Serializable
internal data class SearchBookHitDto(
    @SerialName("bookId") val bookId: String = "",
    @SerialName("bookNumber") val bookNumber: Int = 1,
    @SerialName("bookName") val bookName: String = "",
    @SerialName("chapters") val chapters: Int = 1,
)

@Serializable
internal data class SearchMasonryCardDto(
    @SerialName("id") val id: String = "",
    @SerialName("size") val size: String = "compact",
    @SerialName("kind") val kind: String = "topic",
    @SerialName("tag") val tag: String = "",
    @SerialName("title") val title: String = "",
    @SerialName("reference") val reference: String = "",
    @SerialName("excerpt") val excerpt: String = "",
    @SerialName("bookId") val bookId: String = "",
    @SerialName("bookNumber") val bookNumber: Int = 1,
    @SerialName("chapter") val chapter: Int = 1,
    @SerialName("verseStart") val verseStart: Int = 1,
    @SerialName("verseEnd") val verseEnd: Int = 1,
    @SerialName("accentHex") val accentHex: String = "#24426B",
    @SerialName("planId") val planId: String? = null,
)

@Serializable
internal data class CloudSearchResponseDto(
    @SerialName("query") val query: String = "",
    @SerialName("version") val version: String = "KJV",
    @SerialName("aiSource") val aiSource: String = "deterministic",
    @SerialName("mappedTopics") val mappedTopics: List<String> = emptyList(),
    @SerialName("directReference") val directReference: SearchDirectReferenceDto? = null,
    @SerialName("books") val books: List<SearchBookHitDto> = emptyList(),
    @SerialName("masonryCards") val masonryCards: List<SearchMasonryCardDto> = emptyList(),
)

internal enum class SearchFilterTab(val label: String) {
    ALL("All"),
    BOOKS("Books"),
    TOPICS("Topics"),
    VERSES("Verses"),
}

internal data class SearchResultUiState(
    val query: String = "",
    val aiSource: String = "deterministic",
    val aiProviderLabel: String = "",
    val mappedTopics: List<String> = emptyList(),
    val directReference: SearchDirectReferenceDto? = null,
    val books: List<SearchBookHitDto> = emptyList(),
    val masonryCards: List<SearchMasonryCardDto> = emptyList(),
    val isLoadingCloud: Boolean = false,
)
