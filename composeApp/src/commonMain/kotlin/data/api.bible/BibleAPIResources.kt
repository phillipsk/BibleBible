package data.api.bible

import io.ktor.resources.Resource
import kotlinx.serialization.SerialName

//@Serializable
/*@Resource("/GetVersions")
class GetVersions()*/

//@Serializable
@Resource("/books")
class GetBooksAPIBible(@SerialName("include-chapters") private val includeChapters: Boolean = true)

@Resource("/chapters/GEN.1")
class GetChapterAPIBible(
    @SerialName("content-type") val contentType: String = "text", // TODO: review text vs. HTML impl
    @SerialName("include-notes") val includeNotes: Boolean = false,
    @SerialName("include-titles") val includeTitles: Boolean = true,
    @SerialName("include-chapter-numbers") val includeChapterNumbers: Boolean = false,
    @SerialName("include-verse-numbers") val includeVerseNumbers: Boolean = true,
    @SerialName("include-verse-spans") val includeVerseSpans: Boolean = false
)

/*
*
* // https://api.scripture.api.bible/v1/bibles/de4e12af7f28f599-02/
FROM: https://api.scripture.api.bible/v1/bibles/de4e12af7f28f599-02/
*  chapters/GEN.1/?content-type=html&include-notes=false&include-titles=true&include-chapter-numbers=false&include-verse-numbers=true&include-verse-spans=false

*
// chapters/GEN.1?content-type=html&include-notes=false&include-titles=true&include-chapter-numbers=false&include-verse-numbers=true&include-verse-spans=false
// https://api.scripture.api.bible/v1/bibles/de4e12af7f28f599-02/
*
*
*
* https://api.scripture.api.bible/v1/bibles/de4e12af7f28f599-02/chapters/
* GEN.1?content-type=html&include-notes=false&include-titles=true&include-chapter-numbers=false&include-verse-numbers=true&include-verse-spans=false
* */

/*
@Resource("/GetChapter")
class GetChapter(
    val bookId: Int,
    private val chapterId: String = "1",
    private val versionId: String = BibleIQ.selectedVersion.value.lowercase()
)*/
