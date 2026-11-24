package data.notifications

internal data class ScriptureDeepLink(
    val bookUsfm: String = "",
    val bookNumber: Int? = null,
    val chapter: Int = 1,
    val verse: Int = 1,
    val version: String? = null,
    val fetchDailyIfMissing: Boolean = false,
)

private val canonicalBookUsfms = listOf(
    "GEN", "EXO", "LEV", "NUM", "DEU", "JOS", "JDG", "RUT", "1SA", "2SA",
    "1KI", "2KI", "1CH", "2CH", "EZR", "NEH", "EST", "JOB", "PSA", "PRO",
    "ECC", "SNG", "ISA", "JER", "LAM", "EZK", "DAN", "HOS", "JOL", "AMO",
    "OBA", "JON", "MIC", "NAM", "HAB", "ZEP", "HAG", "ZEC", "MAL",
    "MAT", "MRK", "LUK", "JHN", "ACT", "ROM", "1CO", "2CO", "GAL", "EPH",
    "PHP", "COL", "1TH", "2TH", "1TI", "2TI", "TIT", "PHM", "HEB", "JAS",
    "1PE", "2PE", "1JN", "2JN", "3JN", "JUD", "REV",
)

internal fun usfmForBookNumber(bookNumber: Int?): String? =
    bookNumber?.takeIf { it in 1..canonicalBookUsfms.size }?.let { canonicalBookUsfms[it - 1] }

internal fun bookNumberForUsfm(bookUsfm: String?): Int? {
    val clean = bookUsfm?.trim()?.uppercase()?.substringBefore('.') ?: return null
    return canonicalBookUsfms.indexOf(clean).takeIf { it >= 0 }?.plus(1)
}

internal fun parseVotdNotificationPayload(extras: Map<String, String?>): ScriptureDeepLink? {
    if (extras.isEmpty()) return null
    val normalized = extras.entries.associate { (k, v) ->
        k.trim().lowercase() to v?.trim()
    }

    val type = normalized["type"]
    val rawUsfm = normalized["votd_book_usfm"]
        ?: normalized["bookusfm"]
        ?: normalized["book_usfm"]
    val rawBookNumber = (
        normalized["booknumber"]
            ?: normalized["votd_book_number"]
            ?: normalized["book_number"]
        )?.toIntOrNull()?.takeIf { it in 1..66 }
    val resolvedUsfm = rawUsfm?.takeIf { it.isNotBlank() }?.uppercase()
        ?: usfmForBookNumber(rawBookNumber)
        ?: ""
    val chapter = (
        normalized["votd_chapter"]
            ?: normalized["chapter"]
            ?: normalized["chapter_number"]
        )?.toIntOrNull()?.takeIf { it >= 1 }
    val verse = (
        normalized["votd_verse"]
            ?: normalized["verse"]
            ?: normalized["verse_number"]
        )?.toIntOrNull()?.takeIf { it >= 1 } ?: 1
    val version = (
        normalized["votd_version"]
            ?: normalized["versionid"]
            ?: normalized["version"]
        )?.takeIf { it.isNotBlank() }?.uppercase()

    if ((resolvedUsfm.isNotBlank() || rawBookNumber != null) && chapter != null) {
        return ScriptureDeepLink(
            bookUsfm = resolvedUsfm,
            bookNumber = rawBookNumber ?: bookNumberForUsfm(resolvedUsfm),
            chapter = chapter,
            verse = verse,
            version = version,
            fetchDailyIfMissing = false,
        )
    }

    if (type.equals("votd", ignoreCase = true)) {
        return ScriptureDeepLink(
            bookUsfm = "",
            bookNumber = null,
            chapter = 1,
            verse = 1,
            version = version,
            fetchDailyIfMissing = true,
        )
    }

    return null
}

internal fun findVerseCharacterOffset(
    chapterText: String,
    verseNumber: Int,
    version: String,
): Int? {
    if (verseNumber <= 1 || chapterText.isBlank()) return null

    if (version.equals("svd", ignoreCase = true)) {
        val pattern = Regex("(?:^|\\n)$verseNumber\\s")
        val match = pattern.find(chapterText) ?: return null
        val start = match.range.first
        return if (chapterText[start] == '\n') start + 1 else start
    }

    val bracketSpaced = "[$verseNumber] "
    val spacedIdx = chapterText.indexOf(bracketSpaced)
    if (spacedIdx >= 0) return spacedIdx

    val bracketBare = "[$verseNumber]"
    val bareIdx = chapterText.indexOf(bracketBare)
    return if (bareIdx >= 0) bareIdx else null
}

internal fun parseVerseRange(
    scriptureReference: String,
    fallbackVerse: Int = 1,
): IntRange {
    val safeFallback = fallbackVerse.coerceAtLeast(1)
    val match = Regex(":(\\d+)(?:\\s*[-–—]\\s*(\\d+))?").find(scriptureReference)
        ?: return safeFallback..safeFallback
    val start = match.groupValues[1].toIntOrNull()?.coerceAtLeast(1) ?: safeFallback
    val end = match.groupValues[2].toIntOrNull()?.coerceAtLeast(start) ?: start
    return start..end
}

internal fun findVerseCharacterRange(
    chapterText: String,
    verseStart: Int,
    verseEnd: Int = verseStart,
    version: String,
): IntRange? {
    if (chapterText.isBlank()) return null
    val safeStart = verseStart.coerceAtLeast(1)
    val safeEnd = verseEnd.coerceAtLeast(safeStart)

    val startIdx = if (safeStart == 1) {
        if (version.equals("svd", ignoreCase = true)) {
            val match = Regex("(?:^|\\n)1\\s").find(chapterText)
            if (match != null) {
                val first = match.range.first
                if (chapterText[first] == '\n') first + 1 else first
            } else {
                0
            }
        } else {
            val spaced = chapterText.indexOf("[1] ")
            if (spaced >= 0) spaced else {
                val bare = chapterText.indexOf("[1]")
                if (bare >= 0) bare else 0
            }
        }
    } else {
        findVerseCharacterOffset(chapterText, safeStart, version)
    } ?: return null

    val exactNext = findVerseCharacterOffset(chapterText, safeEnd + 1, version)
    val fallbackNext = if (exactNext == null) {
        val endVerseAnchor = if (safeEnd > safeStart) {
            findVerseCharacterOffset(chapterText, safeEnd, version) ?: startIdx
        } else {
            startIdx
        }
        if (endVerseAnchor + 1 < chapterText.length) {
            val tailPattern = if (version.equals("svd", ignoreCase = true)) {
                Regex("\\n\\d+\\s")
            } else {
                Regex("\\n\\[\\d+\\]")
            }
            tailPattern.find(chapterText, startIndex = endVerseAnchor + 1)?.range?.first?.plus(1)
        } else {
            null
        }
    } else {
        null
    }
    var endExclusive = exactNext ?: fallbackNext ?: chapterText.length
    while (endExclusive > startIdx && chapterText[endExclusive - 1].isWhitespace()) {
        endExclusive--
    }
    return if (endExclusive > startIdx) startIdx until endExclusive else null
}
