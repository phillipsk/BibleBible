package ui.configs

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.EaseInCubic
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideOut
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import data.gemini.checkAnimationLastCalled
import io.github.aakira.napier.Napier
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource

@OptIn(ExperimentalResourceApi::class)
@Composable
internal fun HomeTopBar(onClick: () -> Unit) {
    var showSubtitle by mutableStateOf(false)
    var animationCounter by mutableStateOf(0)

    suspend fun animateSubtitle() {
        try {
            val showAnimation = checkAnimationLastCalled()
            Napier.d("HomeTopBar: showAnimation $showAnimation", tag = "HomeTopBar")
            if (showAnimation) {
                listOf(1000L, 700L, 2000L, 50L).forEach { delay ->
                    showSubtitle = !showSubtitle
                    delay(delay)
                    animationCounter++
                }
            }
        } catch (e: Exception) {
            Napier.e("Error in animateSubtitle: ${e.message}", tag = "HomeTopBar")
        }
    }

    LaunchedEffect(Unit) {
        animateSubtitle()
    }

    TopAppBar(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable(onClick = onClick)
            ) {
                Image(
                    painter = painterResource("BibleBible_ico_iv.png"),
                    contentDescription = "BibleBible",
                    modifier = Modifier.padding(4.dp).clip(RoundedCornerShape(30.dp))
                )
                Text(
                    modifier = Modifier.padding(start = 4.dp),
                    text = "BibleBible",
                    style = TextStyle(
                        fontFamily = FontFamily.Cursive,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.1.sp,
                        color = Color.White,
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Visible,
                )
            }
            Spacer(modifier = Modifier.padding(4.dp))
            Row(
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 12.dp, top = 8.dp)
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
                        modifier = Modifier.fillMaxWidth(),
                        text = "AI Assisted \n       Bible Study",
                        style = TextStyle(
                            fontFamily = FontFamily.Cursive,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.1.sp,
                            color = Color.White,

                            ),
//                        maxLines = 1,
                    )
                }
            }
        }
    }
}