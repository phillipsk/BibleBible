package ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.BottomNavigation
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

val LightBlue = Color(0xFFD7E3FC)
val LightOrange = Color(0xFFFFE0B2)
val LightGreen = Color(0xFFE8F5E9)
val LightRed = Color(0xFFFFCDD2)
// ... Define other colors for different categories

val MyThemeColors = lightColors(
    primary = LightBlue,
    primaryVariant = LightGreen,
    secondary = LightOrange
    // ... Set other theme colors as needed
)
@Composable
fun MyAppScreen() {
    MaterialTheme(colors = MyThemeColors) {
        Scaffold(
            topBar = { MyTopBar() },
            content = { MyContent() },
            bottomBar = { MyBottomBar() }
        )
    }
}

@Composable
fun MyTopBar() {
    TopAppBar(
        title = { Text("Bible") },
        backgroundColor = MaterialTheme.colors.primary
    )
}

@Composable
fun MyContent() {
    // Here you would define the content of your app, for example, a LazyColumn for a scrolling list
    LazyColumn {
        item { TestamentSection("Old Testament", LightBlue) }
        item { TestamentSection("New Testament", LightGreen) }
        // ... Add other sections
    }
}

@Composable
fun TestamentSection(testament: String, backgroundColor: Color) {
    Column(modifier = Modifier.background(backgroundColor).fillMaxWidth()) {
        Text(
            text = testament,
            style = MaterialTheme.typography.h5,
            modifier = Modifier.padding(16.dp)
        )
        // Here you would add the books of the testament
        // For example, you can use a LazyRow or a Grid to display book items
    }
}

@Composable
fun MyBottomBar() {
    BottomNavigation(backgroundColor = MaterialTheme.colors.primary) {
        // Define the items in the bottom bar
    }
}

@Composable
fun BookItem(book: String, color: Color) {
    Card(
        backgroundColor = color,
        modifier = Modifier.padding(8.dp)
    ) {
        Text(
            text = book,
            style = MaterialTheme.typography.subtitle1,
            modifier = Modifier.padding(16.dp)
        )
    }
}

//@Preview
@Composable
fun MyAppScreenPreview() {
    MyAppScreen()
}

