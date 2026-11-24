package ui

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

internal enum class BibleWindowLayoutClass {
    COMPACT_PHONE,
    FOLDABLE_DUO_UNFOLDED,
    TABLET_WIDE,
}

internal data class AdaptiveWindowMetrics(
    val layoutClass: BibleWindowLayoutClass,
    val maxWidth: Dp,
    val maxHeight: Dp,
    val bookGridColumns: Int,
    val maxReadingWidth: Dp,
    val hingeSafeGap: Dp,
    val isDualPaneShorts: Boolean,
    val isWideHome: Boolean,
)

/**
 * Classifies the active viewport into compact phone, unfolded foldable / iPhone Duo, or tablet/wide
 * layouts so grid column density, reading line length, and hinge-safe dual-pane geometry adapt cleanly.
 */
internal fun computeAdaptiveWindowMetrics(
    maxWidth: Dp,
    maxHeight: Dp,
): AdaptiveWindowMetrics {
    val safeHeight = maxHeight.value.coerceAtLeast(1f)
    val aspectRatio = maxWidth.value / safeHeight

    return when {
        maxWidth < 560.dp -> AdaptiveWindowMetrics(
            layoutClass = BibleWindowLayoutClass.COMPACT_PHONE,
            maxWidth = maxWidth,
            maxHeight = maxHeight,
            bookGridColumns = 3,
            maxReadingWidth = maxWidth,
            hingeSafeGap = 0.dp,
            isDualPaneShorts = false,
            isWideHome = false,
        )

        maxWidth < 900.dp && aspectRatio in 0.68f..1.45f -> AdaptiveWindowMetrics(
            layoutClass = BibleWindowLayoutClass.FOLDABLE_DUO_UNFOLDED,
            maxWidth = maxWidth,
            maxHeight = maxHeight,
            bookGridColumns = 4,
            maxReadingWidth = 680.dp,
            hingeSafeGap = 28.dp,
            isDualPaneShorts = true,
            isWideHome = true,
        )

        else -> AdaptiveWindowMetrics(
            layoutClass = BibleWindowLayoutClass.TABLET_WIDE,
            maxWidth = maxWidth,
            maxHeight = maxHeight,
            bookGridColumns = if (maxWidth >= 1024.dp) 6 else 5,
            maxReadingWidth = 760.dp,
            hingeSafeGap = 24.dp,
            isDualPaneShorts = true,
            isWideHome = true,
        )
    }
}
