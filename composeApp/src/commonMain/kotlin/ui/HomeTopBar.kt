package ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import data.apiBible.BibleAPIBibles
import data.apiBible.BibleAPIDataModel
import data.apiBible.BookData
import data.apiBible.getChapterBibleAPI
import io.github.aakira.napier.Napier
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource

@OptIn(ExperimentalResourceApi::class, ExperimentalMaterial3Api::class)
@Composable
internal fun HomeTopBar(onClick: () -> Unit = {}) {
    CenterAlignedTopAppBar(
        title = {
            // Title content goes here, you can adjust or add more content
            Text(text = "BibleBible", color = Color.White)
        },
        navigationIcon = {
            // Navigation icon content goes here, e.g., a back arrow or a menu icon
            IconButton(onClick = { /* do something */ }) {
                Icon(
                    painter = painterResource("BibleBible_ico_iv.png"),
                    contentDescription = "BibleBible",
                    modifier = Modifier.padding(4.dp).clip(RoundedCornerShape(20.dp))
                )
            }
        },
        actions = {
            // Actions content goes here, e.g., icons on the right side of the top bar
            BookMenu(
                selectedBookData = BibleAPIDataModel.selectedBookData,
                bookDataList = BibleAPIDataModel.books.data
            )
            BibleMenu(
                bibleVersionsList = BibleAPIDataModel.abbreviationList
            )
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
internal fun BookMenu(selectedBookData: BookData, bookDataList: List<BookData>?) {
    val scope = rememberCoroutineScope()
    var expanded by remember { mutableStateOf(false) }
    val rotationAngle by animateFloatAsState(
        targetValue = if (expanded) 90f else 0f,
        animationSpec = tween(300)
    )
    ClickableText(
        text = AnnotatedString(selectedBookData.abbreviation ?: "Gen"),
        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp, color = Color.White),
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
                scope.launch {
                    Napier.v("scope launch", tag = "BB2455")
                    getChapterBibleAPI(
                        chapterNumber = it.remoteKey,
                        bibleId = BibleAPIDataModel.selectedBibleId
                    )
                    Napier.v("scope middle", tag = "BB2455")
                    BibleAPIDataModel.run {
                        updateBookData(it)
                        updateSelectedChapter(it.remoteKey)
                    }
                    Napier.v("scope end", tag = "BB2455")
                }
            }, text = {
                Text("${it.abbreviation} ")
            })
        }
    }
}

@Composable
internal fun BibleMenu(bibleVersionsList: List<BibleAPIBibles.BibleAPIVersion>) {
    val scope = rememberCoroutineScope()
    var expanded by remember { mutableStateOf(false) }
    val rotationAngle by animateFloatAsState(
        targetValue = if (expanded) 90f else 0f,
        animationSpec = tween(300)
    )
    ClickableText(
        text = AnnotatedString(BibleAPIDataModel.selectedVersion),
        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp, color = Color.White),
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
                    scope.launch {
//                        getBooksBibleAPI()
                        getChapterBibleAPI(
                            chapterNumber = BibleAPIDataModel.selectedChapter,
                            bibleId = it.id
                        )
                        BibleAPIDataModel.run {
                            updateSelectedVersion(it.abbreviationLocal)
                            updateSelectedBibleId(it.id)
                        }
                    }
                    expanded = false
                }, text = {
                    Text("${it.nameLocalCleaned} ")
                })
            }
        }
    }
}