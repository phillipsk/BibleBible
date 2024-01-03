package data.bibleIQ

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BibleVersion(
    val table: String? = null,
    @SerialName("version_id") val versionId: String? = null,
    val abbreviation: String? = null,
    val version: String? = null,
    val language: String? = null
)

@Serializable
data class BibleBook(private val b: String? = null, @SerialName("n") val name: String? = null) {
    val bookId = b?.toInt()
}

@Serializable
data class Chapter(
    private val id: String,
    private val b: String,
    private val c: String,
    private val v: String,
    @SerialName("t") val verse: String
) {
    val chapterVerseUUID = id.toLong()
    val bookId = b.toInt()
    val chapterId = c.toInt()
    val verseId = v.toInt()
}