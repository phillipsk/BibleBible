package data.bibleIQ

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import kotlin.native.concurrent.ThreadLocal

@ThreadLocal
object BibleIQ {

    var books = mutableStateOf(listOf<BibleBook>())
    var bibleVersions = mutableStateOf(listOf<BibleVersion>())
    val abbreviationList get() = bibleVersions.value.mapNotNull { it.abbreviation }
    var selectedVersion: MutableState<String> = mutableStateOf("")
        get() {
            if (field.value.isEmpty()) {
                field.value = abbreviationList.find { it.contains("KJV") } ?: ""
            }
            return field
        }
        private set
}