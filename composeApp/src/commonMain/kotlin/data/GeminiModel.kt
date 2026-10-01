package data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import data.bibleIQ.BibleIQDataModel
import data.gemini.GeminiResponseDto
import data.gemini.generateContent
import email.kevinphillips.biblebible.BuildKonfig
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CancellationException

object GeminiModel {

    internal var showSummary by mutableStateOf(false)
    internal var isLoading by mutableStateOf(false)
        private set

    internal val isAvailable: Boolean
        get() = BuildKonfig.AI_SUMMARIES_ENABLED && BuildKonfig.GEMINI_API_KEY.isNotBlank()
    private val summaries = RequestCache<SummaryKey, GeminiResponseDto>(maxEntries = 32)
    private var chapterGeneration = 0

    private data class SummaryKey(val book: String, val chapter: Int, val version: String)

    private var geminiData by mutableStateOf(GeminiResponseDto())
    internal fun updateGeminiData(data: GeminiResponseDto) {
        geminiData = data
    }
    internal val geminiDataText: String? get() = geminiData.summaryText

    val isSuccessful get() = !isLoading && showSummary && geminiDataText != null

    internal var geminiFullResponse by mutableStateOf("")
        private set

    internal fun concatGeminiResponse(text: String?) {
        geminiFullResponse += text ?: ""
    }

    internal fun resetForChapter() {
        chapterGeneration++
        showSummary = false
        isLoading = false
        geminiData = GeminiResponseDto()
    }

    internal suspend fun generateAISummary(
        pullToRefresh: Boolean = false,
        fetchSummary: suspend (String) -> GeminiResponseDto = { generateContent(it) },
        summaryEnabled: Boolean = isAvailable,
    ) {
        if (isLoading) return
        if (!summaryEnabled) {
            BibleIQDataModel.updateErrorSnackBar("AI summaries are currently unavailable.")
            showSummary = false
            return
        }
        val book = BibleIQDataModel.selectedBook.name
        val chapter = BibleIQDataModel.bibleChapter?.chapterId
        if (book.isNullOrBlank() || chapter == null) {
            showSummary = false
            return
        }
        val key = SummaryKey(book, chapter, BibleIQDataModel.selectedVersion.lowercase())
        val generation = chapterGeneration
        isLoading = true
        try {
            val response = summaries.get(key, refresh = pullToRefresh) {
                fetchSummary("Summarize $book $chapter (${key.version}) in at most 150 words.").also {
                    if (it.summaryText == null) error("Empty summary")
                }
            }
            if (generation == chapterGeneration) updateGeminiData(response)
        } catch (cancelled: CancellationException) {
            if (generation == chapterGeneration && geminiDataText == null) showSummary = false
            throw cancelled
        } catch (error: Exception) {
            if (generation == chapterGeneration) {
                Napier.e("AI summary request failed (${error::class.simpleName}).", tag = "GeminiServiceImp")
                BibleIQDataModel.updateErrorSnackBar("AI summary could not connect. Please try again later.")
                if (geminiDataText == null) showSummary = false
            }
        } finally {
            if (generation == chapterGeneration) isLoading = false
        }
    }
}
