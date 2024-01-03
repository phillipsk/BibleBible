
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import data.bibleIQ.BibleBook
import data.bibleIQ.getBooks
import data.bibleIQ.getVersions
import ui.BibleAppScreen

@Composable
fun App() {
    val books = remember { mutableStateOf(listOf<BibleBook>()) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(true) {
        getVersions()
        getBooks(books)
    }

    MaterialTheme {
        BibleAppScreen()
    }
}