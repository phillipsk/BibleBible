package ui.configs

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.BottomSheetState
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.FilterChip
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import data.appPrefs.updateUserPrefsBibleVersion
import data.bibleIQ.BibleIQDataModel
import data.bibleIQ.BibleIQVersions
import io.github.aakira.napier.Napier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterialApi::class)
@Composable
internal fun BibleMenu(
    bibleVersionsList: BibleIQVersions,
    selectedVersion: String,
    bottomSheetState: BottomSheetState,
) {
    val scope = rememberCoroutineScope { Dispatchers.IO }
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        bibleVersionsList.data.forEach { version ->
            if (version.abbreviation != null) {
//                Napier.v("version.abbreviation ${version.abbreviation} " +
//                        " equals :: selectedVersion :: $selectedVersion", tag = "AP8243")
                val selected = remember(selectedVersion) { selectedVersion == version.abbreviation }
                FilterChip(
                    onClick = {
                        BibleIQDataModel.run {
                            updateSelectedVersion(version.abbreviation)
                        }
                        scope.launch {
                            Napier.v(
                                "updateUserPrefsBibleVersion :: selectedVersion ${version.abbreviation}",
                                tag = "AP8243"
                            )
                            updateUserPrefsBibleVersion(version.abbreviation)
                            withContext(Dispatchers.Main) {
                                delay(400)
                                bottomSheetState.collapse()
                                Napier.v("BibleMenu :: bottomSheetState.collapse() ", tag = "FF6290")
                            }
                        }
                    },
                    selected = selected,
                    leadingIcon = if (selected) {
                        {
                            Icon(
                                imageVector = Icons.Filled.Done,
                                contentDescription = "Done icon",
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    } else {
                        null
                    },
                ) {
                    Text("${version.abbreviation} ", fontSize = 14.sp)
                }
            }
        }
    }
}