@file:OptIn(ExperimentalMaterialApi::class)

package ui.search

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import data.analytics.AnalyticsTracker
import data.bible.BibleState
import data.bible.BookData
import data.config.CloudSessionManager
import data.plans.ReadingPlanRepository
import data.search.SearchBookHitDto
import data.search.SearchDirectReferenceDto
import data.search.SearchFilterTab
import data.search.SearchMasonryCardDto
import data.search.SearchRepository
import data.search.SearchResultUiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val GoldAccent = Color(0xFFFBD38D)
private val DeepNavy = Color(0xFF17293D)

private fun parseAccentColor(hex: String?, fallback: Color = Color(0xFF24426B)): Color {
    val clean = hex?.trim()?.removePrefix("#") ?: return fallback
    if (clean.length != 6) return fallback
    val rgb = clean.toLongOrNull(16) ?: return fallback
    return Color((0xFF000000L or rgb).toInt())
}

@Composable
internal fun ElasticSearchTriggerBar(
    onActivateSearch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onActivateSearch,
        shape = RoundedCornerShape(16.dp),
        color = DeepNavy,
        border = BorderStroke(1.2.dp, Color(0x66FBD38D)),
        elevation = 2.dp,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp)
            .testTag("elastic_search_trigger"),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search Bible",
                tint = GoldAccent,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = "Search books, topics, or how you feel...",
                color = Color(0xFFCBD5E1),
                fontSize = 13.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Surface(
                shape = RoundedCornerShape(50),
                color = Color(0x24FBD38D),
                elevation = 0.dp,
            ) {
                Text(
                    text = "Search",
                    color = GoldAccent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                )
            }
        }
    }
}

@Composable
internal fun SpotlightSearchScreen(
    onBack: () -> Unit,
    onSelectBook: (BookData) -> Unit,
    onJumpToReference: (bookId: String, bookNumber: Int, bookName: String, chapter: Int, verseStart: Int, verseEnd: Int, source: String) -> Unit,
    onOpenPlan: (String) -> Unit,
    autoFocusInput: Boolean = true,
    modifier: Modifier = Modifier,
) {
    var query by remember { mutableStateOf("") }
    var activeFilter by remember { mutableStateOf(SearchFilterTab.ALL) }
    var searchState by remember { mutableStateOf(SearchResultUiState()) }
    val focusRequester = remember { FocusRequester() }
    val scope = rememberCoroutineScope()
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    LaunchedEffect(autoFocusInput) {
        if (autoFocusInput) {
            runCatching { focusRequester.requestFocus() }
        }
    }

    LaunchedEffect(
        query,
        BibleState.selectedVersion,
        CloudSessionManager.onDeviceAiEnabled,
        CloudSessionManager.cloudSearchAiFallbackEnabled,
        CloudSessionManager.searchMasonryTopicsEnabled,
    ) {
        val clean = query.trim()
        if (clean.isEmpty()) {
            searchState = SearchResultUiState()
            return@LaunchedEffect
        }

        // Immediate 0 ms local pass
        val immediate = SearchRepository.searchImmediateLocal(
            query = clean,
            availableBooks = BibleState.uiBooks.data.orEmpty(),
            includeMasonry = CloudSessionManager.searchMasonryTopicsEnabled,
        )
        searchState = immediate.copy(isLoadingCloud = CloudSessionManager.isGatewayConfigured)

        // Debounced Tier 1 (On-Device AI) + Tier 2/3 (GCP Cloud Run) enrichment
        delay(160L)
        val enriched = SearchRepository.searchEnriched(
            query = clean,
            versionId = BibleState.selectedVersion,
            availableBooks = BibleState.uiBooks.data.orEmpty(),
        )
        searchState = enriched
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF1A293B))
            .padding(top = statusBarTop)
            .testTag("spotlight_search_screen"),
    ) {
        // Pinned Spotlight Top Bar
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF152334))
                .padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Surface(
                    onClick = onBack,
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0x26FBD38D),
                    elevation = 0.dp,
                    modifier = Modifier.testTag("spotlight_back_btn"),
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
                            text = "Back",
                            color = GoldAccent,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
                Text(
                    text = "Spotlight Search",
                    color = GoldAccent,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Cancel",
                    color = Color(0xFFCBD5E1),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clickable { onBack() }
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                )
            }

            // Gold-Rimmed Search Box
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = DeepNavy,
                border = BorderStroke(1.5.dp, GoldAccent),
                elevation = 0.dp,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 11.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = GoldAccent,
                        modifier = Modifier.size(18.dp),
                    )
                    Box(modifier = Modifier.weight(1f)) {
                        if (query.isEmpty()) {
                            Text(
                                text = "Book, verse (John 3:16), topic, or mood...",
                                color = Color(0xFF94A3B8),
                                fontSize = 14.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        BasicTextField(
                            value = query,
                            onValueChange = { query = it },
                            singleLine = true,
                            textStyle = TextStyle(
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                            ),
                            cursorBrush = SolidColor(GoldAccent),
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester)
                                .testTag("spotlight_search_input"),
                        )
                    }
                    if (query.isNotEmpty()) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(Color(0x28FFFFFF))
                                .clickable { query = "" }
                                .testTag("spotlight_clear_btn"),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear search",
                                tint = GoldAccent,
                                modifier = Modifier.size(14.dp),
                            )
                        }
                    }
                }
            }

            // Sample C Filter Tabs
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
            ) {
                SearchFilterTab.entries.forEach { tab ->
                    val selected = activeFilter == tab
                    Surface(
                        onClick = { activeFilter = tab },
                        shape = RoundedCornerShape(50),
                        color = if (selected) GoldAccent else Color(0x16FFFFFF),
                        border = if (selected) null else BorderStroke(1.dp, Color(0x33FBD38D)),
                        elevation = 0.dp,
                        modifier = Modifier.testTag("spotlight_filter_${tab.name.lowercase()}"),
                    ) {
                        Text(
                            text = tab.label,
                            color = if (selected) Color(0xFF09111E) else Color(0xFFE2E8F0),
                            fontSize = 12.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        )
                    }
                }
            }
        }

        if (searchState.isLoadingCloud) {
            LinearProgressIndicator(
                color = GoldAccent,
                backgroundColor = Color(0x22FBD38D),
                modifier = Modifier.fillMaxWidth().height(2.dp),
            )
        }

        // Scrollable Results Area
        Column(
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 14.dp, end = 14.dp, top = 14.dp, bottom = 20.dp + navBarBottom),
        ) {
            if (query.trim().isEmpty()) {
                SpotlightEmptySuggestions(
                    onPickQuery = { picked -> query = picked },
                )
            } else {
                val showBooks = activeFilter == SearchFilterTab.ALL || activeFilter == SearchFilterTab.BOOKS
                val showVerses = activeFilter == SearchFilterTab.ALL || activeFilter == SearchFilterTab.VERSES
                val showTopics = (activeFilter == SearchFilterTab.ALL || activeFilter == SearchFilterTab.TOPICS || activeFilter == SearchFilterTab.VERSES) &&
                    CloudSessionManager.searchMasonryTopicsEnabled

                if (searchState.mappedTopics.isNotEmpty() && showTopics) {
                    SearchAiRoutingPill(
                        aiSource = searchState.aiSource,
                        providerLabel = searchState.aiProviderLabel,
                        mappedTopics = searchState.mappedTopics,
                    )
                }

                if (showVerses && searchState.directReference != null) {
                    DirectReferenceJumpCard(
                        ref = searchState.directReference!!,
                        onClick = {
                            val r = searchState.directReference!!
                            AnalyticsTracker.track(
                                "search_direct_ref_clicked",
                                "search",
                                mapOf("reference" to r.reference),
                            )
                            onJumpToReference(
                                r.bookId,
                                r.bookNumber,
                                r.bookName,
                                r.chapter,
                                r.verseStart,
                                r.verseEnd,
                                "search_direct_ref",
                            )
                        },
                    )
                }

                if (showBooks && searchState.books.isNotEmpty()) {
                    SearchBooksSection(
                        books = searchState.books,
                        onSelectBook = onSelectBook,
                    )
                }

                if (showTopics && searchState.masonryCards.isNotEmpty()) {
                    val filteredCards = when (activeFilter) {
                        SearchFilterTab.VERSES -> searchState.masonryCards.filter { it.kind == "verse" || it.reference.isNotBlank() }
                        else -> searchState.masonryCards
                    }
                    if (filteredCards.isNotEmpty()) {
                        StaggeredMasonryWall(
                            cards = filteredCards,
                            onCardClick = { card ->
                                AnalyticsTracker.track(
                                    "search_masonry_card_clicked",
                                    "search",
                                    mapOf("id" to card.id, "ref" to card.reference),
                                )
                                if (card.size == "wide" && !card.planId.isNullOrBlank() && CloudSessionManager.readingPlansEnabled) {
                                    scope.launch { ReadingPlanRepository.selectActivePlan(card.planId) }
                                    onOpenPlan(card.planId)
                                } else {
                                    onJumpToReference(
                                        card.bookId,
                                        card.bookNumber,
                                        card.reference.substringBeforeLast(" ").ifBlank { card.bookId },
                                        card.chapter,
                                        card.verseStart,
                                        card.verseEnd,
                                        "search_masonry",
                                    )
                                }
                            },
                        )
                    }
                }

                val hasAnyResults = (showVerses && searchState.directReference != null) ||
                    (showBooks && searchState.books.isNotEmpty()) ||
                    (showTopics && searchState.masonryCards.isNotEmpty())

                if (!hasAnyResults && !searchState.isLoadingCloud) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0x16FFFFFF),
                        border = BorderStroke(1.dp, Color(0x28FBD38D)),
                        modifier = Modifier.fillMaxWidth().testTag("search_empty_results"),
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(18.dp),
                        ) {
                            Text(
                                text = "No exact matches yet for \"${query.trim()}\"",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = "Try a book name (John, Psalms), a topic (Peace, Grace, Wisdom), or a verse reference (Philippians 4:6).",
                                color = Color(0xFFCBD5E1),
                                fontSize = 12.5.sp,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SpotlightEmptySuggestions(
    onPickQuery: (String) -> Unit,
) {
    val quickPrompts = listOf(
        "Peace",
        "Anxiety",
        "Grace",
        "Overwhelmed at work",
        "Wisdom",
        "Forgiveness",
        "John 3:16",
        "Psalms",
    )
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Try searching by book, reference, topic, or mood",
            color = GoldAccent,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Bold,
        )
        quickPrompts.chunked(3).forEach { rowChips ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                rowChips.forEach { label ->
                    Surface(
                        onClick = { onPickQuery(label) },
                        shape = RoundedCornerShape(50),
                        color = Color(0x1EFBD38D),
                        border = BorderStroke(1.dp, Color(0x4DFBD38D)),
                        elevation = 0.dp,
                    ) {
                        Text(
                            text = label,
                            color = GoldAccent,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchAiRoutingPill(
    aiSource: String,
    providerLabel: String,
    mappedTopics: List<String>,
) {
    val isOnDevice = aiSource == "on_device"
    val isCloudAi = aiSource == "cloud_gemini"
    val pillColor = if (isOnDevice) Color(0x2234D399) else Color(0x22FBD38D)
    val borderColor = if (isOnDevice) Color(0x666EE7B7) else Color(0x55FBD38D)
    val textColor = if (isOnDevice) Color(0xFF6EE7B7) else GoldAccent
    val prefix = when {
        isOnDevice -> providerLabel.ifBlank { "On-Device AI" }
        isCloudAi -> "Cloud Run AI"
        else -> "Topic Match"
    }
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = pillColor,
        border = BorderStroke(1.dp, borderColor),
        elevation = 0.dp,
        modifier = Modifier.fillMaxWidth().testTag("search_ai_routing_pill"),
    ) {
        Text(
            text = "$prefix · Mapped to ${mappedTopics.joinToString(", ")}",
            color = textColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        )
    }
}

@Composable
private fun DirectReferenceJumpCard(
    ref: SearchDirectReferenceDto,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFF1E3A5F),
        border = BorderStroke(1.5.dp, GoldAccent),
        elevation = 4.dp,
        modifier = Modifier.fillMaxWidth().testTag("search_direct_ref_card"),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.horizontalGradient(listOf(Color(0xFF19304F), Color(0xFF2F5380))))
                .padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f)) {
                Text(
                    text = "DIRECT SCRIPTURE JUMP",
                    color = GoldAccent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp,
                )
                Text(
                    text = ref.reference,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Open in ${BibleState.selectedVersion} and center on this passage",
                    color = Color(0xFFDCE6F2),
                    fontSize = 12.sp,
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Open passage",
                tint = GoldAccent,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

@Composable
private fun SearchBooksSection(
    books: List<SearchBookHitDto>,
    onSelectBook: (BookData) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Books",
            color = GoldAccent,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Bold,
        )
        books.chunked(3).forEach { rowBooks ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                for (slot in 0 until 3) {
                    val hit = rowBooks.getOrNull(slot)
                    if (hit != null) {
                        val rawColor = BibleState.getBibleBookColor(hit.bookNumber)
                        val solid = rawColor.compositeOver(Color(0xFF1A2838))
                        Surface(
                            onClick = {
                                val resolved = BibleState.resolveBookData(
                                    bookId = hit.bookId,
                                    bookNumber = hit.bookNumber,
                                    fallbackName = hit.bookName,
                                    minChapters = hit.chapters,
                                )
                                AnalyticsTracker.trackBookSelected(
                                    bookId = resolved.bookId,
                                    bookName = resolved.name ?: hit.bookName,
                                    source = "spotlight_search",
                                )
                                onSelectBook(resolved)
                            },
                            shape = RoundedCornerShape(14.dp),
                            color = solid,
                            border = BorderStroke(1.dp, Color(0x36FFFFFF)),
                            elevation = 2.dp,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("search_book_tile_${hit.bookId}"),
                        ) {
                            Text(
                                text = hit.bookName,
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp),
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun StaggeredMasonryWall(
    cards: List<SearchMasonryCardDto>,
    onCardClick: (SearchMasonryCardDto) -> Unit,
) {
    val heroCard = cards.firstOrNull { it.size == "hero" }
    val wideCard = cards.firstOrNull { it.size == "wide" }
    val masonryPool = cards.filter { it !== heroCard && it !== wideCard }
    val leftColumn = masonryPool.filterIndexed { idx, _ -> idx % 2 == 0 }
    val rightColumn = masonryPool.filterIndexed { idx, _ -> idx % 2 == 1 }

    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth().testTag("search_masonry_wall"),
    ) {
        Text(
            text = "Topics & Passages",
            color = GoldAccent,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Bold,
        )

        if (heroCard != null) {
            MasonryTopicCard(
                card = heroCard,
                isTall = true,
                onClick = { onCardClick(heroCard) },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        if (masonryPool.isNotEmpty()) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    leftColumn.forEach { card ->
                        MasonryTopicCard(
                            card = card,
                            isTall = card.size == "tall",
                            onClick = { onCardClick(card) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    rightColumn.forEach { card ->
                        MasonryTopicCard(
                            card = card,
                            isTall = card.size == "tall",
                            onClick = { onCardClick(card) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }

        if (wideCard != null) {
            MasonryTopicCard(
                card = wideCard,
                isTall = false,
                onClick = { onCardClick(wideCard) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun MasonryTopicCard(
    card: SearchMasonryCardDto,
    isTall: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val baseColor = parseAccentColor(card.accentHex)
    val solidColor = baseColor.copy(alpha = 0.92f).compositeOver(Color(0xFF152334))
    val brush = Brush.verticalGradient(
        colors = listOf(solidColor, Color(0xFF132238)),
    )

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = solidColor,
        border = BorderStroke(1.dp, Color(0x4DFBD38D)),
        elevation = 3.dp,
        modifier = modifier.testTag("masonry_card_${card.id}"),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .background(brush)
                .padding(horizontal = 14.dp, vertical = if (isTall) 14.dp else 12.dp),
        ) {
            Text(
                text = card.tag.uppercase(),
                color = GoldAccent,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.6.sp,
            )
            Text(
                text = card.title,
                color = Color.White,
                fontSize = if (card.size == "hero") 15.sp else 13.5.sp,
                fontWeight = FontWeight.Bold,
            )
            if (isTall || card.size == "hero" || card.size == "wide") {
                Text(
                    text = card.excerpt,
                    color = Color(0xFFDCE6F2),
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    maxLines = if (card.size == "hero") 4 else 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
            ) {
                Text(
                    text = if (card.size == "wide" && !card.planId.isNullOrBlank()) "Open Plan" else card.reference,
                    color = GoldAccent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = GoldAccent,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}
