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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import data.GeminiModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun GeminiSummary(scrollState: ScrollState, selectedFontSize: Float) {
    val refreshScope = rememberCoroutineScope()
    val state = rememberPullRefreshState(GeminiModel.isLoading, {
        refreshScope.launch {
            GeminiModel.generateAISummary(pullToRefresh = true)
            scrollState.scrollTo(0)
        }
    })

    Box(Modifier.pullRefresh(state)) {
        GeminiModel.geminiDataText?.let { content ->
            Column(modifier = Modifier.verticalScroll(scrollState)) {
                Text(text = content, fontSize = selectedFontSize.sp, modifier = Modifier.padding(4.dp))
            }
        }
        PullRefreshIndicator(GeminiModel.isLoading, state, Modifier.align(Alignment.TopCenter))
    }
}
