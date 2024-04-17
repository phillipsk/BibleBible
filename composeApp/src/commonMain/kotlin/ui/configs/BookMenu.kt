package ui.configs

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.sp
import data.apiBible.BookData
import data.bibleIQ.BibleIQDataModel
import io.github.aakira.napier.Napier

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
    Row(modifier = Modifier, verticalAlignment = Alignment.CenterVertically) {
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
}