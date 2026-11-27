package ui.plans

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import data.plans.ReadingPlanRepository

private val GoldAccent = Color(0xFFFBD38D)

@OptIn(ExperimentalMaterialApi::class)
@Composable
internal fun ReadingPlanBannerCard(
    onOpenPlans: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val activePlan = ReadingPlanRepository.activeOrDefaultPlan()
    val progress = ReadingPlanRepository.progressFor(activePlan)
    val hasStarted = ReadingPlanRepository.hasJoinedActivePlan() || progress.completedCount > 0
    val cardBrush = Brush.horizontalGradient(
        colors = listOf(
            Color(0xFF173347),
            Color(0xFF214A5E),
            Color(0xFF2C6073),
        )
    )

    Card(
        onClick = onOpenPlans,
        shape = RoundedCornerShape(20.dp),
        elevation = 4.dp,
        border = BorderStroke(1.dp, Color(0x66FBD38D)),
        backgroundColor = Color(0xFF173347),
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
            .testTag("reading_plan_entry_card"),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .background(cardBrush)
                .padding(horizontal = 18.dp, vertical = 14.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color(0x2EFBD38D)),
                    ) {
                        Text(
                            text = if (hasStarted) "${progress.percentComplete}%" else "${activePlan.durationDays}d",
                            color = GoldAccent,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(
                            text = "BIBLE READING PLAN",
                            color = GoldAccent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.6.sp,
                        )
                        Text(
                            text = activePlan.title,
                            style = MaterialTheme.typography.subtitle1.copy(
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        val subtitle = if (hasStarted && progress.nextReadingDay != null && progress.nextReading != null) {
                            "Next: Day ${progress.nextReadingDay.dayNumber} · ${progress.nextReading.label}"
                        } else {
                            "Guided daily passages with auto-checkoff"
                        }
                        Text(
                            text = subtitle,
                            color = Color(0xFFDCE6F2),
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                Surface(
                    shape = RoundedCornerShape(50),
                    color = GoldAccent,
                    elevation = 0.dp,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    ) {
                        Text(
                            text = if (hasStarted) "Read" else "Explore",
                            color = Color(0xFF09111E),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = Color(0xFF09111E),
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }

            if (hasStarted) {
                LinearProgressIndicator(
                    progress = (progress.percentComplete / 100f).coerceIn(0f, 1f),
                    color = GoldAccent,
                    backgroundColor = Color(0x28FFFFFF),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(RoundedCornerShape(50)),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
internal fun ReadingPlanTwinCard(
    onOpenPlans: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val activePlan = ReadingPlanRepository.activeOrDefaultPlan()
    val progress = ReadingPlanRepository.progressFor(activePlan)
    val hasStarted = ReadingPlanRepository.hasJoinedActivePlan() || progress.completedCount > 0
    val cardBrush = Brush.linearGradient(
        colors = listOf(
            Color(0xFF173347),
            Color(0xFF2C6073),
        )
    )

    Card(
        onClick = onOpenPlans,
        shape = RoundedCornerShape(20.dp),
        elevation = 4.dp,
        border = BorderStroke(1.dp, Color(0x66FBD38D)),
        backgroundColor = Color(0xFF173347),
        modifier = modifier
            .padding(bottom = 16.dp)
            .testTag("reading_plan_twin_card"),
    ) {
        Column(
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .background(cardBrush)
                .padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color(0x33FBD38D),
                    elevation = 0.dp,
                ) {
                    Text(
                        text = if (hasStarted) "${progress.percentComplete}%" else "Plans",
                        color = GoldAccent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Open Reading Plans",
                    tint = GoldAccent,
                    modifier = Modifier.size(18.dp),
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = activePlan.title,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val nextLine = if (hasStarted && progress.nextReadingDay != null && progress.nextReading != null) {
                    "Day ${progress.nextReadingDay.dayNumber}: ${progress.nextReading.label}"
                } else {
                    "Daily guided readings"
                }
                Text(
                    text = nextLine,
                    color = Color(0xFFDCE6F2),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
