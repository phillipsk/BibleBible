package ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
    LazyColumn(contentPadding = PaddingValues(4.dp)) {
        val chapters: List<Chapter> = BibleIQ.chapter.value
        items(chapters) {
            Text(it.verse, modifier = Modifier.padding(4.dp))
        }
    }
}