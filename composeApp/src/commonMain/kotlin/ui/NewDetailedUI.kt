package ui
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.unit.sp

@Composable
fun BibleBookCard(bookName: String, color: Color) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp),
        backgroundColor = color,
        elevation = 4.dp
    ) {
        Text(
            text = bookName,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun TestamentSection(testament: String, books: List<String>, colors: List<Color>) {
    Column(modifier = Modifier.padding(8.dp)) {
        Text(
            text = testament,
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            modifier = Modifier.padding(start = 8.dp, top = 8.dp, bottom = 4.dp)
        )
        books.forEachIndexed { index, book ->
            BibleBookCard(book, colors[index % colors.size])
        }
    }
}

@Composable
private fun BibleBooksScreen() {
    val oldTestamentBooks = listOf("Genesis", "Exodus", "Leviticus", "Numbers", "Deuteronomy")
    val newTestamentBooks = listOf("Matthew", "Mark", "Luke", "John", "Acts")
    val colors = listOf(Color(0xFFFFCDD2), Color(0xFFBBDEFB), Color.Magenta)

    Column {
        TestamentSection("Old Testament", oldTestamentBooks, colors)
        TestamentSection("New Testament", newTestamentBooks, colors)
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
fun BibleAppUI() {
    Scaffold(
        bottomBar = { BottomNavigationBar() },
        modifier = Modifier.fillMaxSize()
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            BibleBooksScreen()
        }
    }
}

//@Preview(showBackground = true)
@Composable
fun PreviewBibleAppUI() {
    MaterialTheme {
        BibleAppUI()
    }
}
