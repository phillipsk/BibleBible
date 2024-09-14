package ui

import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import ui.Orientation.TOP

enum class Orientation {
    TOP, BOTTOM
}

class TopCutShape(private val orientation: Orientation) : Shape {
    private val cutWidth = 1f
    private val cutHeight = 20f
    override fun createOutline(
        size: androidx.compose.ui.geometry.Size,
        layoutDirection: androidx.compose.ui.unit.LayoutDirection,
        density: androidx.compose.ui.unit.Density
    ): Outline {
        val path = Path().apply {
            // Starting point at the bottom left corner
            moveTo(0f, size.height)
            // Draw line to the top left corner
            lineTo(0f, 0f)
            // Draw line to the start of the cut on the top
//            lineTo((size.width - cutWidth) / 2, 0f)
            // Draw line upwards to create the left side of the cut
            lineTo((size.width + cutWidth) / 2, if (orientation == TOP) +cutHeight else -cutHeight)
            // Draw line to the right side of the cut
            lineTo((size.width - cutWidth) / 2, if (orientation == TOP) +cutHeight else -cutHeight)
            // Draw line downwards to end the cut
//            lineTo((size.width + cutWidth) / 2, 0f)
            // Draw line to the top right corner
            lineTo(size.width, 0f)
            // Draw line to the bottom right corner
            lineTo(size.width, size.height)
            // Close the path back to the starting point
            close()
        }
        return Outline.Generic(path)
    }
}
