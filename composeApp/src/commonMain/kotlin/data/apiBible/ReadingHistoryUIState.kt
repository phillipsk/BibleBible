package data.apiBible

data class ReadingHistoryUIState(
    val bookName: String? = null,
    val chapterId: Int? = null,
    val date: String? = null,
) {
    override fun toString(): String {
        return "$date   $bookName, Chapter: $chapterId"
    }
}