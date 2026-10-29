package data.appPrefs

import data.bibleIQ.BibleIQDataModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ReadingSessionLifecycleTest {
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
