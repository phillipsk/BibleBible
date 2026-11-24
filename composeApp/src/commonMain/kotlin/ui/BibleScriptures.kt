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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import biblebible.composeapp.generated.resources.Res
import biblebible.composeapp.generated.resources.newsreader_regular
import biblebible.composeapp.generated.resources.newsreader_semibold
import data.analytics.AnalyticsTracker
import data.appPrefs.updateUserPreferences
import data.bible.BibleChapter
import data.bible.BibleChapterUIState
import data.bible.BibleState
import data.config.CloudSessionManager
import data.notifications.findVerseCharacterOffset
import data.notifications.findVerseCharacterRange
import data.plans.ReadingPlanRepository
import email.kevinphillips.biblebible.isDesktopPlatform
import org.jetbrains.compose.resources.Font
import ui.plans.PlanReaderReflectionCard
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
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
    onFontGestureUsed: () -> Unit = {},
) {
    var previousFontSize by remember {
        mutableStateOf(
            if (selectedFontSize >= 29.5f) CloudSessionManager.readerDefaultFontSizeSp else selectedFontSize
        )
    }
    var textLayout by remember { mutableStateOf<TextLayoutResult?>(null) }
    var viewportSize by remember { mutableStateOf(IntSize.Zero) }
    val currentFontSize by rememberUpdatedState(selectedFontSize)
    val changeFontSize by rememberUpdatedState(onFontSizeChanged)
    val notifyFontGestureUsed by rememberUpdatedState(onFontGestureUsed)
    readingState.contentInset = with(LocalDensity.current) { 12.dp.roundToPx() }

    val layoutStyle = CloudSessionManager.readerLayoutStyle
    val lineHeightMultiplier = CloudSessionManager.readerLineHeightMultiplier
    val verseGapDp = CloudSessionManager.readerVerseGapDp
    val redLetterEnabled = CloudSessionManager.readerRedLetterEnabled

    val isHighlightedChapter =
        BibleState.highlightedVerseBookId.equals(BibleState.selectedBook.bookId, ignoreCase = true) &&
            BibleState.highlightedVerseChapter == chapters.chapterId
    val highlightStart = if (isHighlightedChapter) BibleState.highlightedVerseStart else null
    val highlightEnd = if (isHighlightedChapter) BibleState.highlightedVerseEnd else null
    val activeVersion = chapters.version ?: BibleState.selectedVersion

    val activePlanContext = BibleState.activePlanReading?.takeIf {
        CloudSessionManager.readingPlansEnabled &&
            (it.reading.bookNumber == chapters.bookId || it.reading.bookId.equals(BibleState.selectedBook.bookId, ignoreCase = true)) &&
            it.reading.chapter == chapters.chapterId
    }
    val bottomThresholdPx = with(LocalDensity.current) { 140.dp.roundToPx() }
    var dwellSatisfied by remember(activePlanContext?.reading?.id) { mutableStateOf(false) }
    var reachedPlanBottom by remember(activePlanContext?.reading?.id) { mutableStateOf(false) }

    LaunchedEffect(activePlanContext?.reading?.id, chapters.text != null) {
        if (activePlanContext != null && chapters.text != null) {
            delay(3000L)
            dwellSatisfied = true
        }
    }

    LaunchedEffect(activePlanContext?.reading?.id, scrollState, chapters.text != null) {
        if (activePlanContext == null || chapters.text == null) {
            BibleState.activeReaderScrollProgress = 0f
            return@LaunchedEffect
        }
        snapshotFlow { scrollState.value to scrollState.maxValue }.collect { (value, max) ->
            if (max != Int.MAX_VALUE) {
                val progress = if (max <= 0) 1f else (value.toFloat() / max.toFloat()).coerceIn(0f, 1f)
                BibleState.activeReaderScrollProgress = progress
                if (!readingState.restoring && (max == 0 || value >= (max - bottomThresholdPx).coerceAtLeast(0))) {
                    reachedPlanBottom = true
                }
            }
        }
    }

    LaunchedEffect(
        activePlanContext?.reading?.id,
        dwellSatisfied,
        reachedPlanBottom,
        CloudSessionManager.readingPlansAutoCheckoffEnabled,
    ) {
        val ctx = activePlanContext ?: return@LaunchedEffect
        if (CloudSessionManager.readingPlansAutoCheckoffEnabled && dwellSatisfied && reachedPlanBottom) {
            ReadingPlanRepository.markReadingCompleted(
                planId = ctx.planId,
                dayNumber = ctx.dayNumber,
                readingId = ctx.reading.id,
                source = "auto_scroll",
            )
        }
    }

    val renderedContent = remember(
        chapters.text,
        chapters.verses,
        chapters.chapterId,
        highlightStart,
        highlightEnd,
        activeVersion,
        selectedFontSize,
        layoutStyle,
        verseGapDp,
        redLetterEnabled,
    ) {
        buildScriptureContent(
            chapters = chapters,
            activeVersion = activeVersion,
            selectedFontSize = selectedFontSize,
            layoutStyle = layoutStyle,
            verseGapDp = verseGapDp,
            redLetterEnabled = redLetterEnabled,
            highlightStart = highlightStart,
            highlightEnd = highlightEnd,
        )
    }

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
        renderedContent,
        BibleState.pendingTargetVerse,
        BibleState.pendingTargetBookId,
        BibleState.pendingTargetChapter,
    ) {
        val targetVerse = BibleState.pendingTargetVerse ?: return@LaunchedEffect
        val targetBookId = BibleState.pendingTargetBookId ?: return@LaunchedEffect
        val targetChapter = BibleState.pendingTargetChapter ?: return@LaunchedEffect
        val layout = textLayout ?: return@LaunchedEffect
        if (viewportSize.height <= 0) return@LaunchedEffect
        val renderedText = renderedContent?.text ?: return@LaunchedEffect
        if (renderedText.isBlank()) return@LaunchedEffect
        if (!BibleState.selectedBook.bookId.equals(targetBookId, ignoreCase = true) ||
            chapters.chapterId != targetChapter
        ) {
            return@LaunchedEffect
        }

        val verseEnd = BibleState.highlightedVerseEnd?.coerceAtLeast(targetVerse) ?: targetVerse
        val mappedRanges = (targetVerse..verseEnd).mapNotNull { renderedContent.verseRanges[it] }
        val verseRange = if (mappedRanges.isNotEmpty()) {
            mappedRanges.first().first..mappedRanges.last().last
        } else {
            val chapterText = chapters.text ?: renderedText.text
            findVerseCharacterRange(
                chapterText = chapterText,
                verseStart = targetVerse,
                verseEnd = verseEnd,
                version = activeVersion,
            ) ?: findVerseCharacterOffset(
                chapterText = chapterText,
                verseNumber = targetVerse,
                version = activeVersion,
            )?.let { it..it } ?: if (targetVerse == 1) 0..0 else null
        }

        if (verseRange != null) {
            val maxIdx = (renderedText.length - 1).coerceAtLeast(0)
            val startIdx = verseRange.first.coerceIn(0, maxIdx)
            val endIdx = verseRange.last.coerceIn(startIdx, maxIdx)
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
                                        notifyFontGestureUsed()
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
                        val largeFontSize = 30f
                        val fallbackNormalSize = CloudSessionManager.readerDefaultFontSizeSp
                        val nextSize = if (currentFontSize >= 29.5f) {
                            previousFontSize.takeIf { it < 29.5f } ?: fallbackNormalSize
                        } else {
                            previousFontSize = currentFontSize
                            largeFontSize
                        }
                        AnalyticsTracker.track(
                            "scripture_double_tap_zoom",
                            "preferences",
                            mapOf("fontSize" to nextSize.toInt().toString()),
                        )
                        changeFontSize(nextSize)
                        notifyFontGestureUsed()
                    })
                }
        ) {
            renderedContent?.let { content ->
                val useNewsreader = layoutStyle == "literary_serif" && !activeVersion.equals("SVD", ignoreCase = true)
                val readerFontFamily = if (useNewsreader) {
                    FontFamily(
                        Font(Res.font.newsreader_regular, FontWeight.Normal, FontStyle.Normal),
                        Font(Res.font.newsreader_regular, FontWeight.Medium, FontStyle.Normal),
                        Font(Res.font.newsreader_semibold, FontWeight.SemiBold, FontStyle.Normal),
                        Font(Res.font.newsreader_semibold, FontWeight.Bold, FontStyle.Normal),
                    )
                } else {
                    null
                }
                Text(
                    text = content.text,
                    fontSize = selectedFontSize.sp,
                    lineHeight = (selectedFontSize * lineHeightMultiplier).sp,
                    fontFamily = readerFontFamily,
                    letterSpacing = if (useNewsreader) 0.1.sp else 0.sp,
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
            if (activePlanContext != null && chapters.text != null) {
                PlanReaderReflectionCard(
                    context = activePlanContext,
                    chapterExcerpt = chapters.text.orEmpty().take(1200),
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .widthIn(max = windowMetrics.maxReadingWidth),
                )
            }
        }
    }
}

private val scriptureGoldColor = Color(0xFFFBD38D)
private val scriptureRedLetterColor = Color(0xFFF48B8B)
private val scriptureBodyColor = Color(0xFFF1F5F9)

internal data class RenderedScriptureContent(
    val text: AnnotatedString,
    val verseRanges: Map<Int, IntRange>,
)

internal fun buildScriptureContent(
    chapters: BibleChapterUIState,
    activeVersion: String,
    selectedFontSize: Float,
    layoutStyle: String,
    verseGapDp: Int,
    redLetterEnabled: Boolean,
    highlightStart: Int?,
    highlightEnd: Int?,
): RenderedScriptureContent? {
    val cleanVerses = chapters.verses.filter { !it.t.isNullOrBlank() }
    if (cleanVerses.isEmpty() || layoutStyle == "classic_inline") {
        val rawText = chapters.text ?: return null
        val verseRanges = mutableMapOf<Int, IntRange>()
        val annotated = buildAnnotatedString {
            append(rawText)
            if (redLetterEnabled && cleanVerses.isNotEmpty()) {
                for (verse in cleanVerses) {
                    val verseNum = verse.v?.toIntOrNull() ?: continue
                    val verseBody = verse.t ?: continue
                    if (verse.r.isEmpty()) continue
                    val fullRange = findVerseCharacterRange(
                        chapterText = rawText,
                        verseStart = verseNum,
                        verseEnd = verseNum,
                        version = activeVersion,
                    ) ?: continue
                    val bodyStart = rawText.indexOf(verseBody, startIndex = fullRange.first)
                        .takeIf { it in fullRange.first..fullRange.last } ?: continue
                    for (span in verse.r) {
                        if (span.size < 2) continue
                        val relStart = span[0].coerceIn(0, verseBody.length)
                        val relEnd = span[1].coerceIn(relStart, verseBody.length)
                        if (relEnd > relStart) {
                            addStyle(
                                style = SpanStyle(color = scriptureRedLetterColor),
                                start = bodyStart + relStart,
                                end = bodyStart + relEnd,
                            )
                        }
                    }
                }
            }
            if (highlightStart != null && highlightEnd != null) {
                val range = findVerseCharacterRange(
                    chapterText = rawText,
                    verseStart = highlightStart,
                    verseEnd = highlightEnd,
                    version = activeVersion,
                )
                if (range != null) {
                    val start = range.first.coerceIn(0, rawText.length)
                    val end = (range.last + 1).coerceIn(start, rawText.length)
                    addStyle(
                        style = SpanStyle(
                            color = scriptureGoldColor,
                            fontWeight = FontWeight.SemiBold,
                        ),
                        start = start,
                        end = end,
                    )
                    verseRanges[highlightStart] = start until end
                }
            }
        }
        return RenderedScriptureContent(text = annotated, verseRanges = verseRanges)
    }

    val isSvdArabic = activeVersion.equals("SVD", ignoreCase = true)
    val useLiteraryParagraphs = layoutStyle == "literary_serif" && !isSvdArabic && cleanVerses.drop(1).any { it.p }
    val verseSeparator = if (verseGapDp >= 18) "\n\n" else "\n"
    val paragraphSeparator = if (verseGapDp <= 6) "\n" else "\n\n"
    val verseRanges = mutableMapOf<Int, IntRange>()

    val annotated = buildAnnotatedString {
        cleanVerses.forEachIndexed { index, verse ->
            val verseText = verse.t?.trim().orEmpty()
            val verseLabel = verse.v?.trim().orEmpty()
            val verseNum = verseLabel.toIntOrNull()
            val isHighlighted = highlightStart != null && highlightEnd != null &&
                verseNum != null && verseNum in highlightStart..highlightEnd

            if (index > 0) {
                if (useLiteraryParagraphs) {
                    append(if (verse.p) paragraphSeparator else " ")
                } else {
                    append(verseSeparator)
                }
            }

            val verseOverallStart = length
            val numColor = scriptureGoldColor

            when {
                layoutStyle == "literary_serif" && !isSvdArabic -> {
                    if (verseLabel.isNotEmpty()) {
                        withStyle(
                            SpanStyle(
                                color = numColor,
                                fontSize = (selectedFontSize * 0.66f).sp,
                                fontWeight = FontWeight.Bold,
                                baselineShift = BaselineShift.Superscript,
                            )
                        ) {
                            append(verseLabel)
                        }
                        append("\u00A0")
                    }
                }
                isSvdArabic -> {
                    if (verseLabel.isNotEmpty()) {
                        withStyle(
                            SpanStyle(
                                color = numColor,
                                fontWeight = FontWeight.Bold,
                            )
                        ) {
                            append("$verseLabel ")
                        }
                    }
                }
                else -> {
                    if (verseLabel.isNotEmpty()) {
                        withStyle(
                            SpanStyle(
                                color = numColor,
                                fontWeight = FontWeight.Bold,
                            )
                        ) {
                            append("[$verseLabel] ")
                        }
                    }
                }
            }

            val bodyStart = length
            if (isHighlighted) {
                withStyle(
                    SpanStyle(
                        color = scriptureGoldColor,
                        fontWeight = FontWeight.SemiBold,
                    )
                ) {
                    append(verseText)
                }
            } else {
                withStyle(SpanStyle(color = scriptureBodyColor)) {
                    append(verseText)
                }
                if (redLetterEnabled && verse.r.isNotEmpty()) {
                    for (span in verse.r) {
                        if (span.size < 2) continue
                        val relStart = span[0].coerceIn(0, verseText.length)
                        val relEnd = span[1].coerceIn(relStart, verseText.length)
                        if (relEnd > relStart) {
                            addStyle(
                                style = SpanStyle(color = scriptureRedLetterColor),
                                start = bodyStart + relStart,
                                end = bodyStart + relEnd,
                            )
                        }
                    }
                }
            }

            if (verseNum != null && length > verseOverallStart) {
                verseRanges[verseNum] = verseOverallStart until length
            }
        }
    }
    return RenderedScriptureContent(text = annotated, verseRanges = verseRanges)
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
