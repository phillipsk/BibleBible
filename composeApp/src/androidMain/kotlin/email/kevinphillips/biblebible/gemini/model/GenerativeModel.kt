package email.kevinphillips.biblebible.gemini.model

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import data.GeminiModel
import data.bibleIQ.BibleIQDataModel
import email.kevinphillips.biblebible.BuildKonfig
import io.github.aakira.napier.Napier

actual fun provideGenerativeModel(): IGenerativeModel = object : IGenerativeModel {
    override suspend fun generateContent(input: String): GenerativeModel {
        // Use GenerativeModel specific to Android
        val generativeModel =
            GenerativeModel(modelName = "gemini-pro-vision", apiKey = BuildKonfig.GEMINI_API_KEY)
        return generativeModel
    }
}

class GeminiAndroidService() {
    val inputContent = content {
        text("Generate a summary of the bible chapter Matthew 21")
    }
    val generativeModel = GenerativeModel(
        modelName = "gemini-pro",
        apiKey = BuildKonfig.GEMINI_API_KEY
    )

    suspend fun collectContentStream() = generativeModel.generateContentStream(inputContent).collect { chunk ->
        Napier.d("collectContentStream: $chunk", tag = "GeminiService")
        GeminiModel.concatGeminiResponse(chunk.text)
        chunk.text?.let { BibleIQDataModel.updateErrorSnackBar(it) }

        print(chunk.text)
    }
}
