package data.bibleIQ

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.material.BackdropValue
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.rememberBackdropScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import data.apiBible.BibleAPIDataModel
import data.apiBible.BookData
import io.github.aakira.napier.Napier
import kotlin.native.concurrent.ThreadLocal

@ThreadLocal
object BibleIQDataModel {
    const val RELEASE_BUILD = false
    const val DEFAULT_BIBLE_ID = "kjv"

    var isFirstLaunch by mutableStateOf(true)
    var showHomePage by mutableStateOf(true)

    @OptIn(ExperimentalMaterialApi::class)
    val backdropScaffoldState
        @Composable
        get() = rememberBackdropScaffoldState(
            initialValue = BackdropValue.Revealed,
            animationSpec = spring(stiffness = Spring.StiffnessLow)
//            snackbarHostState = SnackbarHostState()
        )
    var bibleVersions by mutableStateOf(BibleIQVersions())
        private set

    fun updateBibleVersions(newVersions: BibleIQVersions) {
        val list = newVersions.data.filter {
            it.abbreviation == "KJV" || it.abbreviation == "ASV"
                    || it.abbreviation == "RV1909" || it.abbreviation == "SVD"
        }
        bibleVersions = BibleIQVersions(data = newVersions.data)
    }

    private var _selectedVersion: MutableState<String> = mutableStateOf("")
        get() {
            if (field.value.isEmpty()) {
                field.value = bibleVersions.data.find {
                    it.abbreviation?.contains("KJV") == true
                }?.abbreviation ?: "KJV"
            }
            return field
        }
    val selectedVersion: String by _selectedVersion
    internal fun updateSelectedVersion(version: String? = null) {
        Napier.v("updateSelectedVersion: $version", tag = "IQ2452")
        _selectedVersion.value = version ?: (selectedVersion)
    }

    var bibleBooks by mutableStateOf(BibleIQBooks())
        private set

    fun updateBibleBooks(newBooks: BibleIQBooks) {
        bibleBooks = newBooks
    }

    var sortAZ by mutableStateOf(false)
        internal set

    val selectedSortType get() = if (sortAZ) "A-Z" else "OT-NT"

    val fontSizeOptions = listOf(16, 18, 20, 22, 24, 26, 28, 30)
    var selectedFontSize by mutableStateOf(20)

    var selectedBook by mutableStateOf(BookData())
        private set

    fun updateSelectedBook(newBook: BookData) {
        selectedBook = newBook
    }

    var bibleChapter by mutableStateOf<BibleChapterUIState?>(BibleChapterUIState())
        private set

    fun updateBibleChapter(
        newChapter: List<BibleChapter>,
        chapterCount: ChapterCount?,
        version: String
    ) {
        bibleChapter = newChapter.firstOrNull()?.b?.toInt()?.let { bookId ->
            BibleChapterUIState(
                id = newChapter.firstOrNull()?.id,
                bookId = bookId,
                chapterId = newChapter.firstOrNull()?.c?.toInt(),
                text = if (version.uppercase() == "SVD") {
                    newChapter.joinToString("\n") { "${it.v} ${it.t}" }
                } else {
                    newChapter.joinToString(" ") { "[${it.v}] ${it.t}" }
                },
                chapterList = chapterCount?.chapterCount?.let { count ->
                    (1..count).toList()
                }
            )
        }
    }

    fun getAPIBibleCardinal(chapterNumber: String): Int {
        val name = chapterNumber.substringBefore(".")
        return (BibleAPIDataModel.bibleBooks.data?.indexOfFirst { it.bookId == name } ?: 1).plus(1)
    }

    val onHomeClick: () -> Unit = {
        Napier.v("onHomeClick", tag = "BB2452")
        BibleIQDataModel.showHomePage = true
    }

    var errorSnackBar: String by mutableStateOf("")
        private set

    internal fun updateErrorSnackBar(error: String) {
        Napier.v("updateErrorSnackBar: $error", tag = "BB2452")
        errorSnackBar = error
    }

    internal fun clearErrorSnackBar() {
        errorSnackBar = ""
    }
}