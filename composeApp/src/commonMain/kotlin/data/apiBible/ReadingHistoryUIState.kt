package data.apiBible

data class ReadingHistoryUIState(
    val bookName: String? = null,
    val chapterId: Int? = null,
) {
    override fun toString(): String {
        return "$bookName, Chapter: $chapterId"
    }
}