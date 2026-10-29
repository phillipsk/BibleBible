package data.appPrefs

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import data.apiBible.BibleAPIDataModel
import data.apiBible.BookData
import data.apiBible.currentTimeMillis
import data.bibleIQ.BibleIQDataModel
import data.bibleIQ.BibleIQVersion
import email.kevinphillips.biblebible.cache.DriverFactory
import email.kevinphillips.biblebible.db.BibleBibleDatabase
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

internal const val READING_SESSION_LIFETIME_MILLIS = 24L * 60 * 60 * 1000

internal data class SavedReadingSession(
    val bookKey: String,
    val chapter: Int,
    val version: String,
    val characterOffset: Int = 0,
    val lineFraction: Float = 0f,
    val isHome: Boolean = false,
    val lastActiveMillis: Long,
) {
    fun isFresh(nowMillis: Long): Boolean =
        lastActiveMillis > 0 && nowMillis >= lastActiveMillis &&
            nowMillis - lastActiveMillis < READING_SESSION_LIFETIME_MILLIS
}

internal fun canonicalBookKey(book: BookData): String = book.bookId.substringBefore('.')

internal fun validatedReadingSession(
    session: SavedReadingSession?,
    books: List<BookData>,
    versions: List<BibleIQVersion>,
    nowMillis: Long,
): SavedReadingSession? {
    session ?: return null
    if (!session.isFresh(nowMillis)) return null
    if (session.isHome) return session
    val book = books.find { canonicalBookKey(it) == session.bookKey } ?: return null
    if (book.chapterListBookData.orEmpty().none { it.number?.toIntOrNull() == session.chapter }) return null
    if (versions.none { it.abbreviation.equals(session.version, ignoreCase = true) }) return null
    if (session.characterOffset < 0 || !session.lineFraction.isFinite() || session.lineFraction !in 0f..1f) return null
    return session
}

internal class ReadingSessionStore(database: BibleBibleDatabase) {
    private val queries = database.bibleBibleDatabaseQueries

    fun read(): SavedReadingSession? = queries.selectReadingSession().executeAsOneOrNull()?.let {
        if (it.chapter !in 1..Int.MAX_VALUE.toLong() || it.characterOffset !in 0..Int.MAX_VALUE.toLong()) return null
        SavedReadingSession(
            bookKey = it.bookKey,
            chapter = it.chapter.toInt(),
            version = it.bibleVersion,
            characterOffset = it.characterOffset.toInt(),
            lineFraction = it.lineFraction.toFloat(),
            isHome = it.isHome != 0L,
            lastActiveMillis = it.lastActiveMillis,
        )
    }

    fun write(session: SavedReadingSession) = queries.saveReadingSession(
        bookKey = session.bookKey,
        chapter = session.chapter.toLong(),
        bibleVersion = session.version,
        characterOffset = session.characterOffset.toLong(),
        lineFraction = session.lineFraction.toDouble(),
        isHome = if (session.isHome) 1L else 0L,
        lastActiveMillis = session.lastActiveMillis,
    )

    fun clear() = queries.clearReadingSession()
}

/** Lives beyond a composable so a background save is not cancelled on disposal. */
internal object ReadingSession {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val writeMutex = Mutex()
    private var revision = 0L
    private var writtenRevision = -1L
    private var initialized = false
    private var captureAnchor: (() -> Unit)? = null
    private data class PendingWrite(val revision: Long, val snapshot: SavedReadingSession?)
    private val writes = Channel<PendingWrite>(Channel.CONFLATED)

    init {
        scope.launch {
            for (request in writes) write(request)
        }
    }

    var current by mutableStateOf<SavedReadingSession?>(null)
        private set

    fun registerAnchorCapture(capture: () -> Unit): () -> Unit {
        captureAnchor = capture
        return { if (captureAnchor === capture) captureAnchor = null }
    }

    private fun store(): ReadingSessionStore? =
        DriverFactory.createDriver()?.let { ReadingSessionStore(BibleBibleDatabase(it)) }

    suspend fun restore() {
        try {
            val previous = if (initialized) current else withContext(Dispatchers.IO) {
                writeMutex.withLock { store()?.read() }
            }
            val restored = validatedReadingSession(
                previous,
                BibleAPIDataModel.bibleBooks.data.orEmpty(),
                BibleIQDataModel.bibleVersions.data,
                currentTimeMillis(),
            )
            current = restored
            initialized = true
            if (previous != null && restored == null) persist(null)
            if (restored == null || restored.isHome) {
                BibleIQDataModel.discardReadingLocation()
                return
            }
            val book = BibleAPIDataModel.bibleBooks.data.orEmpty().first { canonicalBookKey(it) == restored.bookKey }
            BibleIQDataModel.updateSelectedVersion(restored.version)
            BibleIQDataModel.restoreReadingLocation(book, restored.chapter)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Napier.e("Unable to restore reading position", error, tag = "ReadingSession")
            BibleIQDataModel.showHomePage = true
        }
    }

    fun updateLocation(bookKey: String, chapter: Int, version: String) {
        val previous = current
        val sameLocation = previous?.bookKey == bookKey && previous.chapter == chapter && previous.version.equals(version, true)
        current = SavedReadingSession(
            bookKey = bookKey,
            chapter = chapter,
            version = version,
            characterOffset = if (sameLocation) previous.characterOffset else 0,
            lineFraction = if (sameLocation) previous.lineFraction else 0f,
            lastActiveMillis = currentTimeMillis(),
        )
        initialized = true
        persist(current)
    }

    fun updateAnchor(bookKey: String, chapter: Int, version: String, characterOffset: Int, lineFraction: Float) {
        val previous = current ?: return
        if (previous.isHome || previous.bookKey != bookKey || previous.chapter != chapter || !previous.version.equals(version, true)) return
        if (characterOffset < 0 || !lineFraction.isFinite() || lineFraction !in 0f..1f) return
        if (previous.characterOffset == characterOffset && previous.lineFraction == lineFraction) return
        current = previous.copy(
            characterOffset = characterOffset,
            lineFraction = lineFraction,
            lastActiveMillis = currentTimeMillis(),
        )
        persist(current)
    }

    fun goHome() {
        current = (current ?: SavedReadingSession("", 1, BibleIQDataModel.selectedVersion, lastActiveMillis = 1))
            .copy(isHome = true, lastActiveMillis = currentTimeMillis())
        initialized = true
        persist(current)
        BibleIQDataModel.discardReadingLocation()
    }

    fun onBackground(nowMillis: Long = currentTimeMillis()) {
        if (!initialized) return
        captureAnchor?.invoke()
        current = current?.copy(lastActiveMillis = nowMillis)
        persist(current)
    }

    fun onForeground(nowMillis: Long = currentTimeMillis()) {
        val session = current ?: return
        // Do not touch lastActive here: time spent in the background must count.
        if (!session.isFresh(nowMillis)) {
            current = null
            BibleIQDataModel.discardReadingLocation()
            persist(null)
        }
    }

    private fun persist(snapshot: SavedReadingSession?) {
        // Rapid scroll updates replace a pending snapshot instead of queuing an
        // unbounded number of SQLite writes. Lifecycle saves have no debounce.
        writes.trySend(PendingWrite(++revision, snapshot))
    }

    private suspend fun write(request: PendingWrite) {
        try {
            withContext(Dispatchers.IO) {
                writeMutex.withLock {
                    // A delayed older write must never replace newer navigation.
                    if (request.revision > writtenRevision) {
                        val store = checkNotNull(store()) { "No database driver for reading-position persistence" }
                        if (request.snapshot == null) store.clear() else store.write(request.snapshot)
                        writtenRevision = request.revision
                    }
                }
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Napier.e("Unable to save reading position", error, tag = "ReadingSession")
        }
    }
}
