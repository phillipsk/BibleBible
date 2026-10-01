package data

import data.apiBible.BookData
import data.bibleIQ.BibleChapter
import data.bibleIQ.BibleIQDataModel
import data.bibleIQ.ChapterCount
import data.gemini.CandidateDto
import data.gemini.ContentDto
import data.gemini.GeminiResponseDto
import data.gemini.PartDto
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class GeminiModelTest {
    private fun selectChapter(chapter: Int) {
        GeminiModel.resetForChapter()
        BibleIQDataModel.updateSelectedBook(BookData(name = "Genesis"))
        BibleIQDataModel.updateSelectedVersion("KJV")
        BibleIQDataModel.updateBibleChapter(
            listOf(BibleChapter(b = "1", c = chapter.toString(), t = "verse")),
            ChapterCount(100), "kjv",
        )
        GeminiModel.showSummary = true
    }

    private fun response(text: String) = GeminiResponseDto(
        candidates = listOf(CandidateDto(ContentDto(parts = listOf(PartDto(text))))),
    )

    @Test
    fun reopeningSummaryUsesCacheAndRefreshFetchesAgain() = runTest {
        selectChapter(81)
        var calls = 0
        val fetch: suspend (String) -> GeminiResponseDto = { calls++; response("summary $calls") }
        GeminiModel.generateAISummary(fetchSummary = fetch, summaryEnabled = true)
        selectChapter(81)
        GeminiModel.generateAISummary(fetchSummary = fetch, summaryEnabled = true)
        assertEquals(1, calls)
        assertEquals("summary 1", GeminiModel.geminiDataText)
        GeminiModel.generateAISummary(true, fetch, summaryEnabled = true)
        assertEquals(2, calls)
        assertEquals("summary 2", GeminiModel.geminiDataText)
    }

    @Test
    fun changingChapterIgnoresLateSummaryResponse() = runTest {
        selectChapter(82)
        val release = CompletableDeferred<Unit>()
        val request = launch {
            GeminiModel.generateAISummary(fetchSummary = { release.await(); response("old") }, summaryEnabled = true)
        }
        runCurrent()
        assertTrue(GeminiModel.isLoading)
        selectChapter(83)
        release.complete(Unit)
        request.join()
        assertNull(GeminiModel.geminiDataText)
        assertFalse(GeminiModel.isLoading)
    }

    @Test
    fun failedRefreshPreservesGoodSummaryAndClearsLoading() = runTest {
        selectChapter(84)
        GeminiModel.generateAISummary(fetchSummary = { response("cached") }, summaryEnabled = true)
        GeminiModel.generateAISummary(true, { error("Offline") }, summaryEnabled = true)
        assertEquals("cached", GeminiModel.geminiDataText)
        assertFalse(GeminiModel.isLoading)
        assertTrue(GeminiModel.showSummary)
    }

    @Test
    fun disabledSummaryDoesNotSendRequest() = runTest {
        selectChapter(85)
        GeminiModel.generateAISummary(fetchSummary = { error("Must not request") }, summaryEnabled = false)
        assertNull(GeminiModel.geminiDataText)
        assertFalse(GeminiModel.showSummary)
        assertFalse(GeminiModel.isLoading)
    }

    @Test
    fun cancellingInitialSummaryHidesEmptyViewAndClearsLoading() = runTest {
        selectChapter(86)
        val release = CompletableDeferred<Unit>()
        val request = launch {
            GeminiModel.generateAISummary(fetchSummary = { release.await(); response("unused") }, summaryEnabled = true)
        }
        runCurrent()
        assertTrue(GeminiModel.isLoading)
        request.cancelAndJoin()
        assertNull(GeminiModel.geminiDataText)
        assertFalse(GeminiModel.isLoading)
        assertFalse(GeminiModel.showSummary)
    }
}
