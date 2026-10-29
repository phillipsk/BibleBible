package data.bibleIQ

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BibleIQDataModelTest {

    @Test
    fun testUpdateBibleVersions() {
        // BibleIQVersion(table, versionId, abbreviation, version, language)
        val versions = BibleIQVersions(
            data = listOf(
                BibleIQVersion(null, null, "KJV", "King James Version", null),
                BibleIQVersion(null, null, "NIV", "New International Version", null),
                BibleIQVersion(null, null, "ASV", "American Standard Version", null)
            )
        )
        
        BibleIQDataModel.updateBibleVersions(versions)
        assertEquals(3, BibleIQDataModel.bibleVersions.data.size)
    }

    @Test
    fun testUpdateBibleChapter() {
        val verses = listOf(
            BibleChapter(id = "1", b = "1", c = "1", v = "1", t = "In the beginning..."),
            BibleChapter(id = "2", b = "1", c = "1", v = "2", t = "And the earth...")
        )
        val chapterCount = ChapterCount(chapterCount = 50)
        
        BibleIQDataModel.updateBibleChapter(verses, chapterCount, "KJV")
        
        val uiState = BibleIQDataModel.bibleChapter
        assertEquals(1, uiState?.bookId)
        assertEquals(1, uiState?.chapterId)
        assertTrue(uiState?.text?.contains("[1] In the beginning...") == true)
        assertTrue(uiState?.text?.contains("[2] And the earth...") == true)
        assertEquals(50, uiState?.chapterList?.size)
    }

    @Test
    fun testUpdateBibleChapterArabic() {
        val verses = listOf(
            BibleChapter(id = "1", b = "1", c = "1", v = "1", t = "In the beginning...")
        )
        BibleIQDataModel.updateBibleChapter(verses, ChapterCount(1), "SVD")
        
        val uiState = BibleIQDataModel.bibleChapter
        assertEquals("1 In the beginning...", uiState?.text)
    }

    @Test
    fun testSortToggle() {
        BibleIQDataModel.sortAZ = false
        assertEquals("OT-NT", BibleIQDataModel.selectedSortType)
        
        BibleIQDataModel.sortAZ = true
        assertEquals("A-Z", BibleIQDataModel.selectedSortType)
    }
}
