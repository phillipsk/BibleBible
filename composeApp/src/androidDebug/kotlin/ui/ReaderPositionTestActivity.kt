package ui

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import data.bibleIQ.BibleChapterUIState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** Debug-only host for real Activity recreation and orientation regression tests. */
class ReaderPositionTestActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ReaderPositionTestProbe.created()
        setContent {
            val reading = rememberScriptureReadingState()
            val scroll = rememberScrollState()
            val scope = rememberCoroutineScope()
            var fontSize by rememberSaveable { mutableStateOf(20f) }
            val orientation = LocalConfiguration.current.orientation
            SideEffect { ReaderPositionTestProbe.bind(reading, scroll, scope, orientation) }
            BibleBibleTheme {
                Box(Modifier.fillMaxSize()) {
                    BibleScriptures(
                        chapters = fixtureChapter,
                        scrollState = scroll,
                        selectedFontSize = fontSize,
                        onFontSizeChanged = { fontSize = it },
                        readingState = reading
                    )
                }
            }
        }
    }

    private val fixtureChapter = BibleChapterUIState(
        bookId = 1,
        chapterId = 1,
        version = "KJV",
        text = (1..150).joinToString(" ") {
            "[$it] In the beginning was the Word, and the Word was with God."
        }
    )
}

/** Primitive-only bridge so Android instrumentation can inspect the debug host. */
object ReaderPositionTestProbe {
    @Volatile private var generation = 0
    @Volatile private var orientation = Configuration.ORIENTATION_UNDEFINED
    @Volatile private var reading: ScriptureReadingState? = null
    @Volatile private var scroll: ScrollState? = null
    @Volatile private var scope: CoroutineScope? = null

    internal fun created() {
        generation += 1
        reading = null
        scroll = null
        scope = null
    }

    internal fun bind(
        reading: ScriptureReadingState,
        scroll: ScrollState,
        scope: CoroutineScope,
        orientation: Int
    ) {
        this.reading = reading
        this.scroll = scroll
        this.scope = scope
        this.orientation = orientation
    }

    fun scrollTo(offset: Int) {
        val currentScroll = requireNotNull(scroll)
        requireNotNull(scope).launch { currentScroll.scrollTo(offset) }
    }

    fun snapshot(): ReaderPositionSnapshot? {
        val currentReading = reading ?: return null
        val currentScroll = scroll ?: return null
        val layout = currentReading.layout ?: return null
        val viewportTop = (currentScroll.value - currentReading.contentInset).coerceAtLeast(0).toFloat()
        val visibleLine = layout.getLineForVerticalPosition(viewportTop)
        val lineTop = layout.getLineTop(visibleLine)
        val lineHeight = layout.getLineBottom(visibleLine) - lineTop
        return ReaderPositionSnapshot(
            generation = generation,
            orientation = orientation,
            character = currentReading.anchor.character,
            lineFraction = currentReading.anchor.lineFraction,
            scrollOffset = currentScroll.value,
            expectedScrollOffset = currentReading.scrollOffset(layout).coerceAtMost(currentScroll.maxValue),
            maxScrollOffset = currentScroll.maxValue,
            restoring = currentReading.restoring,
            lineCount = layout.lineCount,
            firstVisibleLineStart = layout.getLineStart(visibleLine),
            firstVisibleLineEnd = layout.getLineEnd(visibleLine),
            viewportLineFraction = if (lineHeight > 0) ((viewportTop - lineTop) / lineHeight).coerceIn(0f, 1f) else 0f
        )
    }
}

data class ReaderPositionSnapshot(
    val generation: Int,
    val orientation: Int,
    val character: Int,
    val lineFraction: Float,
    val scrollOffset: Int,
    val expectedScrollOffset: Int,
    val maxScrollOffset: Int,
    val restoring: Boolean,
    val lineCount: Int,
    val firstVisibleLineStart: Int,
    val firstVisibleLineEnd: Int,
    val viewportLineFraction: Float
)
