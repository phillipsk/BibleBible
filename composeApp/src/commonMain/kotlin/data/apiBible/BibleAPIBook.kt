package data.apiBible

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BibleAPIBook(
    val data: List<BookData>? = null
)

@Serializable
data class BookData(
    private val id: String? = null,
    val bibleId: String? = null,
    val abbreviation: String? = null,
    val name: String? = null,
    val nameLong: String? = null,
    private val chapters: List<Chapter>? = emptyList()
) {
    val bookId = chapters?.getOrNull(1)?.bookId ?: "GEN.1"
    val cleanedBibleId = bibleId ?: BibleAPIDataModel.DEFAULT_BIBLE_ID
    val remoteKey = "$bookId.1"
    val cleanedName = if ((name?.length ?: 0) >= 12
        && name?.getOrNull(0)?.isDigit() == true
    ) name.take(7) else name
    val chapterListBookData = chapters?.drop(1) // drop the first intro chapter
}

@Serializable
data class Chapter(
    @SerialName("id") private val name: String? = null,
    val bibleId: String? = null,
    val bookId: String? = null,
    val number: String? = null,
    val position: Int? = null
)
