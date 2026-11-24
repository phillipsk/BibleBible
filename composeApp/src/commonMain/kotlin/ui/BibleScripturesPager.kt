package ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.BottomSheetScaffoldState
import androidx.compose.material.Divider
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import data.analytics.AnalyticsTracker
import data.appPrefs.ReadingSession
import data.appPrefs.canonicalBookKey
import data.bible.BibleChapterUIState
import data.bible.BibleState
import data.bible.BookData
import data.bible.fetchBibleChapter
import email.kevinphillips.biblebible.isDesktopPlatform
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterialApi::class)
@Composable
internal fun BibleScripturesPager(
    chapters: BibleChapterUIState,
    bibleVersion: String,
    selectedBook: BookData,
    isAISummaryLoading: Boolean,
    showAISummary: Boolean,
    bottomSheetScaffoldState: BottomSheetScaffoldState,
    loadChapter: suspend (BookData, Int, String, Boolean) -> Unit = { book, chapter, version, history ->
        fetchBibleChapter(book, chapter, version, history)
    },
) {
    val bookId = BibleState.getBookOrdinal(selectedBook.remoteKey).coerceAtLeast(1)
    val sameBook = chapters.bookId == bookId
    val chapterCount = if (sameBook) chapters.chapterList?.size?.coerceAtLeast(1) ?: 1 else 1
    val summaryScrollState = rememberScrollState()

    key(selectedBook.bookId, bibleVersion) {
        val scope = rememberCoroutineScope()
        val pagerState = rememberPagerState(
            initialPage = if (sameBook) ((chapters.chapterId ?: 1) - 1).coerceIn(0, chapterCount - 1) else 0,
            pageCount = { chapterCount }
        )

        LaunchedEffect(BibleState.navigationGeneration) {
            if (BibleState.navigationGeneration == 0) return@LaunchedEffect
            val requestedChapter = BibleState.selectedChapter.substringAfter('.').toIntOrNull() ?: return@LaunchedEffect
            if (BibleState.selectedBook.bookId == selectedBook.bookId) {
                val targetPage = (requestedChapter - 1).coerceIn(0, chapterCount - 1)
                if (pagerState.currentPage != targetPage) {
                    pagerState.scrollToPage(targetPage)
                }
            }
        }

        // One request per settled navigation. Recreating an already loaded reader
        // (rotation, foregrounding) must not reload chapter 1 or add history.
        LaunchedEffect(selectedBook.bookId, bibleVersion, pagerState.settledPage) {
            val chapter = pagerState.settledPage + 1
            ReadingSession.updateLocation(canonicalBookKey(selectedBook), chapter, bibleVersion)
            AnalyticsTracker.track(
                name = "chapter_page_changed",
                category = "reading",
                properties = mapOf(
                    "bookKey" to canonicalBookKey(selectedBook),
                    "bookName" to (selectedBook.name ?: canonicalBookKey(selectedBook)),
                    "chapter" to chapter.toString(),
                    "version" to bibleVersion,
                ),
            )
            val loaded = sameBook && chapters.chapterId == chapter &&
                chapters.version.equals(bibleVersion, ignoreCase = true)
            if (!loaded) {
                val newLocation = !sameBook || chapters.chapterId != chapter
                loadChapter(selectedBook, chapter, bibleVersion, newLocation)
            }
        }

        LaunchedEffect(isAISummaryLoading, showAISummary, pagerState.settledPage) {
            if (bottomSheetScaffoldState.bottomSheetState.isExpanded) {
                bottomSheetScaffoldState.bottomSheetState.collapse()
            }
        }

        Column(Modifier.fillMaxSize()) {
            if (sameBook && !chapters.chapterList.isNullOrEmpty()) {
                val chapterListState = rememberLazyListState()
                val density = LocalDensity.current
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .background(MaterialTheme.colors.surface),
                ) {
                    val viewportPx = with(density) { maxWidth.toPx() }
                    val itemWidthPx = with(density) { 42.dp.toPx() }
                    val selectedIdx = pagerState.currentPage.coerceIn(0, chapterCount - 1)

                    // Re-center the active chapter on page changes AND orientation/width changes (landscape <-> portrait)
                    LaunchedEffect(selectedIdx, chapterCount, maxWidth) {
                        val centerOffset = -((viewportPx - itemWidthPx) / 2f).toInt()
                        chapterListState.animateScrollToItem(index = selectedIdx, scrollOffset = centerOffset)
                    }

                    LazyRow(
                        state = chapterListState,
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        itemsIndexed(chapters.chapterList) { index, chapter ->
                            val isSelected = selectedIdx == index
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .then(
                                        if (isSelected) {
                                            Modifier.border(2.dp, MaterialTheme.colors.primary, CircleShape)
                                        } else {
                                            Modifier
                                        }
                                    )
                                    .clickable {
                                        AnalyticsTracker.track(
                                            "chapter_tab_clicked",
                                            "reading",
                                            mapOf(
                                                "bookKey" to canonicalBookKey(selectedBook),
                                                "chapter" to chapter.toString(),
                                            ),
                                        )
                                        scope.launch { pagerState.animateScrollToPage(index) }
                                    },
                            ) {
                                Text(
                                    text = chapter.toString(),
                                    color = if (isSelected) Color.White else Color.White.copy(alpha = 0.78f),
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }
                }
                Divider(
                    color = Color(0x33FFFFFF),
                    thickness = 1.dp,
                )
            }

            val scriptureScrollState = key(chapters.bookId, chapters.chapterId, bibleVersion) {
                rememberScrollState()
            }
            val readingState = key(chapters.bookId, chapters.chapterId, bibleVersion) {
                val savedPosition = ReadingSession.current?.takeIf {
                    !it.isHome && it.bookKey == canonicalBookKey(selectedBook) &&
                        it.chapter == chapters.chapterId && it.version.equals(bibleVersion, true)
                }
                rememberScriptureReadingState(
                    savedPosition?.let { ScriptureAnchor(it.characterOffset, it.lineFraction) } ?: ScriptureAnchor()
                )
            }
            LaunchedEffect(readingState, selectedBook.bookId, chapters.chapterId, bibleVersion) {
                snapshotFlow { readingState.anchor }.collect { anchor ->
                    ReadingSession.updateAnchor(
                        canonicalBookKey(selectedBook), chapters.chapterId ?: 1, bibleVersion,
                        anchor.character, anchor.lineFraction
                    )
                }
            }
            DisposableEffect(readingState, scriptureScrollState, selectedBook.bookId, chapters.chapterId, bibleVersion) {
                val unregister = ReadingSession.registerAnchorCapture {
                    readingState.capture(scriptureScrollState.value)
                    ReadingSession.updateAnchor(
                        canonicalBookKey(selectedBook), chapters.chapterId ?: 1, bibleVersion,
                        readingState.anchor.character, readingState.anchor.lineFraction
                    )
                }
                onDispose { unregister() }
            }
            // Scripture and AI summary positions are deliberately separate.
            LaunchedEffect(scriptureScrollState) {
                scriptureScrollState.interactionSource.interactions.collect {
                    bottomSheetScaffoldState.bottomSheetState.collapse()
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.Top,
                userScrollEnabled = !isDesktopPlatform()
            ) { page ->
                val targetChapter = page + 1
                val pageMode = when {
                    !sameBook || chapters.chapterId != targetChapter || chapters.text == null -> "loading"
                    isAISummaryLoading && showAISummary -> "summary_loading"
                    showAISummary -> "summary"
                    else -> "scripture"
                }
                Crossfade(
                    targetState = pageMode,
                    animationSpec = tween(durationMillis = 180),
                    modifier = Modifier.fillMaxSize(),
                    label = "chapter_page_crossfade",
                ) { mode ->
                    when (mode) {
                        "loading" -> ScriptureShimmerLoading()
                        "summary_loading" -> AiSummaryLoadingView()
                        "summary" -> GeminiSummary(summaryScrollState, BibleState.selectedFontSize)
                        else -> BibleScriptures(
                            chapters,
                            scriptureScrollState,
                            BibleState.selectedFontSize,
                            onFontSizeChanged = { BibleState.selectedFontSize = it },
                            readingState = readingState
                        )
                    }
                }
            }
        }
    }
}
