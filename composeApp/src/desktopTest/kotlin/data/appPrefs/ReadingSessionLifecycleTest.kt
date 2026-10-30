package data.appPrefs

import data.bibleIQ.BibleIQDataModel
import data.apiBible.BookData
import data.apiBible.Chapter
import data.bibleIQ.BibleChapter
import data.bibleIQ.ChapterCount
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.assertSame

class ReadingSessionLifecycleTest {
    @Test
    fun recreatingTheAppKeepsAnAlreadyLoadedMatchingChapter() {
        val book = BookData(
            id = "GEN", name = "Genesis",
            chapters = listOf(Chapter(bookId = "GEN", number = "intro")) +
                (1..3).map { Chapter(bookId = "GEN", number = it.toString()) },
        )
        BibleIQDataModel.updateSelectedVersion("KJV")
        val bookId = BibleIQDataModel.getAPIBibleOrdinal(book.remoteKey)
        BibleIQDataModel.updateBibleChapter(
            listOf(BibleChapter(id = "test", b = bookId.toString(), c = "3", v = "1", t = "Already loaded")),
            ChapterCount(3), "KJV"
        )
        try {
            val loaded = BibleIQDataModel.bibleChapter
            BibleIQDataModel.restoreReadingLocation(book, 3)
            assertSame(loaded, BibleIQDataModel.bibleChapter)
            assertFalse(BibleIQDataModel.showHomePage)

            BibleIQDataModel.restoreReadingLocation(book, 2)
            assertEquals(2, BibleIQDataModel.bibleChapter?.chapterId)
            assertNull(BibleIQDataModel.bibleChapter?.text)
        } finally {
            BibleIQDataModel.discardReadingLocation()
        }
    }

    @Test
    fun backgroundCapturesLatestAnchorAndForegroundChecksExpiryBeforeRefreshingTime() {
        val backgroundTime = 1_800_000_000_000L
        ReadingSession.updateLocation("GEN", 12, "KJV")
        var captured = false
        val unregister = ReadingSession.registerAnchorCapture {
            captured = true
            ReadingSession.updateAnchor("GEN", 12, "KJV", 2351, 0.25f)
        }
        try {
            ReadingSession.onBackground(backgroundTime)
            assertTrue(captured)
            val background = assertNotNull(ReadingSession.current)
            assertEquals(2351, background.characterOffset)
            assertEquals(0.25f, background.lineFraction)
            assertEquals(backgroundTime, background.lastActiveMillis)

            ReadingSession.onForeground(backgroundTime + READING_SESSION_LIFETIME_MILLIS - 1)
            assertEquals(background, ReadingSession.current)
            ReadingSession.onForeground(backgroundTime + READING_SESSION_LIFETIME_MILLIS)
            assertNull(ReadingSession.current)
            assertTrue(BibleIQDataModel.showHomePage)
            assertNull(BibleIQDataModel.bibleChapter?.bookId)
        } finally {
            unregister()
        }
    }

    @Test
    fun disposedReaderCannotReplaceTheCaptureProviderForTheNewChapter() {
        var oldCaptured = false
        var newCaptured = false
        ReadingSession.updateLocation("GEN", 4, "KJV")
        val disposeOldReader = ReadingSession.registerAnchorCapture { oldCaptured = true }
        val disposeNewReader = ReadingSession.registerAnchorCapture { newCaptured = true }
        try {
            disposeOldReader()
            ReadingSession.onBackground()
            assertFalse(oldCaptured)
            assertTrue(newCaptured)
        } finally {
            disposeNewReader()
            ReadingSession.goHome()
        }
    }
}
