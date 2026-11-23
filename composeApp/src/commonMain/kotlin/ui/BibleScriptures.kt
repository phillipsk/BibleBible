package ui

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import data.analytics.AnalyticsTracker
import data.appPrefs.updateUserPreferences
import data.bible.BibleChapterUIState
import data.bible.BibleState
import data.config.CloudSessionManager
import data.notifications.findVerseCharacterOffset
import data.notifications.findVerseCharacterRange
import email.kevinphillips.biblebible.isDesktopPlatform
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop

@OptIn(kotlinx.coroutines.FlowPreview::class)
@Composable
internal fun BibleScriptures(
    chapters: BibleChapterUIState,
    scrollState: ScrollState,
    selectedFontSize: Float,
    onFontSizeChanged: (Float) -> Unit,
    readingState: ScriptureReadingState = rememberScriptureReadingState(),
) {
    var previousFontSize by remember { mutableStateOf(selectedFontSize) }
    var textLayout by remember { mutableStateOf<TextLayoutResult?>(null) }
    var viewportSize by remember { mutableStateOf(IntSize.Zero) }
    val currentFontSize by rememberUpdatedState(selectedFontSize)
    val changeFontSize by rememberUpdatedState(onFontSizeChanged)
    readingState.contentInset = with(LocalDensity.current) { 12.dp.roundToPx() }

    LaunchedEffect(scrollState, readingState) {
        var interactionActive = false
        snapshotFlow { scrollState.value to scrollState.isScrollInProgress }.collect { (offset, inProgress) ->
            if (inProgress && !interactionActive) {
                readingState.beginScrollInteraction()
                interactionActive = true
            }
            if (interactionActive) readingState.capture(offset)
            if (!inProgress) interactionActive = false
        }
    }

    // Wait for the scroll container's new range before restoring the visible text.
    // Keep our own restoration out of the capture flow to avoid cumulative drift.
    LaunchedEffect(textLayout, viewportSize, selectedFontSize, readingState.zoomReleaseGeneration, readingState) {
        val layout = textLayout ?: return@LaunchedEffect
        if (BibleState.pendingTargetVerse != null &&
            BibleState.pendingTargetBookId.equals(BibleState.selectedBook.bookId, ignoreCase = true) &&
            BibleState.pendingTargetChapter == chapters.chapterId
        ) {
            return@LaunchedEffect
        }
        // A release can arrive before the final font change has been measured.
        // Only the effect for that measured font may unlock anchor capture.
        if (layout.layoutInput.style.fontSize != selectedFontSize.sp) return@LaunchedEffect
        val viewport = viewportSize
        val zoomGeneration = readingState.zoomReleaseGeneration
        try {
            withFrameNanos { }
            scrollState.scrollTo(readingState.scrollOffset(layout))
            readingState.restoredScrollOffset = scrollState.value
            withFrameNanos { }
        } finally {
            if (textLayout === layout && viewportSize == viewport &&
                zoomGeneration == readingState.zoomReleaseGeneration) {
                readingState.restoring = false
                if (!readingState.zooming) readingState.awaitingZoomRestore = false
            }
        }
    }

    LaunchedEffect(
        textLayout,
        viewportSize,
        chapters.text,
        BibleState.pendingTargetVerse,
        BibleState.pendingTargetBookId,
        BibleState.pendingTargetChapter,
    ) {
        val targetVerse = BibleState.pendingTargetVerse ?: return@LaunchedEffect
        val targetBookId = BibleState.pendingTargetBookId ?: return@LaunchedEffect
        val targetChapter = BibleState.pendingTargetChapter ?: return@LaunchedEffect
        val layout = textLayout ?: return@LaunchedEffect
        if (viewportSize.height <= 0) return@LaunchedEffect
        val chapterText = chapters.text ?: return@LaunchedEffect
        if (chapterText.isBlank()) return@LaunchedEffect
        if (!BibleState.selectedBook.bookId.equals(targetBookId, ignoreCase = true) ||
            chapters.chapterId != targetChapter
        ) {
            return@LaunchedEffect
        }

        val activeVersion = chapters.version ?: BibleState.selectedVersion
        val verseEnd = BibleState.highlightedVerseEnd?.coerceAtLeast(targetVerse) ?: targetVerse
        val verseRange = findVerseCharacterRange(
            chapterText = chapterText,
            verseStart = targetVerse,
            verseEnd = verseEnd,
            version = activeVersion,
        ) ?: findVerseCharacterOffset(
            chapterText = chapterText,
            verseNumber = targetVerse,
            version = activeVersion,
        )?.let { it..it } ?: if (targetVerse == 1) 0..0 else null

        if (verseRange != null) {
            val startIdx = verseRange.first.coerceIn(0, (chapterText.length - 1).coerceAtLeast(0))
            val endIdx = verseRange.last.coerceIn(startIdx, (chapterText.length - 1).coerceAtLeast(0))
            val startLine = layout.getLineForOffset(startIdx)
            val endLine = layout.getLineForOffset(endIdx)
            val verseMidY = (layout.getLineTop(startLine) + layout.getLineBottom(endLine)) / 2f +
                readingState.contentInset
            readingState.jumpToOffset(startIdx)
            withFrameNanos { }
            val centeredScroll = (verseMidY - viewportSize.height / 2f)
                .roundToInt()
                .coerceIn(0, scrollState.maxValue)
            scrollState.scrollTo(centeredScroll)
            readingState.restoring = false
            readingState.restoredScrollOffset = null
            readingState.capture(scrollState.value)
            readingState.restoredScrollOffset = scrollState.value
        }
        BibleState.clearPendingTargetVerse()
    }

    LaunchedEffect(Unit) {
        snapshotFlow { currentFontSize }
            .distinctUntilChanged()
            .drop(1)
            .debounce(300)
            .collect { updateUserPreferences(it, BibleState.selectedVersion) }
    }

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        val windowMetrics = computeAdaptiveWindowMetrics(maxWidth, maxHeight)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .testTag("ScriptureReader")
                .onSizeChanged { size ->
                    if (size != viewportSize) {
                        readingState.restoring = true
                        viewportSize = size
                    }
                }
                .verticalScroll(scrollState)
                .navigationBarsPadding()
                .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 120.dp)
                .pointerInput(Unit) {
                    try {
                        awaitPointerEventScope {
                            var gestureFontSize = currentFontSize
                            while (true) {
                                val event = awaitPointerEvent()
                                if (event.changes.count { it.pressed } > 1) {
                                    if (!readingState.zooming) {
                                        gestureFontSize = currentFontSize
                                        readingState.beginZoom(scrollState.value)
                                    }
                                    val zoom = event.calculateZoom()
                                    if (zoom.isFinite() && zoom != 1f) {
                                        val maximum = if (isDesktopPlatform()) 72f else 40f
                                        gestureFontSize = (gestureFontSize * zoom).coerceIn(12f, maximum)
                                        changeFontSize(gestureFontSize)
                                        event.changes.forEach { it.consume() }
                                    }
                                } else {
                                    if (readingState.zooming) {
                                        AnalyticsTracker.track(
                                            "scripture_pinch_zoom",
                                            "preferences",
                                            mapOf("fontSize" to gestureFontSize.toInt().toString()),
                                        )
                                    }
                                    readingState.endZoom()
                                }
                            }
                        }
                    } finally {
                        // Disposal/cancellation must not leave a reused reader locked.
                        readingState.endZoom()
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures(onDoubleTap = {
                        val nextSize = if (currentFontSize == 30f) previousFontSize else {
                            previousFontSize = currentFontSize
                            30f
                        }
                        AnalyticsTracker.track(
                            "scripture_double_tap_zoom",
                            "preferences",
                            mapOf("fontSize" to nextSize.toInt().toString()),
                        )
                        changeFontSize(nextSize)
                    })
                }
        ) {
            chapters.text?.let { text ->
                val isHighlightedChapter =
                    BibleState.highlightedVerseBookId.equals(BibleState.selectedBook.bookId, ignoreCase = true) &&
                        BibleState.highlightedVerseChapter == chapters.chapterId
                val highlightStart = if (isHighlightedChapter) BibleState.highlightedVerseStart else null
                val highlightEnd = if (isHighlightedChapter) BibleState.highlightedVerseEnd else null
                val activeVersion = chapters.version ?: BibleState.selectedVersion
                val renderedText = remember(text, highlightStart, highlightEnd, activeVersion) {
                    buildAnnotatedString {
                        append(text)
                        if (highlightStart != null && highlightEnd != null) {
                            val range = findVerseCharacterRange(
                                chapterText = text,
                                verseStart = highlightStart,
                                verseEnd = highlightEnd,
                                version = activeVersion,
                            )
                            if (range != null) {
                                val start = range.first.coerceIn(0, text.length)
                                val end = (range.last + 1).coerceIn(start, text.length)
                                addStyle(
                                    style = SpanStyle(
                                        color = Color(0xFFFBD38D),
                                        fontWeight = FontWeight.SemiBold,
                                    ),
                                    start = start,
                                    end = end,
                                )
                            }
                        }
                    }
                }
                Text(
                    text = renderedText,
                    fontSize = selectedFontSize.sp,
                    lineHeight = (selectedFontSize * CloudSessionManager.readerLineHeightMultiplier).sp,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .widthIn(max = windowMetrics.maxReadingWidth)
                        .fillMaxWidth()
                        .testTag("ScriptureText"),
                    onTextLayout = { result ->
                        if (!sameTextGeometry(textLayout, result)) {
                            readingState.restoring = true
                            readingState.layout = result
                            textLayout = result
                        }
                    }
                )
            }
        }
    }
}

private fun sameTextGeometry(previous: TextLayoutResult?, next: TextLayoutResult): Boolean {
    if (previous == null || previous.layoutInput != next.layoutInput ||
        previous.size != next.size || previous.lineCount != next.lineCount) return false
    // Typeface resolution can change line wrapping without changing TextLayoutInput.
    return (0 until next.lineCount).all { line ->
        previous.getLineStart(line) == next.getLineStart(line) &&
            previous.getLineEnd(line) == next.getLineEnd(line) &&
            previous.getLineTop(line) == next.getLineTop(line) &&
            previous.getLineBottom(line) == next.getLineBottom(line)
    }
}
