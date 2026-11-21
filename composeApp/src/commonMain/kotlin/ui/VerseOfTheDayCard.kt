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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
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
import data.bible.bibleBooksFull
import data.bible.dailyVerseRepository
import data.bible.utcToday
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay

internal sealed interface DailyVerseState {
    data object Loading : DailyVerseState
    data class Ready(val verse: DailyVerse) : DailyVerseState
    data object Unavailable : DailyVerseState
}

@Composable
internal fun VerseOfTheDay(
    version: String,
    modifier: Modifier = Modifier,
    onVerseClick: (DailyVerse) -> Unit = {},
) {
    var retry by remember { mutableStateOf(0) }
    var date by remember { mutableStateOf(utcToday()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000)
            date = utcToday()
        }
    }
    val state by produceState<DailyVerseState>(DailyVerseState.Loading, version, date, retry) {
        value = DailyVerseState.Loading
        value = try {
            val loaded = dailyVerseRepository.get(version)
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
            DailyVerseState.Ready(loaded)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AnalyticsTracker.track(
                name = "votd_load_failed",
                category = "verse_of_the_day",
                properties = mapOf(
                    "version" to version,
                    "reason" to (e::class.simpleName ?: "exception"),
                ),
            )
            DailyVerseState.Unavailable
        }
    }
    VerseOfTheDayCard(
        state = state,
        version = version,
        onRetry = {
            AnalyticsTracker.track("votd_retry_clicked", "verse_of_the_day", mapOf("version" to version))
            retry++
        },
        onVerseClick = onVerseClick,
        modifier = modifier,
    )
}

@Composable
internal fun VerseOfTheDayCard(
    state: DailyVerseState,
    version: String,
    onRetry: () -> Unit,
    onVerseClick: (DailyVerse) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val cardBrush = Brush.horizontalGradient(
        colors = listOf(
            Color(0xFF19304F),
            Color(0xFF24426B),
            Color(0xFF2F5380),
        )
    )
    val readyVerse = (state as? DailyVerseState.Ready)?.verse
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
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Surface(
                shape = RoundedCornerShape(50),
                color = Color(0x2AFBD38D),
                elevation = 0.dp,
            ) {
                Text(
                    text = "Verse of the day",
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
