package data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import data.bibleIQ.BibleIQDataModel
import data.gemini.GeminiResponseDto
import data.gemini.generateContent
import email.kevinphillips.biblebible.BuildKonfig
import io.github.aakira.napier.Napier
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext

object GeminiModel {
    private var pendingSummary: Job? = null

    internal fun cancelPendingSummary() {
        pendingSummary?.cancel()
        pendingSummary = null
        showSummary = false
        isLoading = false
        updateGeminiData(GeminiResponseDto())
        geminiFullResponse = ""
    }

    internal var showSummary by mutableStateOf(false)
    internal var isLoading by mutableStateOf(false)
    internal val isAvailable: Boolean
        get() = BuildKonfig.AI_SUMMARIES_ENABLED && BuildKonfig.GEMINI_API_KEY.isNotBlank()

    private var geminiData by mutableStateOf(GeminiResponseDto())
    internal var errorMessage by mutableStateOf<String?>(null)
        private set

    internal fun updateGeminiData(data: GeminiResponseDto) {
        geminiData = data
        errorMessage = null
    }

    internal fun updateGeminiError(message: String) {
        geminiData = GeminiResponseDto()
        errorMessage = message
    }
    internal val geminiDataText: String? get() = geminiData.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text

    val isSuccessful get() = !isLoading && showSummary && !geminiDataText.isNullOrBlank()

    internal var geminiFullResponse by mutableStateOf("")
        private set

    internal fun concatGeminiResponse(text: String?) {
        geminiFullResponse += text ?: ""
    }

    private val geminiQuery
        get() = "Generate a summary of the bible chapter " +
                BibleIQDataModel.selectedBook.cleanedName + " " +
                BibleIQDataModel.bibleChapter?.chapterId

    internal suspend fun generateAISummary(
        pullToRefresh: Boolean = false,
        available: Boolean = isAvailable,
        generate: suspend (String) -> Unit = { generateContent(it) },
    ) {
        if (!available) {
            updateGeminiError("AI summaries are currently unavailable.")
            showSummary = false
            return
        }
        if (pullToRefresh || showSummary && isLoading && geminiDataText.isNullOrEmpty()) {
            val job = currentCoroutineContext()[Job]
            pendingSummary = job
            try {
                generate(geminiQuery)
            } finally {
                if (pendingSummary === job) pendingSummary = null
            }
        }
    }
}
