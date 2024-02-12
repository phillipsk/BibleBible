package data.bibleIQ

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

val longBookNamesSet = setOf(13,14,46,47,52,53)


@Serializable
data class BibleIQVersions(
    val data: List<BibleIQVersion> = listOf(),
)
@Serializable
data class BibleIQVersion(
    val table: String? = null,
    @SerialName("version_id") val versionId: String? = null,
    val abbreviation: String? = null,
    val version: String? = null,
    val language: String? = null
)

@Serializable
data class BibleIQBooks(
    val data: List<BibleIQBook> = listOf(),
)
@Serializable
data class BibleIQBook(private val b: String? = null, private val n: String? = null) {
    val bookId = b?.toInt() ?: 1
    private val isLongName = bookId in longBookNamesSet
    val name = (if (isLongName) n?.take(7) else n) ?: "Genesis"
}

@Serializable
data class BibleChapter(
    val id: String? = null,
    val b: String? = null,
    val c: String? = null,
    val v: String? = null,
    val t: String? = null
)

data class BibleChapterUIState(
    val id: String? = null,
    val bookId: Int = 1,
    val chapterId: Int? = 1,
    val text: String? = null,
    val chapterList: List<Int>? = null,
)

val bibleBooks = listOf(
    "Genesis", "Exodus", "Leviticus", "Numbers", "Deuteronomy", "Joshua", "Judges", "Ruth",
    "1 Samuel", "2 Samuel", "1 Kings", "2 Kings", "1 Chron", "2 Chron", "Ezra",
    "Nehemiah", "Esther", "Job", "Psalms", "Proverbs", "Ecclesiastes", "Song of Solomon",
    "Isaiah", "Jeremiah", "Lamentations", "Ezekiel", "Daniel", "Hosea", "Joel", "Amos",
    "Obadiah", "Jonah", "Micah", "Nahum", "Habakkuk", "Zephaniah", "Haggai", "Zechariah",
    "Malachi", "Matthew", "Mark", "Luke", "John", "Acts", "Romans", "1 Corin",
    "2 Corin", "Galatians", "Ephesians", "Philippians", "Colossians", "1 Thess",
    "2 Thess", "1 Timothy", "2 Timothy", "Titus", "Philemon", "Hebrews", "James",
    "1 Peter", "2 Peter", "1 John", "2 John", "3 John", "Jude", "Revelation"
)

val bibleBooksFull = listOf(
    "Genesis", "Exodus", "Leviticus", "Numbers", "Deuteronomy", "Joshua", "Judges", "Ruth",
    "1 Samuel", "2 Samuel", "1 Kings", "2 Kings", "1 Chronicles", "2 Chronicles", "Ezra",
    "Nehemiah", "Esther", "Job", "Psalms", "Proverbs", "Ecclesiastes", "Song of Solomon",
    "Isaiah", "Jeremiah", "Lamentations", "Ezekiel", "Daniel", "Hosea", "Joel", "Amos",
    "Obadiah", "Jonah", "Micah", "Nahum", "Habakkuk", "Zephaniah", "Haggai", "Zechariah",
    "Malachi", "Matthew", "Mark", "Luke", "John", "Acts", "Romans", "1 Corinthians",
    "2 Corinthians", "Galatians", "Ephesians", "Philippians", "Colossians", "1 Thessalonians",
    "2 Thessalonians", "1 Timothy", "2 Timothy", "Titus", "Philemon", "Hebrews", "James",
    "1 Peter", "2 Peter", "1 John", "2 John", "3 John", "Jude", "Revelation"
)

