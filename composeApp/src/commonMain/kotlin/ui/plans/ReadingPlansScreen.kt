@file:OptIn(ExperimentalMaterialApi::class)

package ui.plans

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import data.plans.ActivePlanReadingContext
import data.plans.ReadingPlanDayDto
import data.plans.ReadingPlanDto
import data.plans.ReadingPlanRepository
import data.plans.ReadingPlanVerseRefDto
import kotlinx.coroutines.launch

private val GoldAccent = Color(0xFFFBD38D)
private val EmeraldCheck = Color(0xFF34D399)

@Composable
internal fun ReadingPlansScreen(
    onBack: () -> Unit,
    onOpenReading: (ActivePlanReadingContext) -> Unit,
    initialPlanId: String? = null,
    modifier: Modifier = Modifier,
) {
    val plans = ReadingPlanRepository.plans
    var selectedPlanId by remember(initialPlanId, plans) {
        mutableStateOf(
            initialPlanId?.takeIf { id -> plans.any { it.id == id } }
                ?: ReadingPlanRepository.activeOrDefaultPlan().id
        )
    }
    val selectedPlan: ReadingPlanDto = plans.firstOrNull { it.id == selectedPlanId } ?: plans.first()
    val progress = ReadingPlanRepository.progressFor(selectedPlan)
    val scope = rememberCoroutineScope()
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF1A293B))
            .padding(top = statusBarTop)
            .testTag("reading_plans_screen"),
    ) {
        // Top Navigation Bar
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF152334))
                .padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            Surface(
                onClick = onBack,
                shape = RoundedCornerShape(10.dp),
                color = Color(0x26FBD38D),
                elevation = 0.dp,
                modifier = Modifier.testTag("reading_plans_back_btn"),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to Home",
                        tint = GoldAccent,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = "Home",
                        color = GoldAccent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            Text(
                text = "Reading Plans",
                color = GoldAccent,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
            )
            Surface(
                shape = RoundedCornerShape(50),
                color = Color(0x1EFFFFFF),
                elevation = 0.dp,
            ) {
                Text(
                    text = "${progress.completedCount}/${progress.totalCount}",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }
        }

        // Plan Selector Carousel
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            plans.forEach { plan ->
                val isSelected = plan.id == selectedPlan.id
                Surface(
                    onClick = {
                        selectedPlanId = plan.id
                        scope.launch { ReadingPlanRepository.selectActivePlan(plan.id) }
                    },
                    shape = RoundedCornerShape(50),
                    color = if (isSelected) GoldAccent else Color(0xFF17293D),
                    border = if (isSelected) null else BorderStroke(1.dp, Color(0x40FBD38D)),
                    elevation = 0.dp,
                    modifier = Modifier.testTag("plan_tab_${plan.id}"),
                ) {
                    Text(
                        text = plan.title,
                        color = if (isSelected) Color(0xFF09111E) else Color(0xFFCBD5E1),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    )
                }
            }
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 14.dp),
        ) {
            // Active Plan Hero Summary Card
            item(key = "plan_hero_${selectedPlan.id}") {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFF13263E),
                    border = BorderStroke(1.dp, Color(0x55FBD38D)),
                    elevation = 4.dp,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF13263E), Color(0xFF224169))
                                )
                            )
                            .padding(16.dp),
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.weight(1f).padding(end = 12.dp),
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = Color(0x2EFBD38D),
                                    elevation = 0.dp,
                                ) {
                                    Text(
                                        text = "${selectedPlan.category} · ${selectedPlan.durationDays} Days",
                                        color = GoldAccent,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                                    )
                                }
                                Text(
                                    text = selectedPlan.title,
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    text = selectedPlan.subtitle,
                                    color = Color(0xFFCBD5E1),
                                    fontSize = 12.5.sp,
                                    lineHeight = 18.sp,
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "${progress.percentComplete}%",
                                    color = GoldAccent,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.testTag("plan_progress_percent"),
                                )
                                Text(
                                    text = "${progress.completedCount} / ${progress.totalCount} read",
                                    color = Color(0xFFCBD5E1),
                                    fontSize = 11.5.sp,
                                )
                            }
                        }

                        LinearProgressIndicator(
                            progress = (progress.percentComplete / 100f).coerceIn(0f, 1f),
                            color = GoldAccent,
                            backgroundColor = Color(0x28FFFFFF),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(7.dp)
                                .clip(RoundedCornerShape(50)),
                        )

                        if (selectedPlan.sourceAttribution.isNotBlank()) {
                            Text(
                                text = selectedPlan.sourceAttribution,
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp,
                            )
                        }
                    }
                }
            }

            item(key = "plan_list_heading") {
                Text(
                    text = "TAP A PASSAGE TO READ & AUTO-CHECK OFF",
                    color = GoldAccent,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.7.sp,
                    modifier = Modifier.padding(top = 4.dp, start = 2.dp),
                )
            }

            items(
                items = selectedPlan.days,
                key = { day -> "${selectedPlan.id}_day_${day.dayNumber}" },
            ) { day ->
                PlanDayCard(
                    plan = selectedPlan,
                    day = day,
                    completedIds = progress.completedReadingIds,
                    onToggleReading = { reading ->
                        scope.launch {
                            ReadingPlanRepository.toggleReadingCompletion(
                                planId = selectedPlan.id,
                                dayNumber = day.dayNumber,
                                readingId = reading.id,
                            )
                        }
                    },
                    onOpenReading = { reading ->
                        scope.launch { ReadingPlanRepository.selectActivePlan(selectedPlan.id) }
                        onOpenReading(
                            ReadingPlanRepository.contextFor(
                                planId = selectedPlan.id,
                                day = day,
                                reading = reading,
                            )
                        )
                    },
                )
            }

            item(key = "plan_bottom_spacer") {
                Box(modifier = Modifier.height(20.dp + navBarBottom))
            }
        }
    }
}

@Composable
private fun PlanDayCard(
    plan: ReadingPlanDto,
    day: ReadingPlanDayDto,
    completedIds: Set<String>,
    onToggleReading: (ReadingPlanVerseRefDto) -> Unit,
    onOpenReading: (ReadingPlanVerseRefDto) -> Unit,
) {
    val allDone = day.readings.isNotEmpty() && day.readings.all { it.id in completedIds }
    val bgColor = if (allDone) Color(0x38458C6E) else Color(0xFF1B3149)
    val borderColor = if (allDone) Color(0x736EE7B7) else Color(0x40FBD38D)

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = bgColor,
        border = BorderStroke(1.dp, borderColor),
        elevation = 0.dp,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("plan_day_row_${plan.id}_${day.dayNumber}"),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "Day ${day.dayNumber}",
                    color = GoldAccent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = day.title,
                    color = Color.White,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }

            day.readings.forEach { reading ->
                val isDone = reading.id in completedIds
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0x18FFFFFF))
                        .clickable { onOpenReading(reading) }
                        .padding(horizontal = 10.dp, vertical = 9.dp)
                        .testTag("plan_open_${reading.id}"),
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(if (isDone) EmeraldCheck else Color(0x14FFFFFF))
                            .border(
                                width = 1.5.dp,
                                color = if (isDone) EmeraldCheck else Color(0x73FBD38D),
                                shape = CircleShape,
                            )
                            .clickable { onToggleReading(reading) }
                            .testTag("plan_toggle_${reading.id}"),
                    ) {
                        if (isDone) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Completed",
                                tint = Color(0xFF06281E),
                                modifier = Modifier.size(15.dp),
                            )
                        }
                    }

                    Text(
                        text = reading.label,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f),
                    )

                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color(0x28FBD38D),
                        elevation = 0.dp,
                    ) {
                        Text(
                            text = if (isDone) "Re-read" else "Read ›",
                            color = GoldAccent,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        )
                    }
                }
            }
        }
    }
}
