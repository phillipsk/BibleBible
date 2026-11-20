package ui.inhouse

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import data.analytics.AnalyticsTracker
import data.inhouse.InHouseWordDto
import data.inhouse.InHouseWordsRepository

@Composable
internal fun InHouseWordsScreen(
    onBack: () -> Unit,
    onReadChapter: (InHouseWordDto) -> Unit,
    initialCategory: String = "All",
    initialWords: List<InHouseWordDto>? = null,
) {
    var selectedCategory by remember(initialCategory) { mutableStateOf(initialCategory) }
    var categories by remember { mutableStateOf(InHouseWordsRepository.defaultCategories) }
    var allWords by remember(initialWords) {
        mutableStateOf(initialWords ?: InHouseWordsRepository.fallbackWords)
    }
    var isLoading by remember { mutableStateOf(initialWords == null) }

    LaunchedEffect(initialWords) {
        if (initialWords != null) {
            isLoading = false
            return@LaunchedEffect
        }
        isLoading = true
        val feed = InHouseWordsRepository.fetchWords(category = "All")
        if (feed.categories.isNotEmpty()) {
            categories = feed.categories
        }
        if (feed.words.isNotEmpty()) {
            allWords = feed.words
        }
        isLoading = false
    }

    val visibleWords = remember(allWords, selectedCategory) {
        InHouseWordsRepository.filterByCategory(allWords, selectedCategory)
    }

    val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1F2F42))
            .testTag("in_house_words_screen"),
    ) {
        // Dark slate top bar matching BibleBible surface (#2C3E50)
        Surface(
            color = Color(0xFF2C3E50),
            elevation = 4.dp,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = statusTop + 6.dp, bottom = 10.dp, start = 8.dp, end = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("in_house_back_button"),
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back to Home",
                        tint = Color.White,
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "In-House Words",
                        style = MaterialTheme.typography.h6.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 19.sp,
                        ),
                    )
                    Text(
                        text = "Church Sermons, Bible Studies & Morning Prayers",
                        color = Color(0xFFC8D8E8),
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        // Category Filter Pills with subtle right-edge fade
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF253649))
                .padding(vertical = 10.dp),
        ) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(categories) { category ->
                    val selected = category.equals(selectedCategory, ignoreCase = true)
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = if (selected) Color(0xFFFBD38D) else Color(0xFF182534),
                        border = BorderStroke(
                            1.dp,
                            if (selected) Color(0xFFFBD38D) else Color(0x44FFFFFF),
                        ),
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable {
                                selectedCategory = category
                                AnalyticsTracker.track(
                                    "inhouse_category_selected",
                                    "in_house_words",
                                    mapOf("category" to category),
                                )
                            }
                            .testTag("in_house_category_$category"),
                    ) {
                        Text(
                            text = category,
                            color = if (selected) Color(0xFF0F172A) else Color(0xFFE2E8F0),
                            fontSize = 13.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                        )
                    }
                }
            }
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .width(28.dp)
                    .height(34.dp)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(Color.Transparent, Color(0xFF253649))
                        )
                    )
            )
        }

        if (isLoading && visibleWords.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = Color(0xFFFBD38D))
            }
        } else if (visibleWords.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "No church teachings in \"$selectedCategory\" yet.",
                    color = Color(0xFFCBD5E1),
                    fontSize = 15.sp,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 14.dp,
                    bottom = 24.dp + navBottom,
                ),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                items(visibleWords, key = { it.id }) { word ->
                    InHouseWordCard(
                        word = word,
                        onReadChapter = {
                            AnalyticsTracker.track(
                                "inhouse_read_chapter_clicked",
                                "in_house_words",
                                mapOf(
                                    "id" to word.id,
                                    "category" to word.category,
                                    "bookUsfm" to word.bookUsfm,
                                    "chapter" to word.chapter.toString(),
                                ),
                            )
                            onReadChapter(word)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun InHouseWordCard(
    word: InHouseWordDto,
    onReadChapter: () -> Unit,
) {
    val badgeColor = when (word.category.lowercase()) {
        "bible study" -> Color(0xFF1D4D6B)
        "morning prayer" -> Color(0xFF3E3563)
        else -> Color(0xFF1A4D45)
    }

    Card(
        shape = RoundedCornerShape(18.dp),
        backgroundColor = Color(0xFF162433),
        border = BorderStroke(1.dp, Color(0x38FBD38D)),
        elevation = 4.dp,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("in_house_card_${word.id}"),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Top badges: Category + Source
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = badgeColor,
                ) {
                    Text(
                        text = word.category,
                        color = Color(0xFFFBD38D),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
                if (word.sourceLabel.isNotBlank()) {
                    Text(
                        text = word.sourceLabel,
                        color = Color(0xFF94A3B8),
                        fontSize = 11.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            // Title
            Text(
                text = word.title,
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 22.sp,
            )

            // Key Principle Callout with Warm Gold Accent Bar
            if (word.keyPrinciple.isNotBlank()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF1F3347))
                        .padding(10.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .width(3.dp)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color(0xFFFBD38D))
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(
                            text = "KEY PRINCIPLE",
                            color = Color(0xFFFBD38D),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp,
                        )
                        Text(
                            text = word.keyPrinciple,
                            color = Color(0xFFF1F5F9),
                            fontSize = 13.5.sp,
                            lineHeight = 19.sp,
                        )
                    }
                }
            }

            // Practical Summary
            if (word.summary.isNotBlank()) {
                Text(
                    text = word.summary,
                    color = Color(0xFFCBD5E1),
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                )
            }

            // Bottom Action Row: Scripture Reference + Dark-on-Gold Read Chapter CTA
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = Color(0xFFFBD38D),
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = word.scriptureReference.ifBlank { "${word.bookUsfm} ${word.chapter}" },
                        color = Color(0xFFFBD38D),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Button(
                    onClick = onReadChapter,
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = Color(0xFFFBD38D),
                        contentColor = Color(0xFF0F172A),
                    ),
                    shape = RoundedCornerShape(50),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("in_house_read_chapter_${word.id}"),
                ) {
                    Text(
                        text = "Read Chapter",
                        color = Color(0xFF0F172A),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.5.sp,
                    )
                }
            }
        }
    }
}
