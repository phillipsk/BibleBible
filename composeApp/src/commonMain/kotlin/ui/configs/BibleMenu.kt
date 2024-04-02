package ui.configs

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.FilterChip
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import data.bibleIQ.BibleIQDataModel
import data.bibleIQ.BibleIQVersions

@OptIn(ExperimentalMaterialApi::class)
@Composable
internal fun BibleMenu(bibleVersionsList: BibleIQVersions) {
    var checked by remember { mutableStateOf(false)}

    Row {
        bibleVersionsList.data.forEach {
            if (it.abbreviation != null) {
                FilterChip(
                    modifier = Modifier.padding(4.dp),
                    onClick = {
                        BibleIQDataModel.run {
                            updateSelectedVersion(it.abbreviation)
                        }
                        checked = it.abbreviation == BibleIQDataModel.selectedVersion
                    },
                    selected = checked,
                    leadingIcon = if (checked) {
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
                    Text("${it.abbreviation} ")
                }
            }
        }
    }
}