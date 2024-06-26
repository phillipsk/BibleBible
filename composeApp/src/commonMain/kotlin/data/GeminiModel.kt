package data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import data.bibleIQ.BibleIQDataModel
import data.gemini.GeminiResponseDto
import data.gemini.generateContent
import io.github.aakira.napier.Napier

object GeminiModel {

    internal var showSummary by mutableStateOf(false)
    internal var isLoading by mutableStateOf(false)

    private var geminiData by mutableStateOf(GeminiResponseDto())
    internal fun updateGeminiData(data: GeminiResponseDto) {
        geminiData = data
        Napier.v("updateGeminiData: ${geminiDataText?.take(100)}", tag = "GeminiServiceImp")
    }
    internal val geminiDataText: String? get() = geminiData.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text

    val isSuccessful get() = !isLoading && showSummary && geminiDataText != null

    internal var geminiFullResponse by mutableStateOf("")
        private set

    internal fun concatGeminiResponse(text: String?) {
        geminiFullResponse += text ?: ""
    }

    private val geminiQuery
        get() = "Generate a summary of the bible chapter " +
                BibleIQDataModel.selectedBook.cleanedName + " " +
                BibleIQDataModel.bibleChapter?.chapterId

    internal suspend fun generateAISummary(pullToRefresh: Boolean = false) {
        if (pullToRefresh || showSummary && isLoading && geminiDataText.isNullOrEmpty()) {
            generateContent(geminiQuery)
        }
    }
}