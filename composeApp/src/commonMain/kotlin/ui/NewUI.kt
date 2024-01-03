package ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyHorizontalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.BottomNavigation
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import data.bibleIQ.BibleIQ
import data.bibleIQ.Book

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
                book.name?.let {
                    Text(
                        text = it,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        }
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
    val icons = listOf(
        Icons.Default.Home,
        Icons.Default.Search,
        Icons.Default.Favorite,
        Icons.Default.Settings
    )
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
        bottomBar = { /*BottomNavigationBar() */}
    ) {
        var showContent by remember { mutableStateOf(false) }
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            BibleVersions()
            BibleBookList()
        }
    }
}

@Composable
private fun BibleVersions() {
    with(BibleIQ.bibleVersions.value) {
        AnimatedVisibility(this.isNotEmpty()) {
            LazyRow(contentPadding = PaddingValues(10.dp)) {
                items(items = this@with) { bibleVersion ->
                    bibleVersion.abbreviation?.let { abbreviation ->
                        Button(
                            onClick = { /* TODO: Handle click */ },
                            shape = RoundedCornerShape(50), // Rounded corners
                            colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.primary),
                            modifier = Modifier
                                .padding(4.dp) // Add padding around the Button
                                .height(40.dp) // Fixed height for buttons
                        ) {
                            Text(
                                text = abbreviation,
                                fontSize = 14.sp,
                                color = MaterialTheme.colors.onPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BibleBookList() {
    with(BibleIQ.books.value) {
        AnimatedVisibility(this.isNotEmpty()) {
            LazyHorizontalStaggeredGrid(rows = StaggeredGridCells.Adaptive(50.dp), contentPadding = PaddingValues(10.dp)) {
                items(items = this@with) { book ->
                    book.name?.let { bookName ->
                        Button(
                            onClick = { /* TODO: Handle click */ },
                            shape = RoundedCornerShape(50), // Rounded corners
                            colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.primary),
                            modifier = Modifier
                                .padding(4.dp) // Add padding around the Button
                                .height(40.dp) // Fixed height for buttons
                        ) {
                            Text(
                                text = bookName,
                                fontSize = 14.sp,
                                color = MaterialTheme.colors.onPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}

//@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    MaterialTheme {
        BibleAppScreen()
    }
}
