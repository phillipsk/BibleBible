package ui.inhouse

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import data.analytics.AnalyticsTracker

@OptIn(ExperimentalMaterialApi::class)
@Composable
internal fun InHouseWordsEntryCard(
    onOpenInHouseWords: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cardBrush = Brush.horizontalGradient(
        colors = listOf(
            Color(0xFF15383B),
            Color(0xFF1F4E50),
            Color(0xFF28605E),
        )
    )

    Card(
        onClick = {
            AnalyticsTracker.track(
                "inhouse_entry_clicked",
                "in_house_words",
                mapOf("trigger" to "card_tap"),
            )
            onOpenInHouseWords()
        },
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
            .testTag("in_house_words_entry_card"),
        shape = RoundedCornerShape(20.dp),
        elevation = 4.dp,
        border = BorderStroke(1.dp, Color(0x55FBD38D)),
        backgroundColor = Color(0xFF15383B),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(cardBrush)
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f),
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color(0x33FBD38D)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Open In-House Words",
                        tint = Color(0xFFFBD38D),
                        modifier = Modifier.size(24.dp),
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "In-House Words",
                        style = MaterialTheme.typography.subtitle1.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 16.sp,
                        ),
                    )
                    Text(
                        text = "Church sermons, Bible studies & morning prayers",
                        style = MaterialTheme.typography.body2.copy(
                            color = Color(0xFFD6ECEB),
                            fontSize = 12.sp,
                        ),
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color(0x33FBD38D),
                    elevation = 0.dp,
                ) {
                    Text(
                        text = "Church",
                        color = Color(0xFFFBD38D),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                    )
                }

                Surface(
                    onClick = {
                        AnalyticsTracker.track(
                            "inhouse_entry_clicked",
                            "in_house_words",
                            mapOf("trigger" to "arrow_tap"),
                        )
                        onOpenInHouseWords()
                    },
                    shape = RoundedCornerShape(50),
                    color = Color(0xFF0E2628),
                    border = BorderStroke(1.dp, Color(0x66FBD38D)),
                    modifier = Modifier.testTag("in_house_words_entry_arrow"),
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = "Explore In-House Words",
                            tint = Color(0xFFFBD38D),
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        }
    }
}
