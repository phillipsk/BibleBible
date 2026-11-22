package ui.configs.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import data.analytics.AnalyticsTracker
import data.bibleIQ.BibleIQDataModel

@Composable
internal fun HomeTopBar(onClick: () -> Unit = {}) {
    var showSubtitle by mutableStateOf(false)
    var animationCounter by mutableStateOf(0)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        HomeTitle(onClick)
        Spacer(modifier = Modifier.weight(1f))
        if (showSubtitle) {
            AnimatedSubtitle(showSubtitle, animationCounter)
        } else {
            SortBibleBooksToggle()
        }
    }
}

@Composable
internal fun SortBibleBooksToggle() {
    val isSortAZ = BibleIQDataModel.sortAZ
    val otNtBg by animateColorAsState(
        targetValue = if (!isSortAZ) Color(0xFFFBD38D) else Color.Transparent,
        animationSpec = tween(220),
        label = "ot_nt_bg",
    )
    val azBg by animateColorAsState(
        targetValue = if (isSortAZ) Color(0xFFFBD38D) else Color.Transparent,
        animationSpec = tween(220),
        label = "az_bg",
    )
    val otNtText by animateColorAsState(
        targetValue = if (!isSortAZ) Color(0xFF0F172A) else Color(0xFFE2E8F0),
        animationSpec = tween(220),
        label = "ot_nt_text",
    )
    val azText by animateColorAsState(
        targetValue = if (isSortAZ) Color(0xFF0F172A) else Color(0xFFE2E8F0),
        animationSpec = tween(220),
        label = "az_text",
    )

    Surface(
        shape = RoundedCornerShape(50),
        color = Color(0x550F1E33),
        border = BorderStroke(1.dp, Color(0x66FBD38D)),
        elevation = 0.dp,
        modifier = Modifier.testTag("book_sort_segmented_toggle"),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(2.dp),
        ) {
            Text(
                text = "OT-NT",
                style = MaterialTheme.typography.subtitle2.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = otNtText,
                ),
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(otNtBg)
                    .clickable {
                        if (BibleIQDataModel.sortAZ) {
                            BibleIQDataModel.sortAZ = false
                            AnalyticsTracker.track(
                                "book_sort_toggled",
                                "preferences",
                                mapOf("sortType" to BibleIQDataModel.selectedSortType),
                            )
                        }
                    }
                    .padding(horizontal = 10.dp, vertical = 5.dp),
            )
            Text(
                text = "A-Z",
                style = MaterialTheme.typography.subtitle2.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = azText,
                ),
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(azBg)
                    .clickable {
                        if (!BibleIQDataModel.sortAZ) {
                            BibleIQDataModel.sortAZ = true
                            AnalyticsTracker.track(
                                "book_sort_toggled",
                                "preferences",
                                mapOf("sortType" to BibleIQDataModel.selectedSortType),
                            )
                        }
                    }
                    .padding(horizontal = 10.dp, vertical = 5.dp),
            )
        }
    }
}
