package ui.configs.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import data.gemini.checkAnimationLastCalled
import io.github.aakira.napier.Napier
import kotlinx.coroutines.delay

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
//        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            HomeTitle(onClick)
            AnimatedSubtitle(showSubtitle, animationCounter)
        }
    }
}