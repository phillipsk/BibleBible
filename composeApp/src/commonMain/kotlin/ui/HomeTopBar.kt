package ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp

@Composable
fun HomeTopBar(
    abbreviation: MutableState<String> = mutableStateOf(""),
    homeUiState: HomeUiState
) {
    val header = AnnotatedString(
        text = "${abbreviation.value} " +
                "${homeUiState.version}"
    )
    Row {
        Text(text = "BibleBible")
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = header)
    }
}