package ui

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.BottomSheetValue
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.rememberBottomSheetState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import data.bible.BibleBooksResponse
import data.bible.BibleChapterUIState
import data.bible.BibleState
import data.bible.BibleVersions
import data.bible.BookData
import data.bible.Chapter
import data.bible.ReadingHistoryUIState
import data.bible.json.BUNDLED_BIBLE_BOOKS_JSON
import data.config.CloudFeatureFlagsDto
import data.config.CloudRuntimeConfigDto
import data.config.CloudSessionManager
import email.kevinphillips.biblebible.db.SelectReadingHistory
import kotlin.math.abs
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import ui.configs.ChapterTopBar
import ui.configs.DRAWER_AUTO_DISMISS_MS

@OptIn(ExperimentalTestApi::class, ExperimentalMaterialApi::class)
class ReaderPolishAndDeepLinkUiTest {

    @AfterTest
    fun tearDown() {
        CloudSessionManager.resetForTesting()
        BibleState.sortAZ = false
        BibleState.showHomePage = true
        BibleState.clearPendingTargetVerse()
        BibleState.clearHighlightedVerse()
    }

    @Test
    fun deepLinkCentersTargetVerseAndUsesGoldTextWithoutBackgroundFill() = runComposeUiTest {
        val books = Json.decodeFromString<BibleBooksResponse>(BUNDLED_BIBLE_BOOKS_JSON)
        BibleState.updateBooks(books)
        val psalms = books.data!!.first { it.bookId == "PSA" }
        BibleState.updateSelectedBook(psalms)

        val chapterText = (1..40).joinToString(" ") { v ->
            "[$v] Blessed are the undefiled in the way, who walk in the law of the Lord verse $v."
        }
        val chapter = BibleChapterUIState(
            bookId = 19,
            chapterId = 119,
            text = chapterText,
            version = "KJV",
        )
        BibleState.focusScriptureVerse(bookId = "PSA", chapter = 119, verseStart = 20, verseEnd = 20)

        lateinit var scrollState: ScrollState
        lateinit var readingState: ScriptureReadingState
        setContent {
            BibleBibleTheme {
                scrollState = rememberScrollState()
                readingState = rememberScriptureReadingState()
                Box(modifier = Modifier.size(360.dp, 400.dp)) {
                    BibleScriptures(
                        chapters = chapter,
                        scrollState = scrollState,
                        selectedFontSize = 20f,
                        onFontSizeChanged = {},
                        readingState = readingState,
                    )
                }
            }
        }

        mainClock.advanceTimeBy(120)
        waitForIdle()

        runOnIdle {
            assertNull(BibleState.pendingTargetVerse)
            val layout = requireNotNull(readingState.layout)
            val verseStartIdx = chapterText.indexOf("[20]")
            val line = layout.getLineForOffset(verseStartIdx)
            val verseScreenMidY = (layout.getLineTop(line) + layout.getLineBottom(line)) / 2f +
                readingState.contentInset - scrollState.value
            val viewportMidY = layout.size.height.coerceAtLeast(1)
            // Viewport box is 400dp tall at 1f density on Skiko (~400px)
            assertTrue(scrollState.value > 0, "Should scroll down to verse 20")
            assertTrue(abs(verseScreenMidY - 200f) < 60f, "Expected verse 20 near center (~200px), got $verseScreenMidY (viewport=${viewportMidY})")
        }

        val textNode = onNodeWithTag("ScriptureText").fetchSemanticsNode()
        val annotated = textNode.config[SemanticsProperties.Text].first()
        val goldSpans = annotated.spanStyles.filter { it.item.color == Color(0xFFFBD38D) }
        assertTrue(goldSpans.isNotEmpty(), "Expected gold text SpanStyle on deep-linked verse")
        assertTrue(
            goldSpans.all { it.item.background == Color.Unspecified },
            "Gold verse highlight must not apply a background box fill"
        )
    }

    @Test
    fun chapterTopBarShowsFullBookNameAndRemovesBibleBibleCenterMarketing() = runComposeUiTest {
        val chronBook = BookData(
            id = "2CH",
            name = "2 Chronicles",
            abbreviation = "2CH",
            chapters = listOf(
                Chapter(bookId = "2CH.intro"),
                Chapter(bookId = "2CH.1", number = "1", position = 1),
            ),
        )
        BibleState.updateSelectedBook(chronBook)

        setContent {
            BibleBibleTheme {
                ChapterTopBar(
                    showHomePage = false,
                    onBackClick = {},
                    showBottomSheet = {},
                )
            }
        }

        onNodeWithText("2 Chronicles").assertIsDisplayed()
        onNodeWithText("2 Chron").assertDoesNotExist()
        onNodeWithTag("chapter_top_bar_emblem").assertDoesNotExist()
        onNodeWithText("BibleBible").assertDoesNotExist()
    }

    @Test
    fun bookGridSectionSpacersFollowOtNtOrderAndFeatureFlag() = runComposeUiTest {
        val books = Json.decodeFromString<BibleBooksResponse>(BUNDLED_BIBLE_BOOKS_JSON)
        CloudSessionManager.applyRuntimeConfig(
            CloudRuntimeConfigDto(
                features = CloudFeatureFlagsDto(bookSectionPaddingDp = 12)
            )
        )
        BibleState.sortAZ = false

        setContent {
            BibleBibleTheme {
                Box(modifier = Modifier.size(412.dp, 915.dp)) {
                    BibleBookList(bookData = books.data)
                }
            }
        }

        waitForIdle()
        onNodeWithTag("book_section_spacer_0").assertExists()

        runOnIdle { BibleState.sortAZ = true }
        waitForIdle()
        onNodeWithTag("book_section_spacer_0").assertDoesNotExist()
    }

    @Test
    fun tabletBookGridSplitsOldAndNewTestamentsBalancedThreeByThree() = runComposeUiTest {
        val books = Json.decodeFromString<BibleBooksResponse>(BUNDLED_BIBLE_BOOKS_JSON)
        CloudSessionManager.applyRuntimeConfig(
            CloudRuntimeConfigDto(
                features = CloudFeatureFlagsDto(
                    bookSectionPaddingDp = 12,
                    bookGridTabletLayout = "split_testaments",
                )
            )
        )
        BibleState.sortAZ = false

        setContent {
            BibleBibleTheme {
                Box(modifier = Modifier.size(1200.dp, 860.dp)) {
                    BibleBookList(bookData = books.data)
                }
            }
        }

        waitForIdle()
        onNodeWithTag("tablet_split_testaments_grid").assertIsDisplayed()
        onNodeWithText("Old Testament").assertIsDisplayed()
        onNodeWithText("New Testament").assertIsDisplayed()
        onNodeWithTag("split_old_testament_spacer_0").assertExists()
    }

    @Test
    fun settingsBottomSheetHas20sTimeoutAndNavigatesOnHistoryTap() = runComposeUiTest {
        assertEquals(20_000L, DRAWER_AUTO_DISMISS_MS)
        val books = Json.decodeFromString<BibleBooksResponse>(BUNDLED_BIBLE_BOOKS_JSON)
        BibleState.updateBooks(books)
        BibleState.showHomePage = true
        BibleState.updateReadingHistory(
            listOf(SelectReadingHistory(DATE = "2026-11-23", b = "43", c = "3"))
        )

        lateinit var scaffoldState: androidx.compose.material.BottomSheetScaffoldState
        setContent {
            BibleBibleTheme {
                scaffoldState = androidx.compose.material.rememberBottomSheetScaffoldState(
                    bottomSheetState = rememberBottomSheetState(BottomSheetValue.Expanded)
                )
                Box(modifier = Modifier.size(420.dp, 680.dp)) {
                    androidx.compose.material.BottomSheetScaffold(
                        scaffoldState = scaffoldState,
                        sheetPeekHeight = 0.dp,
                        sheetContent = {
                            ui.configs.BottomSheetConfigView(
                                bibleVersionsList = BibleVersions(),
                                showAISummary = false,
                                bottomSheetState = scaffoldState.bottomSheetState,
                                autoDismissDelayMs = 0L,
                                onInitialLoad = {},
                            )
                        },
                    ) {
                        Box(modifier = Modifier.size(420.dp, 680.dp))
                    }
                }
            }
        }

        waitForIdle()
        onNodeWithTag("reading_history_item_0").assertIsDisplayed().performClick()
        mainClock.advanceTimeBy(300)
        waitForIdle()

        runOnIdle {
            assertFalse(BibleState.showHomePage)
            assertEquals("JHN", BibleState.selectedBook.bookId)
            assertEquals("JHN.3", BibleState.selectedChapter)
            assertFalse(scaffoldState.bottomSheetState.isExpanded)
        }
    }
}
