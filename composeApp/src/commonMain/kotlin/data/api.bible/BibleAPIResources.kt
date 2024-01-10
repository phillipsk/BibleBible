package data.api.bible

import data.bibleIQ.BibleIQ
import io.ktor.resources.Resource
import kotlinx.serialization.SerialName

@Resource("/bibles")
class GetBiblesAPIBible(private val language: String? = BibleIQ.selectedLanguage?.value)

@Resource("/bibles/{bibleId}/books")
class GetBooksAPIBible(
    private val bibleId: String = BibleIQ.selectedBibleId.value,
    @SerialName("include-chapters") private val includeChapters: Boolean = true
)

@Resource("/bibles/{bibleId}/chapters/{chapter}")
class GetChapterAPIBible(
    private val bibleId: String = BibleIQ.selectedBibleId.value,
    private val chapter: String = BibleIQ.selectedChapterString,
    @SerialName("content-type") val contentType: String = "text", // TODO: review text vs. HTML impl
    @SerialName("include-notes") val includeNotes: Boolean = false,
    @SerialName("include-titles") val includeTitles: Boolean = true,
    @SerialName("include-chapter-numbers") val includeChapterNumbers: Boolean = true,
    @SerialName("include-verse-numbers") val includeVerseNumbers: Boolean = true,
    @SerialName("include-verse-spans") val includeVerseSpans: Boolean = false
)