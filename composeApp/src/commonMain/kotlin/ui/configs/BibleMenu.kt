package ui.configs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.BottomSheetState
import androidx.compose.material.ChipDefaults
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.FilterChip
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import data.analytics.AnalyticsTracker
import data.appPrefs.updateUserPrefsBibleVersion
import data.bible.BibleState
import data.bible.BibleVersions
import io.github.aakira.napier.Napier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterialApi::class)
@Composable
internal fun BibleMenu(
    bibleVersionsList: BibleVersions,
    selectedVersion: String,
    bottomSheetState: BottomSheetState,
    onInteraction: () -> Unit = {},
) {
    val scope = rememberCoroutineScope { Dispatchers.IO }
    val validVersions = remember(bibleVersionsList.data) {
        bibleVersionsList.data.filter { it.abbreviation != null }
    }
    val listState = rememberLazyListState()
    val density = LocalDensity.current

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val viewportPx = with(density) { maxWidth.toPx() }
        val estimatedChipWidthPx = with(density) { 88.dp.toPx() }
        val centerOffset = -((viewportPx - estimatedChipWidthPx) / 2f).toInt()

        LaunchedEffect(bottomSheetState.isExpanded, selectedVersion, maxWidth) {
            if (bottomSheetState.isExpanded) {
                val selectedIdx = validVersions.indexOfFirst {
                    it.abbreviation.equals(selectedVersion, ignoreCase = true)
                }
                if (selectedIdx >= 0) {
                    listState.animateScrollToItem(index = selectedIdx, scrollOffset = centerOffset)
                }
            }
        }

        LazyRow(
            state = listState,
            contentPadding = PaddingValues(horizontal = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            items(validVersions, key = { it.abbreviation ?: it.uiAbbreviation }) { version ->
                val abbr = version.abbreviation ?: return@items
                val selected = selectedVersion.equals(abbr, ignoreCase = true)
                val gold = Color(0xFFFBD38D)
                FilterChip(
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (selected) gold else Color(0x22FFFFFF),
                    ),
                    colors = ChipDefaults.filterChipColors(
                        backgroundColor = Color(0x14FFFFFF),
                        selectedBackgroundColor = Color(0x2EFBD38D),
                        contentColor = Color(0xFFE2E8F0),
                        selectedContentColor = gold,
                        leadingIconColor = gold,
                        selectedLeadingIconColor = gold,
                    ),
                    onClick = {
                        onInteraction()
                        val previousVersion = BibleState.selectedVersion
                        BibleState.updateSelectedVersion(abbr)
                        AnalyticsTracker.trackTranslationChanged(previousVersion, abbr)
                        scope.launch {
                            Napier.v(
                                "updateUserPrefsBibleVersion :: selectedVersion $abbr",
                                tag = "AP8243",
                            )
                            updateUserPrefsBibleVersion(abbr)
                            delay(400)
                            bottomSheetState.collapse()
                        }
                    },
                    selected = selected,
                    leadingIcon = if (selected) {
                        {
                            Icon(
                                imageVector = Icons.Filled.Done,
                                contentDescription = "Done icon",
                                modifier = Modifier.size(12.dp),
                            )
                        }
                    } else {
                        null
                    },
                ) {
                    Text(version.uiAbbreviation, fontSize = 14.sp)
                }
            }
        }
    }
}