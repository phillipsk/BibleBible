package data.bibleIQ

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

val longBookNamesSet = setOf(13,14,46,47,52,53)

@Serializable
data class BibleVersion(
    val table: String? = null,
    @SerialName("version_id") val versionId: String? = null,
    val abbreviation: String? = null,
    val version: String? = null,
    val language: String? = null
)

@Serializable
data class BibleBook(private val b: String? = null, private val n: String? = null) {
    private val bookId = b?.toInt()
    private val isLongName = bookId in longBookNamesSet
    val name = if (isLongName) n?.take(7) else n
}

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

// You can access and use the 'bibleBooks' list in your Kotlin code as needed.


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