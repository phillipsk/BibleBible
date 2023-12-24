package ui
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.BottomNavigation
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import data.Bible
import data.Book

@Composable
fun BookCategory(title: String, books: List<Book>, categoryColor: Color) {
    Column(modifier = Modifier.padding(8.dp)) {
        Text(
            text = title,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colors.onSurface
        )
        Spacer(modifier = Modifier.height(4.dp))
        for (book in books) {
            Card(
                backgroundColor = categoryColor,
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                Text(
                    text = book.name,
                    modifier = Modifier.padding(8.dp)
                )
            }
        }
    }
}

@Composable
fun BibleBookList() {
    val oldTestamentBooks = Bible.books.value.subList(0,39)
    val newTestamentBooks = Bible.books.value.subList(39,Bible.books.value.size)

    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        BookCategory("Old Testament", oldTestamentBooks, Color(0xFFE3F2FD))
        BookCategory("New Testament", newTestamentBooks, Color(0xFFFFF3E0))
    }
}

@Composable
private fun BottomNavigationBarOld() {
    BottomNavigation(
        backgroundColor = MaterialTheme.colors.primary,
        contentColor = Color.White
    ) {
        BottomNavigationItem(
            icon = { Icon(Icons.Filled.Home, contentDescription = "Home") },
            selected = true,
            onClick = {}
        )
        BottomNavigationItem(
            icon = { Icon(Icons.Filled.Search, contentDescription = "Search") },
            selected = false,
            onClick = {}
        )
        BottomNavigationItem(
            icon = { Icon(Icons.Filled.Favorite, contentDescription = "Favorites") },
            selected = false,
            onClick = {}
        )
        BottomNavigationItem(
            icon = { Icon(Icons.Filled.Settings, contentDescription = "Settings") },
            selected = false,
            onClick = {}
        )
    }
}

@Composable
private fun BottomNavigationBar() {
    val items = listOf("Home", "Search", "Favorites", "Settings")
    val icons = listOf(Icons.Default.Home, Icons.Default.Search, Icons.Default.Favorite, Icons.Default.Settings)
    BottomNavigation(
        backgroundColor = Color.White,
        contentColor = Color.Black
    ) {
        items.forEachIndexed { index, item ->
            BottomNavigationItem(
                icon = { Icon(icons[index], contentDescription = item) },
                label = { Text(item) },
                selected = index == 0,
                onClick = { /* Handle navigation */ }
            )
        }
    }
}

@Composable
fun BibleAppScreen() {
    Scaffold(
        bottomBar = { BottomNavigationBar() }
    ) {
        BibleBookList()
    }
}

//@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    MaterialTheme {
        BibleAppScreen()
    }
}
