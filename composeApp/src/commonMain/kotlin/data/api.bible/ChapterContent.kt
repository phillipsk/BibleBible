package data.api.bible

import kotlinx.serialization.Serializable

@Serializable
data class ChapterContent(
    val data: ChapterData? = null,
    val meta: MetaData? = null
)

@Serializable
data class ChapterData(
    val id: String? = null,
    val bibleId: String? = null,
    val number: String? = null,
    val bookId: String? = null,
    val reference: String? = null,
    val copyright: String? = null,
    val verseCount: Int? = null,
    private val content: String? = null,
    val next: ChapterReference? = null,
    val previous: ChapterReference? = null
) {
    val cleanedContent = "Chapter ${content?.replace("¶", "")}"
}

@Serializable
data class ChapterReference(
    val id: String? = null,
    val number: String? = null,
    val bookId: String? = null
)

@Serializable
data class MetaData(
    val fums: String? = null,
    val fumsId: String? = null,
    val fumsJsInclude: String? = null,
    val fumsJs: String? = null,
    val fumsNoScript: String? = null
)
