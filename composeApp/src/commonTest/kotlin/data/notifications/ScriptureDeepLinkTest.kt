package data.notifications

import data.bible.BibleBooksResponse
import data.bible.BibleState
import data.bible.DailyVerseData
import data.bible.json.BUNDLED_BIBLE_BOOKS_JSON
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ScriptureDeepLinkTest {
    private val originalBooks = BibleState.bibleBooks
    private val originalVersion = BibleState.selectedVersion

    @AfterTest
    fun resetState() {
        BibleState.discardReadingLocation()
        BibleState.updateBooks(originalBooks)
        BibleState.updateSelectedVersion(originalVersion)
    }

    @Test
    fun parsesAndroidLocalNotificationExtrasIncludingVerseAndVersion() {
        val parsed = parseVotdNotificationPayload(
            mapOf(
                "votd_book_usfm" to "jhn",
                "votd_chapter" to "3",
                "votd_verse" to "16",
                "votd_version" to "bsb",
            )
        )
        assertNotNull(parsed)
        assertEquals("JHN", parsed.bookUsfm)
        assertEquals(43, parsed.bookNumber)
        assertEquals(3, parsed.chapter)
        assertEquals(16, parsed.verse)
        assertEquals("BSB", parsed.version)
        assertFalse(parsed.fetchDailyIfMissing)
    }

    @Test
    fun parsesFcmAndApnsPayloadByUsfmOrBookNumberFallback() {
        val byUsfm = parseVotdNotificationPayload(
            mapOf(
                "type" to "votd",
                "bookNumber" to "19",
                "bookUsfm" to "PSA",
                "chapter" to "23",
                "verse" to "4",
                "versionId" to "KJV",
            )
        )
        assertNotNull(byUsfm)
        assertEquals("PSA", byUsfm.bookUsfm)
        assertEquals(19, byUsfm.bookNumber)
        assertEquals(23, byUsfm.chapter)
        assertEquals(4, byUsfm.verse)
        assertEquals("KJV", byUsfm.version)

        val byNumberOnly = parseVotdNotificationPayload(
            mapOf(
                "type" to "votd",
                "bookNumber" to "50",
                "chapter" to "4",
                "verse" to "13",
            )
        )
        assertNotNull(byNumberOnly)
        assertEquals("PHP", byNumberOnly.bookUsfm)
        assertEquals(50, byNumberOnly.bookNumber)
        assertEquals(4, byNumberOnly.chapter)
        assertEquals(13, byNumberOnly.verse)
    }

    @Test
    fun parsesFallbackLocalVotdPayloadAndRejectsUnrelatedPayloads() {
        val fallback = parseVotdNotificationPayload(mapOf("type" to "votd"))
        assertNotNull(fallback)
        assertTrue(fallback.fetchDailyIfMissing)

        assertNull(parseVotdNotificationPayload(emptyMap()))
        assertNull(parseVotdNotificationPayload(mapOf("type" to "other", "foo" to "bar")))
        assertNull(parseVotdNotificationPayload(mapOf("bookUsfm" to "JHN", "chapter" to "0")))
    }

    @Test
    fun findVerseCharacterOffsetLocatesBracketedAndSvdVerses() {
        val kjvChapter = "[1] Verse one [15] Verse fifteen [16] For God so loved [17] Next"
        assertNull(findVerseCharacterOffset(kjvChapter, 1, "KJV"))
        assertEquals(
            kjvChapter.indexOf("[16] "),
            findVerseCharacterOffset(kjvChapter, 16, "KJV"),
        )
        assertNull(findVerseCharacterOffset(kjvChapter, 99, "KJV"))

        val svdChapter = "1 الآية الأولى\n16 لأنه هكذا أحب الله العالم\n17 التالية"
        assertNull(findVerseCharacterOffset(svdChapter, 1, "SVD"))
        assertEquals(
            svdChapter.indexOf("16 "),
            findVerseCharacterOffset(svdChapter, 16, "SVD"),
        )
    }

    @Test
    fun consumePendingDeepLinkNavigatesToExactBookChapterAndTargetVerse() = runTest {
        val books = Json.decodeFromString<BibleBooksResponse>(BUNDLED_BIBLE_BOOKS_JSON)
        BibleState.updateBooks(books)
        BibleState.showHomePage = true
        BibleState.showBiblicalShorts = true

        BibleState.enqueueDeepLink(
            ScriptureDeepLink(
                bookUsfm = "PHP",
                bookNumber = 50,
                chapter = 4,
                verse = 13,
                version = "BSB",
            )
        )

        val handled = BibleState.consumePendingDeepLink()
        assertTrue(handled)
        assertNull(BibleState.pendingDeepLink)
        assertFalse(BibleState.showHomePage)
        assertFalse(BibleState.showBiblicalShorts)
        assertEquals("Philippians", BibleState.selectedBook.name)
        assertEquals(50, BibleState.bibleChapter?.bookId)
        assertEquals(4, BibleState.bibleChapter?.chapterId)
        assertEquals(13, BibleState.pendingTargetVerse)
        assertEquals("PHP", BibleState.pendingTargetBookId)
        assertEquals(4, BibleState.pendingTargetChapter)
        assertEquals("BSB", BibleState.selectedVersion)
    }

    @Test
    fun consumePendingDeepLinkFetchesDailyVerseWhenMissingFromLocalNotification() = runTest {
        val books = Json.decodeFromString<BibleBooksResponse>(BUNDLED_BIBLE_BOOKS_JSON)
        BibleState.updateBooks(books)

        BibleState.enqueueDeepLink(ScriptureDeepLink(fetchDailyIfMissing = true))
        val handled = BibleState.consumePendingDeepLink {
            DailyVerseData(
                bookNumber = 23,
                chapter = 40,
                verse = 31,
                text = "They that wait upon the LORD shall renew their strength",
                bookUsfm = "ISA",
            )
        }

        assertTrue(handled)
        assertEquals("Isaiah", BibleState.selectedBook.name)
        assertEquals(23, BibleState.bibleChapter?.bookId)
        assertEquals(40, BibleState.bibleChapter?.chapterId)
        assertEquals(31, BibleState.pendingTargetVerse)
        assertFalse(BibleState.showHomePage)
    }

    @Test
    fun parsesAndConsumesBiblicalWordsReadingNudgeAndAnnouncementPayloads() = runTest {
        val books = Json.decodeFromString<BibleBooksResponse>(BUNDLED_BIBLE_BOOKS_JSON)
        BibleState.updateBooks(books)

        val wordsLink = parseVotdNotificationPayload(
            mapOf(
                "type" to "biblical_words",
                "destination" to "biblical_words",
                "category" to "Faith",
            )
        )
        assertNotNull(wordsLink)
        assertEquals(PushDestinationKind.BIBLICAL_WORDS, wordsLink.destination)
        assertEquals("Faith", wordsLink.shortsCategory)

        BibleState.enqueueDeepLink(wordsLink)
        assertTrue(BibleState.consumePendingDeepLink())
        assertTrue(BibleState.showBiblicalShorts)
        assertEquals("Faith", BibleState.selectedShortsCategory)

        val readingNudge = parseVotdNotificationPayload(
            mapOf(
                "type" to "reading_nudge",
                "destination" to "scripture",
                "bookNumber" to "19",
                "bookUsfm" to "PSA",
                "chapter" to "23",
                "verse" to "1",
                "versionId" to "KJV",
            )
        )
        assertNotNull(readingNudge)
        assertEquals(PushDestinationKind.SCRIPTURE, readingNudge.destination)
        BibleState.enqueueDeepLink(readingNudge)
        assertTrue(BibleState.consumePendingDeepLink())
        assertFalse(BibleState.showBiblicalShorts)
        assertEquals("Psalms", BibleState.selectedBook.name)
        assertEquals(23, BibleState.bibleChapter?.chapterId)

        val homeAnnouncement = parseVotdNotificationPayload(mapOf("type" to "announcement"))
        assertNotNull(homeAnnouncement)
        assertEquals(PushDestinationKind.HOME, homeAnnouncement.destination)
        BibleState.enqueueDeepLink(homeAnnouncement)
        assertTrue(BibleState.consumePendingDeepLink())
        assertTrue(BibleState.showHomePage)
    }
}
