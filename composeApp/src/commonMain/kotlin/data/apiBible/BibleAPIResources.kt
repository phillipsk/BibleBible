package data.apiBible

import io.ktor.resources.Resource
import kotlinx.serialization.SerialName

@Resource("/bibles")
class GetBiblesAPIBible(private val language: String? = BibleAPIDataModel.selectedLanguage)

@Resource("/bibles/{bibleId}/books")
class GetBooksAPIBible(
    private val bibleId: String = BibleAPIDataModel.selectedBibleId,
    @SerialName("include-chapters") private val includeChapters: Boolean = true
)

@Resource("/bibles/{bibleId}/chapters/{chapter}")
class GetChapterAPIBible(
    private val bibleId: String,
    val chapter: String,
    @SerialName("content-type") val contentType: String = "text",
    @SerialName("include-notes") val includeNotes: Boolean = false,
    @SerialName("include-titles") val includeTitles: Boolean = true,
    @SerialName("include-chapter-numbers") val includeChapterNumbers: Boolean = true,
    @SerialName("include-verse-numbers") val includeVerseNumbers: Boolean = true,
    @SerialName("include-verse-spans") val includeVerseSpans: Boolean = false
)