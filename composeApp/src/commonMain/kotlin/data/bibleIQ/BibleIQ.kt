package data.bibleIQ

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import data.api.bible.BibleAPIBibles
import data.api.bible.BibleAPIBook
import data.api.bible.BookData
import data.api.bible.ChapterContent
import kotlin.native.concurrent.ThreadLocal

@ThreadLocal
object BibleIQ {
    private const val defaultBibleId = "de4e12af7f28f599-02"

    var selectedBibleId = mutableStateOf(defaultBibleId)
    var chapter = mutableStateOf(ChapterContent())
    var books = mutableStateOf(BibleAPIBook())
    var bibleVersions = mutableStateOf(BibleAPIBibles())
    val abbreviationList get() = bibleVersions.value.data?.map { it } ?: emptyList()
    var selectedVersion: MutableState<String> = mutableStateOf("")
        get() {
            if (field.value.isEmpty()) {
                field.value = abbreviationList.find {
                    it.abbreviationLocal?.contains("KJV") == true
                }?.abbreviationLocal ?: "KJV"
            }
            println("println :: updated selectedVersion $field")
            return field
        }
    var selectedBookData = mutableStateOf(BookData())

    var selectedChapter = mutableStateOf(-1)
    var selectedChapterString = ""

    fun updateSelectedChapter(chapter: Int? = null) {
        if (selectedChapter.value == -1) {
            val chapterList = selectedBookData.value.chapters
            if (!chapterList.isNullOrEmpty()) {
                selectedChapter.value = chapter ?: 1
                selectedChapterString = selectedBookData.value.bookId + "." + selectedChapter.value
            }
        }
    }

}