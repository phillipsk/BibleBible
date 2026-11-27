package data.bible

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class BibleVersions(
    @SerialName("data") val data: List<BibleVersion> = listOf(),
)

@Serializable
internal data class BibleVersion(
    @SerialName("table") val table: String? = null,
    @SerialName("version_id") val versionId: String? = null,
    @SerialName("abbreviation") val abbreviation: String? = null,
    @SerialName("version") val version: String? = null,
    @SerialName("language") val language: String? = null,
) {
    val uiAbbreviation = when (abbreviation) {
        "KJV" -> "KJV"
        "BSB" -> "BSB"
        "WEB" -> "WEB"
        "BBE" -> "BBE"
        "RV1909" -> "Spanish: RV1909"
        "SVD" -> "Arabic: SVD"
        else -> abbreviation ?: "KJV"
    }
}

@Serializable
internal data class BibleBooksResponse(
    @SerialName("data") val data: List<BookData>? = null,
)

@Serializable
internal data class BookData(
    @SerialName("id") private val id: String? = null,
    @SerialName("bibleId") val bibleId: String? = null,
    @SerialName("abbreviation") val abbreviation: String? = null,
    @SerialName("name") val name: String? = null,
    @SerialName("nameLong") val nameLong: String? = null,
    @SerialName("chapters") private val chapters: List<Chapter>? = emptyList(),
) {
    val bookId = chapters?.getOrNull(1)?.bookId ?: id ?: "GEN.1"
    val remoteKey = "$bookId.1"
    val cleanedName = if ((name?.length ?: 0) >= 12 &&
        name?.getOrNull(0)?.isDigit() == true
    ) {
        name.take(7)
    } else {
        name
    }
    val chapterListBookData = chapters?.drop(1) // drop the first intro chapter
}

@Serializable
internal data class Chapter(
    @SerialName("id") private val name: String? = null,
    @SerialName("bibleId") val bibleId: String? = null,
    @SerialName("bookId") val bookId: String? = null,
    @SerialName("number") val number: String? = null,
    @SerialName("position") val position: Int? = null,
)

@Serializable
internal data class BibleChapter(
    @SerialName("id") val id: String? = null,
    @SerialName("b") val b: String? = null,
    @SerialName("c") val c: String? = null,
    @SerialName("v") val v: String? = null,
    @SerialName("t") val t: String? = null,
    @SerialName("p") val p: Boolean = false,
    @SerialName("r") val r: List<List<Int>> = emptyList(),
)

@Serializable
internal data class ChapterCount(@SerialName("chapterCount") val chapterCount: Long? = null)

internal data class BibleChapterUIState(
    val id: String? = null,
    val bookId: Int? = null,
    val chapterId: Int? = 1,
    val text: String? = null,
    val chapterList: List<Long>? = null,
    val version: String? = null,
    val verses: List<BibleChapter> = emptyList(),
)

internal data class ReadingHistoryUIState(
    val bookName: String? = null,
    val chapterId: Int? = null,
    val date: String? = null,
    val bookOrdinal: Int? = null,
) {
    override fun toString(): String {
        return "$date   $bookName, Chapter: $chapterId"
    }
}

internal val bibleBooksFull = listOf(
    "Genesis", "Exodus", "Leviticus", "Numbers", "Deuteronomy", "Joshua", "Judges", "Ruth",
    "1 Samuel", "2 Samuel", "1 Kings", "2 Kings", "1 Chronicles", "2 Chronicles", "Ezra",
    "Nehemiah", "Esther", "Job", "Psalms", "Proverbs", "Ecclesiastes", "Song of Solomon",
    "Isaiah", "Jeremiah", "Lamentations", "Ezekiel", "Daniel", "Hosea", "Joel", "Amos",
    "Obadiah", "Jonah", "Micah", "Nahum", "Habakkuk", "Zephaniah", "Haggai", "Zechariah",
    "Malachi", "Matthew", "Mark", "Luke", "John", "Acts", "Romans", "1 Corinthians",
    "2 Corinthians", "Galatians", "Ephesians", "Philippians", "Colossians", "1 Thessalonians",
    "2 Thessalonians", "1 Timothy", "2 Timothy", "Titus", "Philemon", "Hebrews", "James",
    "1 Peter", "2 Peter", "1 John", "2 John", "3 John", "Jude", "Revelation",
)
