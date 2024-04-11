package ui.configs.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.EaseInCubic
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideOut
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.aakira.napier.Napier

@Composable
internal fun AnimatedSubtitle(showSubtitle: Boolean, animationCounter: Int) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(top = 8.dp)
    ) {
        Napier.d("HomeTopBar: showSubtitle $showSubtitle", tag = "HomeTopBar")
        AnimatedVisibility(
            visible = showSubtitle,
            exit = if (animationCounter < 2) {
                fadeOut() + slideOutVertically()
            } else {
                slideOut(tween(200, easing = EaseInCubic)) {
                    IntOffset(180, 0)
                }
            },
            modifier = Modifier.animateContentSize()
        ) {
            Text(
                modifier = Modifier.padding(end = 18.dp),
                text = "AI Assisted \n       Bible Study",
                style = TextStyle(
                    fontFamily = FontFamily.Cursive,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.1.sp,
                    color = Color.White
                )
            )
        }
    }
}