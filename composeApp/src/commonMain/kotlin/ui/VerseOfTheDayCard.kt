package ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import data.analytics.AnalyticsTracker
import data.bible.BibleState
import data.bible.DailyVerse
import data.bible.DailyVerseRepository
import data.bible.bibleBooksFull
import data.bible.dailyVerseRepository
import data.bible.utcToday
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeSource

// Minimum time the refresh animation stays visible after an explicit pull.
internal var DailyVerseRefreshMinimum: Duration = 1600.milliseconds

internal sealed interface DailyVerseState {
    data object Loading : DailyVerseState
    data class Ready(val verse: DailyVerse) : DailyVerseState
    data object Unavailable : DailyVerseState
}

internal class DailyVersePresentation {
    var state by mutableStateOf<DailyVerseState>(DailyVerseState.Loading)
        internal set
    var refreshing by mutableStateOf(false)
        internal set
    var refreshRequest by mutableStateOf(0)
        private set

    fun refresh() {
        if (refreshing || state == DailyVerseState.Loading) return
        refreshing = true
        refreshRequest++
    }
}

@Composable
internal fun rememberDailyVerse(
    version: String,
    repository: DailyVerseRepository = dailyVerseRepository,
): DailyVersePresentation {
    val presentation = remember(version, repository) { DailyVersePresentation() }
    var date by remember { mutableStateOf(utcToday()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000)
            date = utcToday()
        }
    }
    LaunchedEffect(presentation, date, presentation.refreshRequest) {
        val refreshing = presentation.refreshing
        val previous = presentation.state as? DailyVerseState.Ready
        if (!refreshing) {
            presentation.state = DailyVerseState.Loading
        }
        val startedAt = TimeSource.Monotonic.markNow()
        // Hold the loading animation for a minimum time so a fast connection
        // still shows the stretch, glass, and arc instead of an instant swap.
        suspend fun holdMinimum() {
            if (!refreshing) return
            val remaining = DailyVerseRefreshMinimum - startedAt.elapsedNow()
            if (remaining.isPositive()) delay(remaining)
        }
        try {
            val loaded = repository.get(version, refresh = refreshing)
            if (refreshing) {
                BibleState.clearHighlightedVerse()
            }
            BibleState.setActiveDailyVerse(loaded)
            holdMinimum()
            AnalyticsTracker.track(
                name = "votd_loaded",
                category = "verse_of_the_day",
                properties = mapOf(
                    "version" to loaded.version,
                    "bookUsfm" to (loaded.data.bookUsfm ?: loaded.data.bookNumber.toString()),
                    "chapter" to loaded.data.chapter.toString(),
                    "verse" to loaded.data.verse.toString(),
                ),
            )
            presentation.state = DailyVerseState.Ready(loaded)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AnalyticsTracker.track(
                name = "votd_load_failed",
                category = "verse_of_the_day",
                properties = mapOf("version" to version, "reason" to (e::class.simpleName ?: "exception")),
            )
            try {
                holdMinimum()
            } catch (c: CancellationException) {
                throw c
            }
            presentation.state = previous?.takeIf { refreshing } ?: DailyVerseState.Unavailable
        }
        presentation.refreshing = false
    }
    return presentation
}

@Composable
internal fun VerseOfTheDay(
    presentation: DailyVersePresentation,
    version: String,
    modifier: Modifier = Modifier,
    onVerseClick: (DailyVerse) -> Unit = {},
) {
    VerseOfTheDayCard(
        state = presentation.state,
        version = version,
        onRetry = presentation::refresh,
        onVerseClick = onVerseClick,
        modifier = modifier,
        refreshing = presentation.refreshing,
    )
}

@Composable
internal fun VerseOfTheDayCard(
    state: DailyVerseState,
    version: String,
    onRetry: () -> Unit,
    onVerseClick: (DailyVerse) -> Unit = {},
    modifier: Modifier = Modifier,
    refreshing: Boolean = false,
) {
    val cardBrush = Brush.horizontalGradient(
        colors = listOf(
            Color(0xFF19304F),
            Color(0xFF24426B),
            Color(0xFF2F5380),
        )
    )
    val readyVerse = (state as? DailyVerseState.Ready)?.verse
    // After a refresh finishes, sweep the glass across the fresh verse a couple more times.
    var freshPasses by remember { mutableStateOf(false) }
    var wasRefreshing by remember { mutableStateOf(false) }
    LaunchedEffect(refreshing) {
        if (!refreshing && wasRefreshing && readyVerse != null) {
            freshPasses = true
            delay(FreshSheenPasses * FreshSheenPassMillis)
            freshPasses = false
        }
        wasRefreshing = refreshing
    }
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
            .testTag("verse_of_the_day_card")
            .then(
                if (readyVerse != null) {
                    Modifier.clickable {
                        AnalyticsTracker.track(
                            name = "votd_clicked",
                            category = "verse_of_the_day",
                            properties = mapOf(
                                "version" to readyVerse.version,
                                "bookUsfm" to (readyVerse.data.bookUsfm ?: readyVerse.data.bookNumber.toString()),
                                "chapter" to readyVerse.data.chapter.toString(),
                            ),
                        )
                        onVerseClick(readyVerse)
                    }
                } else {
                    Modifier
                }
            ),
        shape = RoundedCornerShape(20.dp),
        elevation = 4.dp,
        border = BorderStroke(1.dp, Color(0x44FBD38D)),
        backgroundColor = Color(0xFF19304F),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(cardBrush)
                .refreshSheen(active = refreshing || freshPasses, loading = refreshing)
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Surface(
                shape = RoundedCornerShape(50),
                color = Color(0x2AFBD38D),
                elevation = 0.dp,
            ) {
                Text(
                    text = "Verse of the Day",
                    style = MaterialTheme.typography.subtitle2.copy(
                        color = Color(0xFFFBD38D),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        letterSpacing = 0.6.sp,
                    ),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }
            when (state) {
                DailyVerseState.Loading -> {
                    LinearProgressIndicator(
                        color = Color(0xFFFBD38D),
                        backgroundColor = Color(0x33FFFFFF),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        "Loading today’s verse…",
                        style = MaterialTheme.typography.body2.copy(color = Color(0xFFCBD5E1)),
                    )
                }

                DailyVerseState.Unavailable -> {
                    Text(
                        "Today’s verse couldn’t be loaded. You can still browse the books below.",
                        style = MaterialTheme.typography.body2.copy(color = Color(0xFFE2E8F0)),
                    )
                    TextButton(onClick = onRetry) {
                        Text("Try again", color = Color(0xFFFBD38D), fontWeight = FontWeight.Bold)
                    }
                }

                is DailyVerseState.Ready -> {
                    val verse = state.verse.data
                    val isRtl = version.equals("SVD", ignoreCase = true)
                    CompositionLocalProvider(
                        LocalLayoutDirection provides if (isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr
                    ) {
                        Text(
                            verse.text,
                            style = MaterialTheme.typography.body1.copy(
                                color = Color.White,
                                lineHeight = 23.sp,
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    val book = BibleState.bibleBooks.data?.find { it.bookId == verse.bookUsfm }?.name
                        ?: bibleBooksFull.getOrNull(verse.bookNumber - 1)
                        ?: verse.bookUsfm.orEmpty()
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            "$book ${verse.chapter}:${verse.verse} · $version",
                            style = MaterialTheme.typography.caption.copy(
                                color = Color(0xFFFBD38D),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                            ),
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Read chapter",
                            tint = Color(0xFFFBD38D),
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        }
    }
}

// Frosted gold sheen that sweeps across the card while the next verse loads.
// Driven by the wall clock so it still moves when Android animation scales are 0.
private const val FreshSheenPasses = 2L
private const val FreshSheenPassMillis = 1600L

private fun Modifier.refreshSheen(active: Boolean, loading: Boolean): Modifier = if (!active) this else composed {
    var time by remember { mutableLongStateOf(0L) }
    var start by remember(loading) { mutableLongStateOf(-1L) }
    val period = if (loading) 1200L else FreshSheenPassMillis
    val totalDuration = FreshSheenPasses * FreshSheenPassMillis
    LaunchedEffect(loading) {
        while (true) {
            withFrameNanos {
                time = it / 1_000_000L
                if (start < 0L) start = time
            }
        }
    }
    graphicsLayer {
        alpha = if (loading) {
            0.72f
        } else {
            val elapsed = if (start >= 0L) (time - start).coerceAtLeast(0L) else 0L
            val alphaProgress = (elapsed / 350f).coerceIn(0f, 1f)
            0.72f + 0.28f * alphaProgress
        }
    }
        .drawWithContent {
            drawContent()
            val elapsed = if (start >= 0L) (time - start).coerceAtLeast(0L) else 0L
            if (!loading && elapsed >= totalDuration) return@drawWithContent
            val phase = (elapsed % period) / period.toFloat()
            val x = size.width * (-0.4f + 1.8f * phase)
            val tintAlpha = if (loading) {
                0x14 / 255f
            } else {
                val totalProgress = (elapsed.toFloat() / totalDuration).coerceIn(0f, 1f)
                (0x14 / 255f) * (1f - totalProgress) * (1f - totalProgress)
            }
            if (tintAlpha > 0f) {
                drawRect(Color(0xFFFBD38D).copy(alpha = tintAlpha))
            }
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(Color.Transparent, Color(0x48FBD38D), Color.Transparent),
                    startX = x - size.width * 0.3f,
                    endX = x + size.width * 0.3f,
                ),
            )
        }
}
