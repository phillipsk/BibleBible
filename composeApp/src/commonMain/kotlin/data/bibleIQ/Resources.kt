package data.bibleIQ

import io.ktor.resources.Resource

@Resource("/GetVersions")
class GetVersions

@Resource("/GetBooks")
class GetBooks(val language: String = "english")

@Resource("/GetChapter")
class GetChapter(
    val bookId: Int,
    private val chapterId: String? = "1",
    private val versionId: String? = BibleIQDataModel.selectedVersion.lowercase()
)

@Resource("/GetChapterCount")
class GetChapterCount(val bookId: Int)