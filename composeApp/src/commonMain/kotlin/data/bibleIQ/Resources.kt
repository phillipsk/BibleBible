package data.bibleIQ

import data.apiBible.BibleAPIDataModel
import io.ktor.resources.Resource

@Resource("/GetVersions")
class GetVersions

@Resource("/GetBooks")
class GetBooks(val language: String = "english")

@Resource("/GetChapter")
class GetChapter(
    val bookId: Int,
    private val chapterId: String = "1",
    private val versionId: String = BibleAPIDataModel.selectedVersion.lowercase()
)