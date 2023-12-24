import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch

@Composable
fun App() {
    val books = remember { mutableStateOf(listOf<Book>()) }
    val scope = rememberCoroutineScope()

    scope.launch(Dispatchers.IO) {
        getBooks(books)
    }

    MaterialTheme {
        var showContent by remember { mutableStateOf(false) }
        AnimatedVisibility(books.value.isNotEmpty()) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
            ) {
                items(books.value) {
                    Text(it.name)
                }
            }
        }
    }
}