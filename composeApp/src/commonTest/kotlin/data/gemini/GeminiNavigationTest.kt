package data.gemini

import data.GeminiModel
import data.bible.BibleState
import data.bible.BookData
import data.bible.Chapter
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import ui.SummaryBlock
import ui.parseMarkdownSummaryBlocks
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GeminiNavigationTest {
    @Test
    fun navigatingAwayCancelsSummaryAndRejectsALateResponse() = runTest {
        val started = CompletableDeferred<Unit>()
        val releaseResponse = CompletableDeferred<Unit>()
        val client = HttpClient(MockEngine {
            started.complete(Unit)
            withContext(NonCancellable) {
                releaseResponse.await()
                respond(
                    """{"candidates":[{"content":{"parts":[{"text":"Old chapter summary"}]}}]}""",
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                )
            }
        }) { install(ContentNegotiation) { json() } }

        try {
            GeminiModel.showSummary = true
            GeminiModel.isLoading = true
            val request = launch {
                GeminiModel.generateAISummary(pullToRefresh = true, available = true) { prompt -> generateContent(prompt, client) }
            }
            started.await()
            BibleState.onHomeClick()
            releaseResponse.complete(Unit)
            request.join()

            assertTrue(request.isCancelled)
            assertNull(GeminiModel.geminiDataText, "A canceled chapter's response must not become the new summary")
            assertFalse(GeminiModel.showSummary)
            assertFalse(GeminiModel.isLoading)
            assertTrue(BibleState.showHomePage)
        } finally {
            releaseResponse.complete(Unit)
            GeminiModel.cancelPendingSummary()
            client.close()
        }
    }

    @Test
    fun preserveShowSummaryKeepsAiModeActiveAcrossSameBookChapterSwipe() = runTest {
        val started = CompletableDeferred<Unit>()
        val releaseResponse = CompletableDeferred<Unit>()
        val client = HttpClient(MockEngine {
            started.complete(Unit)
            withContext(NonCancellable) {
                releaseResponse.await()
                respond(
                    """{"candidates":[{"content":{"parts":[{"text":"Ruth 3 summary"}]}}]}""",
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                )
            }
        }) { install(ContentNegotiation) { json() } }

        try {
            GeminiModel.showSummary = true
            GeminiModel.isLoading = true
            val request = launch {
                GeminiModel.generateAISummary(pullToRefresh = true, available = true) { prompt ->
                    generateContent(prompt, client)
                }
            }
            started.await()
            GeminiModel.cancelPendingSummary(preserveShowSummary = true)
            releaseResponse.complete(Unit)
            request.join()

            assertTrue(request.isCancelled)
            assertTrue(GeminiModel.showSummary, "Sticky AI mode must remain active when swiping chapters in the same book")
            assertFalse(GeminiModel.isLoading)
            assertNull(GeminiModel.geminiDataText)

            // Simulate a loaded summary locking AI mode, then swiping to another chapter
            GeminiModel.updateGeminiData(
                GeminiResponseDto(
                    candidates = listOf(CandidateDto(content = ContentDto(parts = listOf(PartDto(text = "Ruth 3 summary")))))
                )
            )
            assertTrue(GeminiModel.isAiButtonHighlighted)

            GeminiModel.cancelPendingSummary(preserveShowSummary = true)
            assertTrue(GeminiModel.isAiButtonHighlighted, "Gold checkmark stays locked while loading the next chapter")

            // If loading the next chapter fails or exits, the checkmark and gold highlight clear
            GeminiModel.updateGeminiError("Temporary error")
            GeminiModel.cancelPendingSummary(preserveShowSummary = false)
            assertFalse(GeminiModel.isAiButtonHighlighted)
        } finally {
            releaseResponse.complete(Unit)
            GeminiModel.cancelPendingSummary()
            client.close()
        }
    }

    @Test
    fun restoreReadingLocationDisablesStickyAiModeEvenWithinSameBook() {
        val ruth = BookData(
            id = "RUT",
            name = "Ruth",
            chapters = (1..4).map { ch ->
                Chapter(name = "RUT.$ch", bookId = "RUT", number = ch.toString(), position = ch)
            },
        )
        try {
            BibleState.updateSelectedBook(ruth)
            GeminiModel.showSummary = true
            GeminiModel.updateGeminiData(
                GeminiResponseDto(
                    candidates = listOf(CandidateDto(content = ContentDto(parts = listOf(PartDto(text = "Ruth 3")))))
                )
            )

            BibleState.restoreReadingLocation(ruth, 4)

            assertFalse(GeminiModel.showSummary, "Deep linking or explicit jumps must not re-enable AI summary mode")
            assertNull(GeminiModel.geminiDataText)
            assertEquals("RUT.4", BibleState.selectedChapter)
        } finally {
            GeminiModel.cancelPendingSummary()
            BibleState.onHomeClick()
        }
    }

    @Test
    fun parseMarkdownSummaryBlocksStripsHorizontalRulesAndReplacesDashes() {
        val raw = """
            # Ruth 3
            ---
            - **Kindness** — Boaz honors Ruth.
            ***
            ___
            Key theme -- redemption in action.
        """.trimIndent()

        val blocks = parseMarkdownSummaryBlocks(raw)

        assertEquals(
            listOf(
                SummaryBlock.Heading("Ruth 3"),
                SummaryBlock.Bullet("**Kindness**, Boaz honors Ruth."),
                SummaryBlock.Paragraph("Key theme, redemption in action."),
            ),
            blocks,
        )
    }
}
