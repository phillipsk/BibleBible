package data.api.bible

import data.bibleIQ.BibleIQ
import io.ktor.resources.Resource
import kotlinx.serialization.SerialName

@Resource("/bibles")
class GetBiblesAPIBible(val language: String = "eng")

@Resource("/bibles/{bibleId}/books")
class GetBooksAPIBible(
    val bibleId: String = BibleIQ.selectedBibleId.value,
    @SerialName("include-chapters") private val includeChapters: Boolean = true
)

@Resource("/bibles/{bibleId}/chapters/{chapter}")
class GetChapterAPIBible(
    private val bibleId: String = BibleIQ.selectedBibleId.value,
    val chapter: String,
    @SerialName("content-type") val contentType: String = "text", // TODO: review text vs. HTML impl
    @SerialName("include-notes") val includeNotes: Boolean = false,
    @SerialName("include-titles") val includeTitles: Boolean = true,
    @SerialName("include-chapter-numbers") val includeChapterNumbers: Boolean = false,
    @SerialName("include-verse-numbers") val includeVerseNumbers: Boolean = true,
    @SerialName("include-verse-spans") val includeVerseSpans: Boolean = false
)