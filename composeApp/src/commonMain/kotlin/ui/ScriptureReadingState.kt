package ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextLayoutResult
import kotlin.math.roundToInt

/** A location in the text, independent of screen width, density, and font size. */
internal data class ScriptureAnchor(val character: Int = 0, val lineFraction: Float = 0f)

@Stable
internal class ScriptureReadingState(initialAnchor: ScriptureAnchor = ScriptureAnchor()) {
    var anchor by mutableStateOf(initialAnchor)
        private set

    internal var layout: TextLayoutResult? = null
    internal var restoring = true
    internal var contentInset = 0
    internal var restoredScrollOffset: Int? = null

    fun capture(scrollOffset: Int) {
        val textLayout = layout ?: return
        if (restoring || scrollOffset == restoredScrollOffset || textLayout.lineCount == 0) return
        restoredScrollOffset = null
        val textOffset = (scrollOffset - contentInset).coerceAtLeast(0)
        val line = textLayout.getLineForVerticalPosition(textOffset.toFloat())
        val top = textLayout.getLineTop(line)
        val height = textLayout.getLineBottom(line) - top
        anchor = ScriptureAnchor(
            character = textLayout.getLineStart(line),
            lineFraction = if (height > 0) ((textOffset - top) / height).coerceIn(0f, 1f) else 0f
        )
    }

    fun scrollOffset(textLayout: TextLayoutResult): Int {
        if (textLayout.lineCount == 0) return 0
        if (anchor.character == 0 && anchor.lineFraction == 0f) return 0
        val character = anchor.character.coerceIn(0, textLayout.layoutInput.text.length)
        val line = textLayout.getLineForOffset(character)
        val top = textLayout.getLineTop(line)
        val height = textLayout.getLineBottom(line) - top
        return (top + height * anchor.lineFraction.coerceIn(0f, 1f)).roundToInt().coerceAtLeast(0) + contentInset
    }

    companion object {
        val Saver = listSaver<ScriptureReadingState, Any>(
            save = { listOf(it.anchor.character, it.anchor.lineFraction) },
            restore = { ScriptureReadingState(ScriptureAnchor(it[0] as Int, it[1] as Float)) }
        )
    }
}

@Composable
internal fun rememberScriptureReadingState(): ScriptureReadingState =
    rememberSaveable(saver = ScriptureReadingState.Saver) { ScriptureReadingState() }
