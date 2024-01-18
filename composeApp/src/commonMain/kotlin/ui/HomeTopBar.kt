package ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import data.api.apiBible.BibleAPIDataModel
import data.apiBible.BibleAPIBibles
import data.apiBible.BookData
import data.apiBible.getBooksBibleAPI
import data.apiBible.getChapterBibleAPI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch

@Composable
fun HomeTopBar() {
    TopAppBar(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "BibleBible",
            style = MaterialTheme.typography.h6.copy(fontSize = 20.sp),
            color = MaterialTheme.colors.onSurface,
            modifier = Modifier.weight(1f)
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(end = 16.dp)
        ) {
            BookMenu(
                selectedBookData = BibleAPIDataModel.selectedBookData,
                bookDataList = BibleAPIDataModel.books.data,
                bibleId = BibleAPIDataModel.selectedBibleId
            )
            BibleMenu(
                bibleVersionsList = BibleAPIDataModel.abbreviationList
            )
        }
    }
}

@Composable
fun BookMenu(selectedBookData: BookData, bookDataList: List<BookData>?, bibleId: String) {
    val scope = rememberCoroutineScope()
    var expanded by remember { mutableStateOf(false) }
    val rotationAngle by animateFloatAsState(
        targetValue = if (expanded) 90f else 0f,
        animationSpec = tween(300)
    )
    ClickableText(
        text = AnnotatedString(selectedBookData.abbreviation ?: "Gen"),
        style = MaterialTheme.typography.subtitle1.copy(fontSize = 16.sp),
        onClick = { expanded = !expanded }
    )
    IconButton(onClick = { expanded = !expanded }) {
        Icon(
            Icons.Filled.ArrowDropDown,
            contentDescription = "Dropdown Menu",
            modifier = Modifier.graphicsLayer(rotationZ = rotationAngle)
        )
    }
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = { expanded = false }
    ) {
        bookDataList?.forEach {
            DropdownMenuItem(onClick = {
                expanded = false
                BibleAPIDataModel.run {
                    updateBookData(it)
                    updateSelectedChapter(it.key)
                }
                scope.launch {
                    getChapterBibleAPI(
                        chapterNumber = selectedBookData.key,
                        bibleId = bibleId
                    )
                }
            }) {
                Text("${it.abbreviation} ")
            }
        }
    }
}

@Composable
fun BibleMenu(bibleVersionsList: List<BibleAPIBibles.BibleAPIVersion>) {
    val scope = rememberCoroutineScope { Dispatchers.IO }
    var expanded by remember { mutableStateOf(false) }
    val rotationAngle by animateFloatAsState(
        targetValue = if (expanded) 90f else 0f,
        animationSpec = tween(300)
    )
    ClickableText(
        text = AnnotatedString(BibleAPIDataModel.selectedVersion),
        style = MaterialTheme.typography.subtitle1.copy(fontSize = 16.sp),
        onClick = { expanded = !expanded }
    )
    IconButton(onClick = { expanded = !expanded }) {
        Icon(
            Icons.Filled.ArrowDropDown,
            contentDescription = "Dropdown Menu",
            modifier = Modifier.graphicsLayer(rotationZ = rotationAngle)
        )
    }
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = { expanded = false }
    ) {
        bibleVersionsList.forEach {
            if (it.abbreviationLocal != null && it.id != null) {
                DropdownMenuItem(onClick = {
                    BibleAPIDataModel.run {
                        updateSelectedVersion(it.abbreviationLocal)
                        updateSelectedBibleId(it.id)
                    }
                    scope.launch {
                        getBooksBibleAPI()
                    }
                    expanded = false
                }) {
                    Text("${it.nameLocalCleaned} ")
                }
            }
        }
    }
}