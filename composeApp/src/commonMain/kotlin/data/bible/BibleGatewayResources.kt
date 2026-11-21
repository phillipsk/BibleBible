package data.bible

import io.ktor.resources.Resource

@Resource("/GetVersions")
class GetVersions

@Resource("/GetChapter")
class GetChapter(
    val bookId: Int,
    private val chapterId: String? = "1",
    private val versionId: String? = BibleState.selectedVersion.lowercase(),
)

@Resource("/GetChapterCount")
class GetChapterCount(val bookId: Int)

@Resource("/GetVerseOfTheDay")
class GetVerseOfTheDay(
    val versionId: String = BibleState.selectedVersion.lowercase(),
)

@Resource("/GetBiblicalShorts")
class GetBiblicalShorts(
    val category: String? = null,
    val limit: Int? = null,
)
