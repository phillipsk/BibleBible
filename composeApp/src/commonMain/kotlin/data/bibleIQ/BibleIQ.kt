package data.bibleIQ

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import data.api.bible.BibleAPIBook
import ui.Chapter
import kotlin.native.concurrent.ThreadLocal

@ThreadLocal
object BibleIQ {

    var chapter = mutableStateOf(listOf<Chapter>())
    var books: MutableState<BibleAPIBook> = mutableStateOf(BibleAPIBook())
    var bibleVersions = mutableStateOf(listOf<BibleVersion>())
    val abbreviationList get() = bibleVersions.value.mapNotNull { it.abbreviation }
    var selectedVersion: MutableState<String> = mutableStateOf("")
        get() {
            if (field.value.isEmpty()) {
                field.value = abbreviationList.find { it.contains("KJV") } ?: ""
            }
            println("println :: updated selectedVersion $field")
            return field
        }
    var selectedChapter = mutableStateOf(-1)
}