package data.api.bible

import kotlinx.serialization.Serializable

@Serializable
data class BibleAPIBook(
    val data: List<BookData>? = emptyList()
)

@Serializable
data class BookData(
    private val id: String? = null,
    val bibleId: String? = null,
    val abbreviation: String? = null,
    val name: String? = null,
    val nameLong: String? = null,
    val chapters: List<Chapter>? = emptyList()
) {
    val bookId = id ?: "GEN"
}

@Serializable
data class Chapter(
    private val id: String? = null,
    val bibleId: String? = null,
    val bookId: String? = null,
    val number: String? = null,
    val position: Int? = null
) {
    val chapterId = id?.toInt() ?: -1
}

