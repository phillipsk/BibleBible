package ui.configs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.size
import androidx.compose.material.ChipDefaults
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.FilterChip
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.runtime.Composable
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
    isAISummarySuccessful: Boolean,
    showSummary: Boolean,
) {
    val scope = rememberCoroutineScope()

    val goldAccent = Color(0xFFFBD38D)
    val enabled = GeminiModel.isAvailable && (!GeminiModel.isLoading || showSummary)
    val isLoadedActive = isAISummarySuccessful
    val contentTint = when {
        !enabled -> Color.White.copy(alpha = 0.45f)
        isLoadedActive -> goldAccent
        else -> Color.White
    }
    val borderColor = when {
        !enabled -> Color(0x28FFFFFF)
        isLoadedActive -> goldAccent
        else -> Color(0x66FFFFFF)
    }
    Napier.d("GeminiModel.showSummary ${GeminiModel.showSummary}", tag = "Gemini")
    FilterChip(
        enabled = enabled,
        border = BorderStroke(
            width = 1.5.dp,
            color = borderColor,
        ),
        colors = ChipDefaults.filterChipColors(
            backgroundColor = Color(0x16FFFFFF),
            selectedBackgroundColor = if (isLoadedActive) Color(0x2AFBD38D) else Color(0x2EFFFFFF),
            contentColor = contentTint,
            selectedContentColor = contentTint,
            leadingIconColor = contentTint,
            selectedLeadingIconColor = contentTint,
            disabledBackgroundColor = Color(0x0AFFFFFF),
            disabledContentColor = Color.White.copy(alpha = 0.45f),
        ),
        onClick = {
            scope.launch {
                generateAISummary()
            }
        },
        selected = showSummary,
        leadingIcon = if (isAISummarySuccessful) {
            {
                Icon(
                    imageVector = Icons.Filled.Done,
                    contentDescription = "Done icon",
                    tint = contentTint,
                    modifier = Modifier.size(12.dp)
                )
            }
        } else {
            null
        },
    ) {
        Text(
            text = "AI",
            style = MaterialTheme.typography.subtitle1.copy(
                fontSize = 12.sp,
                color = contentTint,
            )
        )
    }
}
