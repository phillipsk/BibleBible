package ui.configs

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.FilterChip
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import data.bibleIQ.BibleIQDataModel
import data.bibleIQ.BibleIQVersions

@OptIn(ExperimentalMaterialApi::class)
@Composable
internal fun BibleMenu(bibleVersionsList: BibleIQVersions, selectedVersion: String) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        bibleVersionsList.data.forEach { version ->
            if (version.abbreviation != null) {
                val selected = remember(selectedVersion) { selectedVersion == version.abbreviation }
                FilterChip(
                    onClick = {
                        BibleIQDataModel.run {
                            updateSelectedVersion(version.abbreviation)
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
                    Text("${version.abbreviation} ")
                }
            }
        }
    }
}