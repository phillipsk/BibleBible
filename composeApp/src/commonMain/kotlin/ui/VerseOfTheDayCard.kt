package ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import data.analytics.AnalyticsTracker
import data.apiBible.BibleAPIDataModel
import data.bibleIQ.DailyVerse
import data.bibleIQ.bibleBooksFull
import data.bibleIQ.dailyVerseRepository
import data.bibleIQ.utcToday
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay

internal sealed interface DailyVerseState {
    data object Loading : DailyVerseState
    data class Ready(val verse: DailyVerse) : DailyVerseState
    data object Unavailable : DailyVerseState
}

@Composable
internal fun VerseOfTheDay(version: String) {
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
        state,
        version,
        onRetry = {
            AnalyticsTracker.track("votd_retry_clicked", "verse_of_the_day", mapOf("version" to version))
            retry++
        },
    )
}

@Composable
internal fun VerseOfTheDayCard(
    state: DailyVerseState,
    version: String,
    onRetry: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
        shape = RoundedCornerShape(20.dp),
        elevation = 3.dp,
        backgroundColor = MaterialTheme.colors.primaryVariant,
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Verse of the day", style = MaterialTheme.typography.h6)
            when (state) {
                DailyVerseState.Loading -> {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                    Text("Loading today’s verse…", style = MaterialTheme.typography.body2)
                }

                DailyVerseState.Unavailable -> {
                    Text("Today’s verse couldn’t be loaded. You can still browse the books below.")
                    TextButton(onClick = onRetry) { Text("Try again") }
                }

                is DailyVerseState.Ready -> {
                    val verse = state.verse.data
                    val isRtl = version.equals("SVD", ignoreCase = true)
                    CompositionLocalProvider(
                        LocalLayoutDirection provides if (isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr
                    ) {
                        Text(
                            verse.text,
                            style = MaterialTheme.typography.body1,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    val book = BibleAPIDataModel.bibleBooks.data?.find { it.bookId == verse.bookUsfm }?.name
                        ?: bibleBooksFull.getOrNull(verse.bookNumber - 1)
                        ?: verse.bookUsfm.orEmpty()
                    Text("$book ${verse.chapter}:${verse.verse} · $version", style = MaterialTheme.typography.caption)
                }
            }
        }
    }
}
