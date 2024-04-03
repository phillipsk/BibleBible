package ui.configs

import androidx.compose.foundation.layout.size
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.FilterChip
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import data.GeminiModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun AISummaryButton(generateAISummary: () -> Unit, isAISummaryLoading: Boolean) {
    val scope = rememberCoroutineScope()
    val checked = remember(isAISummaryLoading) { GeminiModel.showSummary }

    FilterChip(
        onClick = {
            scope.launch {
                generateAISummary()
            }
        },
        selected = checked,
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