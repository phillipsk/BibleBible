package data.appPrefs

import data.apiBible.BookData
import data.apiBible.Chapter
import data.bibleIQ.BibleIQVersion
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ReadingSessionTest {
    private val now = 1_800_000_000_000L
    private val genesis = book("GEN", "Genesis", 50)
    private val exodus = book("EXO", "Exodus", 40)
    private val versions = listOf(BibleIQVersion(abbreviation = "KJV"), BibleIQVersion(abbreviation = "ASV"))
    private val reading = SavedReadingSession("GEN", 12, "KJV", 1456, 0.25f, lastActiveMillis = now)

    @Test
    fun restoresOnlyBeforeTwentyFourHours() {
        assertTrue(reading.isFresh(now + READING_SESSION_LIFETIME_MILLIS - 1))
        assertFalse(reading.isFresh(now + READING_SESSION_LIFETIME_MILLIS))
        assertFalse(reading.isFresh(now + READING_SESSION_LIFETIME_MILLIS + 1))
    }

    @Test
    fun rejectsFutureAndInvalidTimestampsRatherThanExtendingTheSession() {
        assertFalse(reading.copy(lastActiveMillis = now + 1).isFresh(now))
        assertFalse(reading.copy(lastActiveMillis = 0).isFresh(now))
        assertFalse(reading.copy(lastActiveMillis = Long.MIN_VALUE).isFresh(now))
    }

    @Test
    fun restoresCanonicalBookRegardlessOfAlphabeticalDisplayOrder() {
        val restored = validatedReadingSession(reading, listOf(exodus, genesis), versions, now)
        assertNotNull(restored)
        assertEquals("GEN", restored.bookKey)
        assertEquals(12, restored.chapter)
        assertEquals(1456, restored.characterOffset)
        assertEquals(0.25f, restored.lineFraction)
    }

    @Test
    fun rejectsRemovedBooksChaptersAndVersions() {
        val books = listOf(genesis, exodus)
        assertNull(validatedReadingSession(reading.copy(bookKey = "MISSING"), books, versions, now))
        assertNull(validatedReadingSession(reading.copy(chapter = 0), books, versions, now))
        assertNull(validatedReadingSession(reading.copy(chapter = 51), books, versions, now))
        assertNull(validatedReadingSession(reading.copy(version = "REMOVED"), books, versions, now))
        assertNotNull(validatedReadingSession(reading.copy(version = "kjv"), books, versions, now))
    }

    @Test
    fun homeStateDoesNotReopenThePreviousReadingLocation() {
        val home = reading.copy(isHome = true)
        val restored = validatedReadingSession(home, emptyList(), emptyList(), now)
        assertTrue(restored!!.isHome)
        assertNull(validatedReadingSession(home, emptyList(), emptyList(), now + READING_SESSION_LIFETIME_MILLIS))
    }

    @Test
    fun rejectsCorruptAnchors() {
        val books = listOf(genesis)
        assertNull(validatedReadingSession(reading.copy(characterOffset = -1), books, versions, now))
        assertNull(validatedReadingSession(reading.copy(lineFraction = Float.NaN), books, versions, now))
        assertNull(validatedReadingSession(reading.copy(lineFraction = Float.POSITIVE_INFINITY), books, versions, now))
        assertNull(validatedReadingSession(reading.copy(lineFraction = 1.1f), books, versions, now))
    }

    private fun book(key: String, name: String, chapters: Int) = BookData(
        id = key,
        name = name,
        chapters = listOf(Chapter(bookId = key, number = "intro")) +
            (1..chapters).map { Chapter(bookId = key, number = it.toString()) },
    )
}
