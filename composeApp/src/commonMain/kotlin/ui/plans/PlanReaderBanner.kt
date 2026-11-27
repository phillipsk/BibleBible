package ui.plans

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import data.ai.OnDeviceAiAssistant
import data.config.CloudSessionManager
import data.plans.ActivePlanReadingContext
import data.plans.ReadingPlanRepository

private val GoldAccent = Color(0xFFFBD38D)
private val EmeraldBg = Color(0xFF34D399)

@Composable
internal fun PlanReaderTopStrip(
    context: ActivePlanReadingContext,
    scrollProgress: Float,
    modifier: Modifier = Modifier,
) {
    val isCompleted = context.reading.id in ReadingPlanRepository.completedReadingIds
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF182A40))
            .testTag("plan_reader_top_strip"),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
                Text(
                    text = "${context.planTitle} · Day ${context.dayNumber}",
                    color = GoldAccent,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = context.dayTitle,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Surface(
                shape = RoundedCornerShape(50),
                color = if (isCompleted) EmeraldBg else Color(0x1CFFFFFF),
                elevation = 0.dp,
                modifier = Modifier
                    .semantics(mergeDescendants = true) {}
                    .testTag("plan_reader_status_pill"),
            ) {
                Text(
                    text = if (isCompleted) "Checked Off" else "Scroll to finish",
                    color = if (isCompleted) Color(0xFF06281E) else GoldAccent,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }
        }
        LinearProgressIndicator(
            progress = (if (isCompleted) 1f else scrollProgress).coerceIn(0.05f, 1f),
            color = if (isCompleted) EmeraldBg else GoldAccent,
            backgroundColor = Color(0x18FFFFFF),
            modifier = Modifier.fillMaxWidth().height(2.5.dp),
        )
    }
}

@Composable
internal fun PlanReaderCompletionBar(
    visible: Boolean,
    hasNextPassage: Boolean,
    onBackToPlan: () -> Unit,
    onNextPassage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + slideInVertically { it },
        exit = fadeOut() + slideOutVertically { it },
        modifier = modifier,
    ) {
        Surface(
            color = Color(0xFF132238),
            border = BorderStroke(1.2.dp, GoldAccent),
            elevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .testTag("plan_reader_completion_bar"),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Text(
                        text = "Reading Completed!",
                        color = GoldAccent,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "Progress saved automatically.",
                        color = Color(0xFFCBD5E1),
                        fontSize = 11.5.sp,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onBackToPlan,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = Color(0x1FFFFFFF),
                            contentColor = Color.White,
                        ),
                    ) {
                        Text("Plan", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                    if (hasNextPassage) {
                        Button(
                            onClick = onNextPassage,
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = GoldAccent,
                                contentColor = Color(0xFF09111E),
                            ),
                            modifier = Modifier.testTag("plan_reader_next_btn"),
                        ) {
                            Text("Next Passage", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun PlanReaderReflectionCard(
    context: ActivePlanReadingContext,
    chapterExcerpt: String,
    modifier: Modifier = Modifier,
) {
    var reflectionText by remember(context.reading.id) { mutableStateOf(context.reflectionPrompt) }
    var reflectionSource by remember(context.reading.id) { mutableStateOf("Curated Reflection") }

    LaunchedEffect(context.reading.id, chapterExcerpt, CloudSessionManager.onDeviceAiEnabled) {
        if (CloudSessionManager.onDeviceAiEnabled && chapterExcerpt.isNotBlank() && OnDeviceAiAssistant.isAvailable()) {
            val generated = OnDeviceAiAssistant.generateGroundedReflection(
                reference = context.reading.label,
                chapterExcerpt = chapterExcerpt,
            )
            if (!generated.isNullOrBlank()) {
                reflectionText = generated
                reflectionSource = OnDeviceAiAssistant.providerLabel()
            }
        }
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0x1CFBD38D),
        border = BorderStroke(1.dp, Color(0x4DFBD38D)),
        elevation = 0.dp,
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 20.dp)
            .testTag("plan_reader_reflection_card"),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(14.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = "Reflection Prompt",
                    color = GoldAccent,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                )
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color(0x26FBD38D),
                    elevation = 0.dp,
                ) {
                    Text(
                        text = reflectionSource,
                        color = GoldAccent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    )
                }
            }
            Text(
                text = reflectionText,
                color = Color(0xFFE2E8F0),
                fontSize = 13.5.sp,
                lineHeight = 20.sp,
            )
        }
    }
}
