package email.kevinphillips.biblebible.gemini.model

interface IGenerativeModel {
    suspend fun generateContent(input: String): Any
}

expect fun provideGenerativeModel(): IGenerativeModel