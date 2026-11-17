package ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import data.apiBible.BibleAPIDataModel
import data.bibleIQ.*
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
            DailyVerseState.Ready(dailyVerseRepository.get(version))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            DailyVerseState.Unavailable
        }
    }
    VerseOfTheDayCard(state, version, onRetry = { retry++ })
}

@Composable
internal fun VerseOfTheDayCard(state: DailyVerseState, version: String, onRetry: () -> Unit) {
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
                    CompositionLocalProvider(LocalLayoutDirection provides if (version == "SVD") LayoutDirection.Rtl else LayoutDirection.Ltr) {
                        Text(verse.text, style = MaterialTheme.typography.body1, modifier = Modifier.fillMaxWidth())
                    }
                    val book = BibleAPIDataModel.bibleBooks.data?.find { it.bookId == verse.bookUsfm }?.name ?: verse.bookUsfm.orEmpty()
                    Text("$book ${verse.chapter}:${verse.verse} · $version", style = MaterialTheme.typography.caption)
                }
            }
        }
    }
}
