import data.notifications.findVerseCharacterRange
import data.notifications.parseVerseRange
import data.shorts.BiblicalShortDto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class ShortsDeepLinkAndHighlightTest {

    @Test
    fun parseVerseRangeHandlesSingleHyphenAndEnDashReferences() {
        assertEquals(14..14, parseVerseRange("Colossians 3:14", fallbackVerse = 1))
        assertEquals(6..7, parseVerseRange("Philippians 4:6-7", fallbackVerse = 6))
        assertEquals(6..7, parseVerseRange("Philippians 4:6\u20137", fallbackVerse = 6))
        assertEquals(3..3, parseVerseRange("Joshua 1", fallbackVerse = 3))
    }

    @Test
    fun findVerseCharacterRangeHighlightsSingleRangeAndFinalVerses() {
        val kjvChapter = "[1] If ye then be risen with Christ.\n" +
            "[6] In every thing by prayer.\n" +
            "[7] And the peace of God.\n" +
            "[14] And above all these things put on charity, which is the bond of perfectness.\n" +
            "[17] And whatsoever ye do in word or deed.\n\n"

        val v1Range = findVerseCharacterRange(kjvChapter, verseStart = 1, verseEnd = 1, version = "kjv")
        assertNotNull(v1Range)
        assertEquals("[1] If ye then be risen with Christ.", kjvChapter.substring(v1Range.first, v1Range.last + 1))

        val multiRange = findVerseCharacterRange(kjvChapter, verseStart = 6, verseEnd = 7, version = "kjv")
        assertNotNull(multiRange)
        assertEquals(
            "[6] In every thing by prayer.\n[7] And the peace of God.",
            kjvChapter.substring(multiRange.first, multiRange.last + 1),
        )

        val v14Range = findVerseCharacterRange(kjvChapter, verseStart = 14, verseEnd = 14, version = "kjv")
        assertNotNull(v14Range)
        assertEquals(
            "[14] And above all these things put on charity, which is the bond of perfectness.",
            kjvChapter.substring(v14Range.first, v14Range.last + 1),
        )

        val lastRange = findVerseCharacterRange(kjvChapter, verseStart = 17, verseEnd = 17, version = "kjv")
        assertNotNull(lastRange)
        assertEquals(
            "[17] And whatsoever ye do in word or deed.",
            kjvChapter.substring(lastRange.first, lastRange.last + 1),
        )
    }

    @Test
    fun inAppCutoffTrimsPromoEndCardWhilePreservingLegacyClips() {
        val base = BiblicalShortDto(
            id = "01-col3-bond-that-holds",
            order = 1,
            title = "The Bond That Holds",
            category = "Biblical Words",
            durationSeconds = 20,
            scriptureReference = "Colossians 3:14",
            bookId = 51,
            bookUsfm = "COL",
            chapter = 3,
            verse = 14,
            keyVerseText = "And above all these things put on charity...",
            narrative = "Steadfast love binds every virtue.",
            lesson = "Let patient charity lead today.",
        )

        assertNull(base.inAppCutoffSeconds())
        assertEquals(20, base.effectiveInAppDurationSeconds())

        val withExplicitBody = base.copy(bodyDurationSeconds = 17.67, promoEndCardSeconds = 2.5, scene2StartSeconds = 7)
        assertEquals(17.67, withExplicitBody.inAppCutoffSeconds())
        assertEquals(17, withExplicitBody.effectiveInAppDurationSeconds())
        assertEquals(7, withExplicitBody.effectiveScene2StartSeconds())

        val withPromoSubtraction = base.copy(promoEndCardSeconds = 2.5)
        assertEquals(17.5, withPromoSubtraction.inAppCutoffSeconds())
        assertEquals(17, withPromoSubtraction.effectiveInAppDurationSeconds())
    }
}
