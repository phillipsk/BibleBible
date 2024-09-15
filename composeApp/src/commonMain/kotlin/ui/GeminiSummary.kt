package ui

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Text
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import data.GeminiModel
import data.GeminiModel.showSummary
import data.bibleIQ.BibleIQDataModel
import io.github.aakira.napier.Napier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun GeminiSummary(scrollState: ScrollState, selectedFontSize: Float) {
    var content by remember { mutableStateOf(GeminiModel.geminiDataText) }
    val refreshScope = rememberCoroutineScope()
    var refreshing by remember(content) { mutableStateOf(false) }

    fun refresh(pullToRefresh: Boolean = false) = refreshScope.launch {
        refreshing = true
        if (pullToRefresh) {
            GeminiModel.generateAISummary(pullToRefresh = true)
        }
        withContext(Dispatchers.Main) {
            if (GeminiModel.geminiDataText?.isEmpty() == false) {
                content = GeminiModel.geminiDataText

            } else {
                Napier.e("showSummary :: $showSummary :: AI Summary could not connect. Please try again later.", tag = "Gem7624")
                BibleIQDataModel.updateErrorSnackBar("AI Summary could not connect. Please try again later.")
//                BibleIQDataModel.showHomePage = true
                GeminiModel.showSummary = false
            }
            scrollState.scrollTo(0)
            refreshing = false
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            GeminiModel.showSummary = false
        }
    }

    val state = rememberPullRefreshState(refreshing, { refresh(pullToRefresh = true) })
    LaunchedEffect(true) {
        Napier.d("GeminiSummary: LaunchedEffect", tag = "Gem7624")
        refresh()
    }

    Box(Modifier.pullRefresh(state)) {
        Napier.d("GeminiSummary: refreshing $refreshing", tag = "GeminiSummary")
        PullRefreshIndicator(refreshing, state, Modifier.align(Alignment.TopCenter))
        content?.let {
            Column(modifier = Modifier.verticalScroll(scrollState)) {
                Text(
                    text = it,
                    fontSize = selectedFontSize.sp,
                    modifier = Modifier.padding(4.dp)
                )
            }
        }
    }
}