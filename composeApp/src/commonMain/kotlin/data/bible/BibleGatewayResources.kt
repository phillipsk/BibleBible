package data.bible

import email.kevinphillips.biblebible.security.ProtectedEndpoints
import io.ktor.client.HttpClient
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.url
import io.ktor.client.statement.HttpResponse

internal suspend fun HttpClient.getBibleChapter(
    bookId: Int,
    chapterId: String? = "1",
    versionId: String? = BibleState.selectedVersion.lowercase(),
    block: HttpRequestBuilder.() -> Unit = {},
): HttpResponse = get {
    url(path = ProtectedEndpoints.chapterPath())
    parameter("bookId", bookId)
    chapterId?.let { parameter("chapterId", it) }
    versionId?.let { parameter("versionId", it) }
    block()
}

internal suspend fun HttpClient.getChapterCount(
    bookId: Int,
    block: HttpRequestBuilder.() -> Unit = {},
): HttpResponse = get {
    url(path = ProtectedEndpoints.chapterCountPath())
    parameter("bookId", bookId)
    block()
}

internal suspend fun HttpClient.getVerseOfTheDay(
    versionId: String = BibleState.selectedVersion.lowercase(),
    after: String? = null,
    block: HttpRequestBuilder.() -> Unit = {},
): HttpResponse = get {
    url(path = ProtectedEndpoints.dailyVersePath())
    parameter("versionId", versionId)
    after?.let { parameter("after", it) }
    block()
}

internal suspend fun HttpClient.getBiblicalShorts(
    category: String? = null,
    limit: Int? = null,
    block: HttpRequestBuilder.() -> Unit = {},
): HttpResponse = get {
    url(path = ProtectedEndpoints.shortsFeedPath())
    category?.let { parameter("category", it) }
    limit?.let { parameter("limit", it) }
    block()
}
