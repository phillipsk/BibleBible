package ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.FilterChip
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Done
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import data.GeminiModel
import data.apiBible.BookData
import data.bibleIQ.BibleIQDataModel
import data.bibleIQ.BibleIQVersions
import io.github.aakira.napier.Napier
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource

@OptIn(ExperimentalResourceApi::class)
@Composable
internal fun HomeTopBar(onClick: () -> Unit, generateAISummary: () -> Unit) {
    TopAppBar(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable(onClick = onClick)
                    .weight(1f)
            ) {
                Image(
                    painter = painterResource("BibleBible_ico_iv.png"),
                    contentDescription = "BibleBible",
                    modifier = Modifier.padding(4.dp).clip(RoundedCornerShape(20.dp))
                )
                Spacer(modifier = Modifier.padding(4.dp))

                Text(
                    text = if (BibleIQDataModel.showHomePage) {
                        "BibleBible"
                    } else {
                        BibleIQDataModel.selectedBook.abbreviation.toString()
                    },
                    style = TextStyle(
                        fontFamily = FontFamily.Cursive,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.1.sp,
                        color = Color.White,
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Visible,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 4.dp, end = 4.dp)
                )

            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(end = 2.dp).wrapContentWidth()
            ) {
                if (!BibleIQDataModel.showHomePage) {
                    GenerateAISummaryButton(
                        generateAISummary,
                        !GeminiModel.isLoading && GeminiModel.showSummary
                    )
                    Spacer(modifier = Modifier.padding(horizontal = 8.dp))
                    BibleMenu(
                        bibleVersionsList = BibleIQDataModel.bibleVersions
                    )
                } else {
                    SortBibleBooksToggle()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun GenerateAISummaryButton(generateAISummary: () -> Unit, isAISummaryLoading: Boolean) {
    val scope = rememberCoroutineScope()
    var selected = remember { GeminiModel.showSummary }

    FilterChip(
        onClick = {
            scope.launch {
                generateAISummary()
                selected = !selected
            }
        },
        selected = selected,
        leadingIcon = if (isAISummaryLoading) {
            {
                Icon(
                    imageVector = Icons.Filled.Done,
                    contentDescription = "Done icon",
                    modifier = Modifier.size(12.dp)
                )
            }
        } else {
            null
        },
    ) {
        Text("AI")
    }
}

@Composable
internal fun SortBibleBooksToggle() {
    var checked by remember { mutableStateOf(!BibleIQDataModel.sortAZ) }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(end = 2.dp).wrapContentWidth()
    ) {
        Switch(
            checked = checked,
            onCheckedChange = {
                BibleIQDataModel.sortAZ = !BibleIQDataModel.sortAZ
                checked = it
            }
        )
        Text(
            text = AnnotatedString(BibleIQDataModel.selectedSortType),
            style = MaterialTheme.typography.subtitle1.copy(fontSize = 14.sp, color = Color.White),
        )
    }
}

@Composable
internal fun BookMenu(bookDataList: List<BookData>?) {
    var expanded by remember { mutableStateOf(false) }
    var selectedBookData by remember(BibleIQDataModel.selectedBook.abbreviation) {
        mutableStateOf(
            BibleIQDataModel.selectedBook
        )
    }
    val rotationAngle by animateFloatAsState(
        targetValue = if (expanded) 90f else 0f,
        animationSpec = tween(300)
    )
    Napier.v("BookMenu: selectedBookData: ${selectedBookData.abbreviation}", tag = "IQ2455")
    Napier.v(
        "BookMenu: BibleIQDataModel selectedBookData: ${BibleIQDataModel.selectedBook.abbreviation}",
        tag = "IQ2455"
    )
    ClickableText(
        text = AnnotatedString(selectedBookData.abbreviation ?: "Gen"),
        style = MaterialTheme.typography.subtitle1.copy(fontSize = 14.sp, color = Color.White),
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
                BibleIQDataModel.run {
                    updateSelectedBook(it)
                }
                selectedBookData = it
            }) {
                Text("${it.abbreviation} ")
            }
        }
    }
}

@Composable
internal fun BibleMenu(bibleVersionsList: BibleIQVersions) {
    val scope = rememberCoroutineScope()
    var expanded by remember { mutableStateOf(false) }
    val rotationAngle by animateFloatAsState(
        targetValue = if (expanded) 90f else 0f,
        animationSpec = tween(300)
    )
    var selectedBibleVersion by remember { mutableStateOf(BibleIQDataModel.selectedVersion) }
    LaunchedEffect(true) {
        Napier.v("LaunchedEffect :: BibleMenu", tag = "IQ2455")
    }
    ClickableText(
        text = AnnotatedString(selectedBibleVersion),
        style = MaterialTheme.typography.subtitle1.copy(fontSize = 14.sp, color = Color.White),
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
        bibleVersionsList.data.forEach {
            if (it.abbreviation != null) {
                DropdownMenuItem(onClick = {
                    BibleIQDataModel.run {
                        updateSelectedVersion(it.abbreviation)
                    }
                    selectedBibleVersion = it.abbreviation ?: ""
                    expanded = false
                }) {
                    Text("${it.uiAbbreviation} ")
                }
            }
        }
    }
}