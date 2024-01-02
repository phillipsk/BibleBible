import androidx.compose.animation.AnimatedVisibility
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import data.Bible
import data.getBooks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import ui.BibleAppScreen

@Composable
fun App() {
    val books = remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    scope.launch(Dispatchers.IO) {
        getBooks(books)
    }

    MaterialTheme {
        var showContent by remember { mutableStateOf(false) }
//            Text(text = books.value)
        AnimatedVisibility(Bible.responseBibleApi.value.data?.isNotEmpty() == true) {
//        AnimatedVisibility(books.value.isNotEmpty()) {
            /*            LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                        ) {
                            items(books.value) {
                                Text(it.name)
                            }
                        }*/
            BibleAppScreen()
        }
    }
}