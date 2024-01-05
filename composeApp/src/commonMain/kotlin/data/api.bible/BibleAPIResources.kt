package data.api.bible

import io.ktor.resources.Resource
import kotlinx.serialization.SerialName

//@Serializable
/*@Resource("/GetVersions")
class GetVersions()*/

//@Serializable
@Resource("/books")
class GetBooksAPIBible(@SerialName("include-chapters") private val includeChapters: Boolean = true)

/*
@Resource("/GetChapter")
class GetChapter(
    val bookId: Int,
    private val chapterId: String = "1",
    private val versionId: String = BibleIQ.selectedVersion.value.lowercase()
)*/
