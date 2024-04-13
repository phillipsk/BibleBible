package ui.configs.home

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Switch
import androidx.compose.material.SwitchDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.sp
import data.bibleIQ.BibleIQDataModel
import data.gemini.checkAnimationLastCalled
import io.github.aakira.napier.Napier
import kotlinx.coroutines.delay

@Composable
internal fun HomeTopBar(onClick: () -> Unit = {}) {
    var showSubtitle by mutableStateOf(false)
    var animationCounter by mutableStateOf(0)

    suspend fun animateSubtitle() {
        try {
            val showAnimation = checkAnimationLastCalled()
            Napier.d("HomeTopBar: showAnimation $showAnimation", tag = "HomeTopBar")
            if (showAnimation) {
                listOf(1000L, 700L, 2000L, 50L).forEach { delay ->
                    showSubtitle = !showSubtitle
                    delay(delay)
                    animationCounter++
                }
            }
        } catch (e: Exception) {
            Napier.e("Error in animateSubtitle: ${e.message}", tag = "HomeTopBar")
        }
    }

    LaunchedEffect(Unit) {
//        animateSubtitle()
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        HomeTitle(onClick)
        Napier.d("HomeTopBar: showSubtitle $showSubtitle", tag = "HomeTopBar")
        Spacer(modifier = Modifier.weight(1f))
        if (showSubtitle) {
            AnimatedSubtitle(showSubtitle, animationCounter)
        } else {
            SortBibleBooksToggle()
        }
    }
}

@Composable
internal fun SortBibleBooksToggle() {
    var checked by remember { mutableStateOf(!BibleIQDataModel.sortAZ) }
    Switch(
        checked = checked,
        onCheckedChange = {
            BibleIQDataModel.sortAZ = !BibleIQDataModel.sortAZ
            checked = it
        },
        colors = SwitchDefaults.colors(
            checkedThumbColor = Color.White,
            checkedTrackColor = Color.White.copy(alpha = 0.5f),
            uncheckedThumbColor = Color.White,
            uncheckedTrackColor = Color.White.copy(alpha = 0.5f)
        )
    )
    Text(
        text = AnnotatedString(BibleIQDataModel.selectedSortType),
        style = MaterialTheme.typography.subtitle2.copy(fontSize = 12.sp, color = Color.White),
    )
}