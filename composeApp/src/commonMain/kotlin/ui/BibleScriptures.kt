package ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import data.api.bible.Chapters
import data.bibleIQ.BibleIQ
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Chapter(
    private val id: String,
    private val b: String,
    private val c: String,
    private val v: String,
    @SerialName("t") val verse: String
) {
    val chapterVerseUUID = id.toLong()
    val bookId = b.toInt()
    val chapterId = c.toInt()
    val verseId = v.toInt()
}

@Composable
fun BibleScriptures() {
    val chapters: Chapters = BibleIQ.chapter.value
    chapters.data?.getOrNull(1)?.data?.content?.let {
        Text(
            it,
            modifier = Modifier.padding(4.dp)
        )
    }
}