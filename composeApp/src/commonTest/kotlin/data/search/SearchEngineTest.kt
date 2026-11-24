package data.search

import data.bible.BookData
import data.bible.Chapter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SearchEngineTest {

    private val sampleBooks = listOf(
        BookData(id = "GEN", name = "Genesis", chapters = listOf(Chapter(bookId = "GEN.intro"), Chapter(bookId = "GEN", number = "1", position = 1))),
        BookData(id = "PSA", name = "Psalms", chapters = listOf(Chapter(bookId = "PSA.intro"), Chapter(bookId = "PSA", number = "23", position = 23))),
        BookData(id = "JHN", name = "John", chapters = listOf(Chapter(bookId = "JHN.intro"), Chapter(bookId = "JHN", number = "3", position = 3))),
        BookData(id = "1CO", name = "1 Corinthians", chapters = listOf(Chapter(bookId = "1CO.intro"), Chapter(bookId = "1CO", number = "13", position = 13))),
        BookData(id = "PHP", name = "Philippians", chapters = listOf(Chapter(bookId = "PHP.intro"), Chapter(bookId = "PHP", number = "4", position = 4))),
    )

    @Test
    fun parseDirectReferenceHandlesChaptersAndVerseRanges() {
        val john316 = BundledTopicIndex.parseDirectReference("John 3:16")
        assertNotNull(john316)
        assertEquals("JHN", john316.bookId)
        assertEquals(3, john316.chapter)
        assertEquals(16, john316.verseStart)
        assertEquals(16, john316.verseEnd)

        val corRange = BundledTopicIndex.parseDirectReference("1 Cor 13:4-7")
        assertNotNull(corRange)
        assertEquals("1CO", corRange.bookId)
        assertEquals(13, corRange.chapter)
        assertEquals(4, corRange.verseStart)
        assertEquals(7, corRange.verseEnd)

        val psalm23 = BundledTopicIndex.parseDirectReference("Psalm 23")
        assertNotNull(psalm23)
        assertEquals("PSA", psalm23.bookId)
        assertEquals(23, psalm23.chapter)

        assertNull(BundledTopicIndex.parseDirectReference("anxious about work"))
    }

    @Test
    fun immediateLocalSearchFiltersBooksAndPopulatesTopicMasonry() {
        val johnState = SearchRepository.searchImmediateLocal(
            query = "John",
            availableBooks = sampleBooks,
            includeMasonry = true,
        )
        assertTrue(johnState.books.any { it.bookId == "JHN" })

        val anxietyState = SearchRepository.searchImmediateLocal(
            query = "anxious",
            availableBooks = sampleBooks,
            includeMasonry = true,
        )
        assertTrue(anxietyState.masonryCards.isNotEmpty())
        assertTrue(anxietyState.mappedTopics.isNotEmpty())
    }

    @Test
    fun disabledMasonryFlagSuppressesTopicCardsWhileKeepingBookMatches() {
        val state = SearchRepository.searchImmediateLocal(
            query = "Psalms peace",
            availableBooks = sampleBooks,
            includeMasonry = false,
        )
        assertTrue(state.masonryCards.isEmpty())

        val bookOnly = SearchRepository.searchImmediateLocal(
            query = "Psalms",
            availableBooks = sampleBooks,
            includeMasonry = false,
        )
        assertTrue(bookOnly.books.any { it.bookId == "PSA" })
        assertTrue(bookOnly.masonryCards.isEmpty())
    }
}
