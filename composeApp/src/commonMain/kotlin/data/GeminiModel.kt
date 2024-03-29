package data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import data.bibleIQ.BibleIQDataModel
import data.gemini.GeminiResponseDto
import io.github.aakira.napier.Napier

object GeminiModel {

    var geminiData by mutableStateOf(GeminiResponseDto())
        private set

    val geminiDataText get() = geminiData.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text

    internal fun updateGeminiData(data: GeminiResponseDto) {
        geminiData = data
        Napier.v("updateGeminiData: ${geminiDataText?.take(100)}", tag = "GeminiServiceImp")
    }

    var geminiFullResponse by mutableStateOf("")
        private set

    fun concatGeminiResponse(text: String?) {
        geminiFullResponse += text ?: ""
    }

    val geminiQuery get() =  "Generate a summary of the bible chapter " +
            BibleIQDataModel.selectedBook.cleanedName + " " + BibleIQDataModel.bibleChapter?.chapterId

    val generateAISummary: () -> Unit = {

    }
}