package data.api.bible

import io.ktor.resources.Resource
import kotlinx.serialization.SerialName

@Resource("/books")
class GetBooksAPIBible(@SerialName("include-chapters") private val includeChapters: Boolean = true)

@Resource("/chapters/{chapter}")
class GetChapterAPIBible(
    val chapter: String,
    @SerialName("content-type") val contentType: String = "text", // TODO: review text vs. HTML impl
    @SerialName("include-notes") val includeNotes: Boolean = false,
    @SerialName("include-titles") val includeTitles: Boolean = true,
    @SerialName("include-chapter-numbers") val includeChapterNumbers: Boolean = false,
    @SerialName("include-verse-numbers") val includeVerseNumbers: Boolean = true,
    @SerialName("include-verse-spans") val includeVerseSpans: Boolean = false
)