package ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Card
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import data.GeminiModel
import data.analytics.AnalyticsTracker
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
            AnalyticsTracker.trackAiSummary(
                "ai_summary_pull_refresh",
                mapOf(
                    "bookName" to (BibleIQDataModel.selectedBook.cleanedName ?: ""),
                    "chapter" to (BibleIQDataModel.bibleChapter?.chapterId?.toString() ?: ""),
                ),
            )
            GeminiModel.generateAISummary(pullToRefresh = true)
        }
        withContext(Dispatchers.Main) {
            if (GeminiModel.geminiDataText?.isEmpty() == false) {
                content = GeminiModel.geminiDataText
            } else {
                BibleIQDataModel.updateErrorSnackBar(
                    GeminiModel.errorMessage ?: "Gemini returned no summary. Please try again."
                )
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

    val cardBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF19304F),
            Color(0xFF1E3859),
            Color(0xFF162942),
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pullRefresh(state)
    ) {
        val summaryText = content
        if (summaryText.isNullOrBlank()) {
            LoadingScreen(message = "Generating AI summary…")
        } else {
            val blocks = remember(summaryText) { parseMarkdownSummaryBlocks(summaryText) }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .navigationBarsPadding()
                    .padding(horizontal = 14.dp, vertical = 14.dp),
            ) {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, Color(0x44FBD38D)),
                    backgroundColor = Color(0xFF19304F),
                    elevation = 4.dp,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(cardBrush)
                            .padding(horizontal = 20.dp, vertical = 18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = Color(0x2AFBD38D),
                                elevation = 0.dp,
                            ) {
                                Text(
                                    text = "AI Chapter Summary",
                                    color = Color(0xFFFBD38D),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.6.sp,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                )
                            }
                            val chapterNum = BibleIQDataModel.bibleChapter?.chapterId
                            val bookName = BibleIQDataModel.selectedBook.cleanedName.orEmpty()
                            if (bookName.isNotBlank() && chapterNum != null) {
                                Text(
                                    text = "$bookName $chapterNum",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        blocks.forEach { block ->
                            when (block) {
                                is SummaryBlock.Heading -> {
                                    Text(
                                        text = formatInlineMarkdown(block.text),
                                        color = Color(0xFFFBD38D),
                                        fontSize = (selectedFontSize * 1.05f).coerceIn(17f, 26f).sp,
                                        fontWeight = FontWeight.Bold,
                                        lineHeight = (selectedFontSize * 1.4f).sp,
                                        modifier = Modifier.padding(top = 6.dp),
                                    )
                                }
                                is SummaryBlock.Bullet -> {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        verticalAlignment = Alignment.Top,
                                        modifier = Modifier.padding(start = 4.dp),
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .padding(top = (selectedFontSize * 0.45f).dp)
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFFFBD38D))
                                        )
                                        Text(
                                            text = formatInlineMarkdown(block.text),
                                            color = Color(0xFFEBF2FA),
                                            fontSize = selectedFontSize.sp,
                                            lineHeight = (selectedFontSize * 1.48f).sp,
                                            modifier = Modifier.weight(1f),
                                        )
                                    }
                                }
                                is SummaryBlock.Paragraph -> {
                                    Text(
                                        text = formatInlineMarkdown(block.text),
                                        color = Color(0xFFEBF2FA),
                                        fontSize = selectedFontSize.sp,
                                        lineHeight = (selectedFontSize * 1.5f).sp,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        PullRefreshIndicator(
            refreshing = refreshing,
            state = state,
            contentColor = Color(0xFFFBD38D),
            backgroundColor = Color(0xFF19304F),
            modifier = Modifier.align(Alignment.TopCenter),
        )
    }
}

private sealed interface SummaryBlock {
    data class Heading(val text: String) : SummaryBlock
    data class Bullet(val text: String) : SummaryBlock
    data class Paragraph(val text: String) : SummaryBlock
}

private fun parseMarkdownSummaryBlocks(raw: String): List<SummaryBlock> {
    val result = mutableListOf<SummaryBlock>()
    val lines = raw.replace("\r\n", "\n").split("\n")
    for (line in lines) {
        val trimmed = line.trim()
        if (trimmed.isEmpty()) continue
        when {
            trimmed.startsWith("#") -> {
                val heading = trimmed.trimStart('#').trim()
                if (heading.isNotEmpty()) result.add(SummaryBlock.Heading(heading))
            }
            trimmed.startsWith("* ") || trimmed.startsWith("- ") || trimmed.startsWith("• ") -> {
                result.add(SummaryBlock.Bullet(trimmed.drop(2).trim()))
            }
            trimmed.startsWith("**") && trimmed.endsWith("**") && trimmed.length > 4 &&
                !trimmed.drop(2).dropLast(2).contains("**") -> {
                result.add(SummaryBlock.Heading(trimmed.drop(2).dropLast(2).trim()))
            }
            else -> {
                result.add(SummaryBlock.Paragraph(trimmed))
            }
        }
    }
    return result
}

private fun formatInlineMarkdown(text: String): AnnotatedString = buildAnnotatedString {
    var i = 0
    while (i < text.length) {
        if (i + 1 < text.length && text[i] == '*' && text[i + 1] == '*') {
            val end = text.indexOf("**", startIndex = i + 2)
            if (end > i + 2) {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = Color.White)) {
                    append(text.substring(i + 2, end))
                }
                i = end + 2
                continue
            }
        }
        if (text[i] == '*') {
            val end = text.indexOf('*', startIndex = i + 1)
            if (end > i + 1) {
                withStyle(SpanStyle(fontStyle = FontStyle.Italic, color = Color(0xFFCBD5E1))) {
                    append(text.substring(i + 1, end))
                }
                i = end + 1
                continue
            }
        }
        append(text[i])
        i++
    }
}
