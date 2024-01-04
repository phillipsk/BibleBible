package data.bibleIQ

import androidx.compose.runtime.mutableStateOf
import kotlin.native.concurrent.ThreadLocal

@ThreadLocal
object BibleIQ {

    var books = mutableStateOf(listOf<BibleBook>())
    var bibleVersions = mutableStateOf(listOf<BibleVersion>())
    val abbreviationList get() = bibleVersions.value.mapNotNull { it.abbreviation }
    var selectedVersion: String = ""
        get() {
            return field.ifEmpty {
                abbreviationList.find {
                    it.contains("KJV")
                } ?: ""
            }
        }
        set(value) {
            field = value
        }
}