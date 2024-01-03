package data.bibleIQ

import androidx.compose.runtime.mutableStateOf
import kotlin.native.concurrent.ThreadLocal

@ThreadLocal
object BibleIQ {
    var books = mutableStateOf(listOf<BibleBook>())
    var bibleVersions = mutableStateOf(listOf<BibleVersion>())
}