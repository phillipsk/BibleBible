package ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import data.bibleIQ.BibleIQ
import data.bibleIQ.BibleIQ.books
import data.bibleIQ.BibleIQ.selectedChapter
import data.bibleIQ.BibleVersion
import data.bibleIQ.getChapter
import kotlinx.coroutines.launch

val homeUiState get() = HomeUiState()

fun updateAbbreviation(abv: String) {
    homeUiState.version = BibleVersion(abbreviation = abv)
}

@Composable
fun BibleHomeScreen() {
    Scaffold(topBar = { HomeTopBar() }) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            BibleBookList()
            BibleScriptures()
        }
    }
}

@Composable
private fun BibleVersions(onAbbreviationSelected: (String) -> Unit = {}) {
    var selectedAbbreviation by remember { mutableStateOf<String?>(null) }

    // Assume BibleIQ.bibleVersions.value is a list of BibleVersion objects
    val bibleVersions = BibleIQ.bibleVersions.value

    // Only show the LazyRow if there's no selected abbreviation or the list is not empty
    AnimatedVisibility(visible = selectedAbbreviation == null && bibleVersions.isNotEmpty()) {
        LazyRow(contentPadding = PaddingValues(10.dp)) {
            items(items = bibleVersions) { bibleVersion ->
                bibleVersion.abbreviation?.let { abbreviation ->
                    Button(
                        onClick = {
                            selectedAbbreviation = abbreviation
                            onAbbreviationSelected(abbreviation)
                        },
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

@Composable
fun BibleBookList() {
    val scope = rememberCoroutineScope()
    AnimatedVisibility(books.value.isNotEmpty() && selectedChapter.value == -1) {
        Column(modifier = Modifier.padding(4.dp)) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(10.dp),
                userScrollEnabled = true,
            ) {
                items(items = books.value) {
                    it.let {
                        Button(
                            onClick = {
                                selectedChapter.value = it.bookId
                                scope.launch {
                                    getChapter(it.bookId)
                                }
                            },
                            shape = RoundedCornerShape(50), // Rounded corners
                            colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.primary),
                            modifier = Modifier
                                .padding(2.dp) // Add padding around the Button
                                .height(IntrinsicSize.Min) // Allow the button to expand to fit the text
                                .defaultMinSize(
                                    minWidth = 100.dp,
                                    minHeight = 40.dp
                                ) // Set a minimum size
                        ) {
                            Text(
                                text = it.name,
                                fontSize = 14.sp,
                                color = MaterialTheme.colors.onPrimary,
                                maxLines = 1, // Ensure text does not wrap
                                overflow = TextOverflow.Ellipsis // Use ellipsis for text that is too long
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
        BibleHomeScreen()
    }
}
