package ui.configs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.size
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.FilterChip
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import data.GeminiModel
import io.github.aakira.napier.Napier
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun AISummaryButton(
    generateAISummary: () -> Unit,
    isAISummaryLoading: Boolean,
    isAISummarySuccessful: Boolean,
) {
    val scope = rememberCoroutineScope()
    val checked = remember(isAISummaryLoading) { GeminiModel.showSummary }

    Napier.d("AISummaryButton isAISummaryLoading :: $isAISummaryLoading", tag = "Gemini")
    FilterChip(
        border = if (!checked) BorderStroke(2.dp, MaterialTheme.colors.primary) else null,
        onClick = {
            scope.launch {
                generateAISummary()
            }
        },
        selected = checked,
        leadingIcon = if (isAISummarySuccessful) {
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
        Text(
            text = "AI",
            style = MaterialTheme.typography.subtitle1.copy(fontSize = 12.sp, color = Color.White)
        )
    }
}