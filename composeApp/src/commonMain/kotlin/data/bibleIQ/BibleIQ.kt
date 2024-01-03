package data.bibleIQ

import androidx.compose.runtime.mutableStateOf
import kotlin.native.concurrent.ThreadLocal

@ThreadLocal
object BibleIQ {
    var books = mutableStateOf(listOf<Book>())
    var bibleVersions = mutableStateOf(listOf<BibleVersion>())
}