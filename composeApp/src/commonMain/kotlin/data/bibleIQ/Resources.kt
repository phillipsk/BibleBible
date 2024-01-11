package data.bibleIQ

import data.api.apiBible.BibleAPIDataModel
import io.ktor.resources.Resource

//@Serializable
@Resource("/GetVersions")
class GetVersions()

//@Serializable
@Resource("/GetBooks")
class GetBooks(val language: String = "english")

@Resource("/GetChapter")
class GetChapter(
    val bookId: Int,
    private val chapterId: String = "1",
    private val versionId: String = BibleAPIDataModel.selectedVersion.value.lowercase()
)