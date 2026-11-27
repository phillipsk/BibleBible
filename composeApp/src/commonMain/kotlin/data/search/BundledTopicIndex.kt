package data.search

import data.bible.BookData
import data.bible.bibleBooksFull
import data.notifications.usfmForBookNumber

internal data class BundledTopicEntry(
    val topic: String,
    val title: String,
    val tag: String,
    val bookId: String,
    val bookNumber: Int,
    val bookName: String,
    val chapter: Int,
    val verseStart: Int,
    val verseEnd: Int,
    val excerpt: String,
    val accentHex: String,
    val keywords: List<String>,
    val relatedTopics: List<String>,
    val planId: String? = null,
)

internal object BundledTopicIndex {
    private val chapterMaxByUsfm = mapOf(
        "GEN" to 50, "EXO" to 40, "LEV" to 27, "NUM" to 36, "DEU" to 34, "JOS" to 24, "JDG" to 21, "RUT" to 4,
        "1SA" to 31, "2SA" to 24, "1KI" to 22, "2KI" to 25, "1CH" to 29, "2CH" to 36, "EZR" to 10, "NEH" to 13,
        "EST" to 10, "JOB" to 42, "PSA" to 150, "PRO" to 31, "ECC" to 12, "SNG" to 8, "ISA" to 66, "JER" to 52,
        "LAM" to 5, "EZK" to 48, "DAN" to 12, "HOS" to 14, "JOL" to 3, "AMO" to 9, "OBA" to 1, "JON" to 4,
        "MIC" to 7, "NAM" to 3, "HAB" to 3, "ZEP" to 3, "HAG" to 2, "ZEC" to 14, "MAL" to 4,
        "MAT" to 28, "MRK" to 16, "LUK" to 24, "JHN" to 21, "ACT" to 28, "ROM" to 16, "1CO" to 16, "2CO" to 13,
        "GAL" to 6, "EPH" to 6, "PHP" to 4, "COL" to 4, "1TH" to 5, "2TH" to 3, "1TI" to 6, "2TI" to 4,
        "TIT" to 3, "PHM" to 1, "HEB" to 13, "JAS" to 5, "1PE" to 5, "2PE" to 3, "1JN" to 5, "2JN" to 1,
        "3JN" to 1, "JUD" to 1, "REV" to 22,
    )

    private val bookAliases: Map<String, Pair<Int, String>> = buildMap {
        bibleBooksFull.forEachIndexed { idx, name ->
            val number = idx + 1
            val usfm = usfmForBookNumber(number) ?: return@forEachIndexed
            put(name.lowercase(), number to usfm)
            put(name.lowercase().replace(" ", ""), number to usfm)
            put(usfm.lowercase(), number to usfm)
        }
        val extras = mapOf(
            "gen" to (1 to "GEN"), "exo" to (2 to "EXO"), "ex" to (2 to "EXO"), "lev" to (3 to "LEV"),
            "num" to (4 to "NUM"), "deut" to (5 to "DEU"), "josh" to (6 to "JOS"), "judg" to (7 to "JDG"),
            "1sam" to (9 to "1SA"), "2sam" to (10 to "2SA"), "1kgs" to (11 to "1KI"), "2kgs" to (12 to "2KI"),
            "1chr" to (13 to "1CH"), "2chr" to (14 to "2CH"), "neh" to (16 to "NEH"),
            "ps" to (19 to "PSA"), "psa" to (19 to "PSA"), "psalm" to (19 to "PSA"),
            "prov" to (20 to "PRO"), "eccl" to (21 to "ECC"), "isa" to (23 to "ISA"), "jer" to (24 to "JER"),
            "ezek" to (26 to "EZK"), "dan" to (27 to "DAN"), "matt" to (40 to "MAT"), "mt" to (40 to "MAT"),
            "mk" to (41 to "MRK"), "lk" to (42 to "LUK"), "jn" to (43 to "JHN"), "rom" to (45 to "ROM"),
            "1cor" to (46 to "1CO"), "2cor" to (47 to "2CO"), "gal" to (48 to "GAL"), "eph" to (49 to "EPH"),
            "phil" to (50 to "PHP"), "col" to (51 to "COL"), "1thess" to (52 to "1TH"), "2thess" to (53 to "2TH"),
            "1tim" to (54 to "1TI"), "2tim" to (55 to "2TI"), "phlm" to (57 to "PHM"), "heb" to (58 to "HEB"),
            "jas" to (59 to "JAS"), "1pet" to (60 to "1PE"), "2pet" to (61 to "2PE"),
            "1jn" to (62 to "1JN"), "2jn" to (63 to "2JN"), "3jn" to (64 to "3JN"), "rev" to (66 to "REV"),
        )
        putAll(extras)
    }

    val curatedTopics: List<BundledTopicEntry> = listOf(
        BundledTopicEntry(
            topic = "Peace",
            title = "Peace That Surpasses Understanding",
            tag = "Peace",
            bookId = "PHP",
            bookNumber = 50,
            bookName = "Philippians",
            chapter = 4,
            verseStart = 6,
            verseEnd = 7,
            excerpt = "Be careful for nothing; but in every thing by prayer and supplication with thanksgiving let your requests be made known unto God.",
            accentHex = "#2E6F73",
            keywords = listOf("peace", "calm", "still", "quiet", "troubled", "overwhelmed", "serene"),
            relatedTopics = listOf("Rest", "Trust", "Anxiety"),
            planId = "peace_in_every_season",
        ),
        BundledTopicEntry(
            topic = "Anxiety",
            title = "Casting All Your Care Upon Him",
            tag = "Anxiety & Worry",
            bookId = "1PE",
            bookNumber = 60,
            bookName = "1 Peter",
            chapter = 5,
            verseStart = 7,
            verseEnd = 7,
            excerpt = "Casting all your care upon him; for he careth for you.",
            accentHex = "#5E478A",
            keywords = listOf("anxiety", "anxious", "worry", "worried", "stress", "stressed", "panic", "overwhelmed"),
            relatedTopics = listOf("Peace", "Fear", "Trust"),
            planId = "peace_in_every_season",
        ),
        BundledTopicEntry(
            topic = "Rest",
            title = "Rest for the Heavy Laden",
            tag = "Rest",
            bookId = "MAT",
            bookNumber = 40,
            bookName = "Matthew",
            chapter = 11,
            verseStart = 28,
            verseEnd = 30,
            excerpt = "Come unto me, all ye that labour and are heavy laden, and I will give you rest.",
            accentHex = "#24426B",
            keywords = listOf("rest", "tired", "exhausted", "burnout", "weary", "burden", "overwhelmed", "work"),
            relatedTopics = listOf("Peace", "Strength", "Comfort"),
            planId = "peace_in_every_season",
        ),
        BundledTopicEntry(
            topic = "Strength",
            title = "Renew Your Strength Like Eagles",
            tag = "Strength",
            bookId = "ISA",
            bookNumber = 23,
            bookName = "Isaiah",
            chapter = 40,
            verseStart = 31,
            verseEnd = 31,
            excerpt = "But they that wait upon the LORD shall renew their strength; they shall mount up with wings as eagles.",
            accentHex = "#B37044",
            keywords = listOf("strength", "strong", "weak", "weakness", "endure", "power", "exhausted"),
            relatedTopics = listOf("Courage", "Hope", "Trust"),
            planId = "psalms_of_comfort",
        ),
        BundledTopicEntry(
            topic = "Grace",
            title = "Saved by Grace Through Faith",
            tag = "Grace",
            bookId = "EPH",
            bookNumber = 49,
            bookName = "Ephesians",
            chapter = 2,
            verseStart = 8,
            verseEnd = 9,
            excerpt = "For by grace are ye saved through faith; and that not of yourselves: it is the gift of God.",
            accentHex = "#3A6F8C",
            keywords = listOf("grace", "unmerited", "mercy", "gift", "saved", "favor"),
            relatedTopics = listOf("Mercy", "Faith", "Love"),
            planId = "foundations_of_faith",
        ),
        BundledTopicEntry(
            topic = "Forgiveness",
            title = "Forgive as Christ Forgave",
            tag = "Forgiveness",
            bookId = "EPH",
            bookNumber = 49,
            bookName = "Ephesians",
            chapter = 4,
            verseStart = 32,
            verseEnd = 32,
            excerpt = "And be ye kind one to another, tenderhearted, forgiving one another, even as God for Christ's sake hath forgiven you.",
            accentHex = "#458C6E",
            keywords = listOf("forgive", "forgiveness", "pardon", "resentment", "bitterness", "reconcile"),
            relatedTopics = listOf("Mercy", "Grace", "Love"),
            planId = "gospels_in_30_days",
        ),
        BundledTopicEntry(
            topic = "Wisdom",
            title = "Ask God for Wisdom",
            tag = "Wisdom",
            bookId = "JAS",
            bookNumber = 59,
            bookName = "James",
            chapter = 1,
            verseStart = 5,
            verseEnd = 5,
            excerpt = "If any of you lack wisdom, let him ask of God, that giveth to all men liberally, and upbraideth not.",
            accentHex = "#B59442",
            keywords = listOf("wisdom", "wise", "decision", "discernment", "confused", "clarity", "understanding"),
            relatedTopics = listOf("Guidance", "Prayer", "Trust"),
            planId = "proverbs_in_31_days",
        ),
        BundledTopicEntry(
            topic = "Guidance",
            title = "He Shall Direct Thy Paths",
            tag = "Guidance",
            bookId = "PRO",
            bookNumber = 20,
            bookName = "Proverbs",
            chapter = 3,
            verseStart = 5,
            verseEnd = 6,
            excerpt = "Trust in the LORD with all thine heart; and lean not unto thine own understanding.",
            accentHex = "#3E8E89",
            keywords = listOf("guidance", "direction", "path", "future", "decision", "lost", "lead"),
            relatedTopics = listOf("Wisdom", "Trust", "Purpose"),
            planId = "proverbs_in_31_days",
        ),
        BundledTopicEntry(
            topic = "Faith",
            title = "The Substance of Things Hoped For",
            tag = "Faith",
            bookId = "HEB",
            bookNumber = 58,
            bookName = "Hebrews",
            chapter = 11,
            verseStart = 1,
            verseEnd = 1,
            excerpt = "Now faith is the substance of things hoped for, the evidence of things not seen.",
            accentHex = "#4A6BB0",
            keywords = listOf("faith", "believe", "belief", "doubt", "trust", "conviction"),
            relatedTopics = listOf("Trust", "Hope", "Grace"),
            planId = "foundations_of_faith",
        ),
        BundledTopicEntry(
            topic = "Love",
            title = "For God So Loved the World",
            tag = "Love",
            bookId = "JHN",
            bookNumber = 43,
            bookName = "John",
            chapter = 3,
            verseStart = 16,
            verseEnd = 16,
            excerpt = "For God so loved the world, that he gave his only begotten Son, that whosoever believeth in him should not perish.",
            accentHex = "#B04F66",
            keywords = listOf("love", "loved", "charity", "compassion", "marriage", "kindness"),
            relatedTopics = listOf("Grace", "Mercy", "Forgiveness"),
            planId = "gospels_in_30_days",
        ),
        BundledTopicEntry(
            topic = "Prayer",
            title = "Draw Near in Prayer",
            tag = "Prayer",
            bookId = "MAT",
            bookNumber = 40,
            bookName = "Matthew",
            chapter = 6,
            verseStart = 9,
            verseEnd = 13,
            excerpt = "After this manner therefore pray ye: Our Father which art in heaven, Hallowed be thy name.",
            accentHex = "#3B76B0",
            keywords = listOf("prayer", "pray", "praying", "intercede", "supplication", "petition"),
            relatedTopics = listOf("Peace", "Faith", "Joy"),
            planId = "gospels_in_30_days",
        ),
        BundledTopicEntry(
            topic = "Hope",
            title = "A Future and a Hope",
            tag = "Hope",
            bookId = "JER",
            bookNumber = 24,
            bookName = "Jeremiah",
            chapter = 29,
            verseStart = 11,
            verseEnd = 11,
            excerpt = "For I know the thoughts that I think toward you, saith the LORD, thoughts of peace, and not of evil.",
            accentHex = "#C46854",
            keywords = listOf("hope", "future", "discouraged", "despair", "promise", "tomorrow"),
            relatedTopics = listOf("Faith", "Purpose", "Joy"),
            planId = "psalms_of_comfort",
        ),
        BundledTopicEntry(
            topic = "Healing",
            title = "He Healeth the Broken in Heart",
            tag = "Healing",
            bookId = "PSA",
            bookNumber = 19,
            bookName = "Psalms",
            chapter = 147,
            verseStart = 3,
            verseEnd = 3,
            excerpt = "He healeth the broken in heart, and bindeth up their wounds.",
            accentHex = "#3E8E89",
            keywords = listOf("healing", "heal", "sick", "sickness", "pain", "hurt", "recovery", "broken"),
            relatedTopics = listOf("Comfort", "Grief", "Hope"),
            planId = "psalms_of_comfort",
        ),
        BundledTopicEntry(
            topic = "Joy",
            title = "The Joy of the Lord Is Your Strength",
            tag = "Joy",
            bookId = "NEH",
            bookNumber = 16,
            bookName = "Nehemiah",
            chapter = 8,
            verseStart = 10,
            verseEnd = 10,
            excerpt = "Neither be ye sorry; for the joy of the LORD is your strength.",
            accentHex = "#B59442",
            keywords = listOf("joy", "joyful", "glad", "rejoice", "happiness", "happy"),
            relatedTopics = listOf("Gratitude", "Peace", "Strength"),
            planId = "peace_in_every_season",
        ),
        BundledTopicEntry(
            topic = "Courage",
            title = "Be Strong and of a Good Courage",
            tag = "Courage",
            bookId = "JOS",
            bookNumber = 6,
            bookName = "Joshua",
            chapter = 1,
            verseStart = 9,
            verseEnd = 9,
            excerpt = "Be strong and of a good courage; be not afraid, neither be thou dismayed: for the LORD thy God is with thee.",
            accentHex = "#B37044",
            keywords = listOf("courage", "brave", "bold", "afraid", "fear", "dismayed"),
            relatedTopics = listOf("Fear", "Strength", "Trust"),
            planId = "foundations_of_faith",
        ),
        BundledTopicEntry(
            topic = "Fear",
            title = "Fear Not, For I Am With Thee",
            tag = "Fear",
            bookId = "ISA",
            bookNumber = 23,
            bookName = "Isaiah",
            chapter = 41,
            verseStart = 10,
            verseEnd = 10,
            excerpt = "Fear thou not; for I am with thee: be not dismayed; for I am thy God: I will strengthen thee.",
            accentHex = "#724B99",
            keywords = listOf("fear", "afraid", "scared", "terror", "dread", "frightened"),
            relatedTopics = listOf("Courage", "Peace", "Trust"),
            planId = "psalms_of_comfort",
        ),
        BundledTopicEntry(
            topic = "Gratitude",
            title = "In Every Thing Give Thanks",
            tag = "Gratitude",
            bookId = "1TH",
            bookNumber = 52,
            bookName = "1 Thessalonians",
            chapter = 5,
            verseStart = 16,
            verseEnd = 18,
            excerpt = "Rejoice evermore. Pray without ceasing. In every thing give thanks.",
            accentHex = "#B59442",
            keywords = listOf("gratitude", "thankful", "thanks", "thanksgiving", "grateful"),
            relatedTopics = listOf("Joy", "Peace", "Prayer"),
            planId = "peace_in_every_season",
        ),
        BundledTopicEntry(
            topic = "Trust",
            title = "The Lord Is My Shepherd",
            tag = "Trust",
            bookId = "PSA",
            bookNumber = 19,
            bookName = "Psalms",
            chapter = 23,
            verseStart = 1,
            verseEnd = 4,
            excerpt = "The LORD is my shepherd; I shall not want. He maketh me to lie down in green pastures.",
            accentHex = "#3E8E89",
            keywords = listOf("trust", "shepherd", "rely", "depend", "surrender", "confidence"),
            relatedTopics = listOf("Peace", "Faith", "Guidance"),
            planId = "psalms_of_comfort",
        ),
        BundledTopicEntry(
            topic = "Grief",
            title = "Near to the Brokenhearted",
            tag = "Grief & Loss",
            bookId = "PSA",
            bookNumber = 19,
            bookName = "Psalms",
            chapter = 34,
            verseStart = 18,
            verseEnd = 18,
            excerpt = "The LORD is nigh unto them that are of a broken heart; and saveth such as be of a contrite spirit.",
            accentHex = "#4A6BB0",
            keywords = listOf("grief", "grieve", "mourn", "loss", "sad", "sorrow", "tears"),
            relatedTopics = listOf("Comfort", "Healing", "Hope"),
            planId = "psalms_of_comfort",
        ),
        BundledTopicEntry(
            topic = "Comfort",
            title = "The God of All Comfort",
            tag = "Comfort",
            bookId = "2CO",
            bookNumber = 47,
            bookName = "2 Corinthians",
            chapter = 1,
            verseStart = 3,
            verseEnd = 4,
            excerpt = "Blessed be God, even the Father of our Lord Jesus Christ, the Father of mercies, and the God of all comfort.",
            accentHex = "#3B76B0",
            keywords = listOf("comfort", "console", "hurting", "lonely", "sorrow"),
            relatedTopics = listOf("Grief", "Peace", "Healing"),
            planId = "psalms_of_comfort",
        ),
        BundledTopicEntry(
            topic = "Work",
            title = "Work Heartily as Unto the Lord",
            tag = "Work & Calling",
            bookId = "COL",
            bookNumber = 51,
            bookName = "Colossians",
            chapter = 3,
            verseStart = 23,
            verseEnd = 23,
            excerpt = "And whatsoever ye do, do it heartily, as to the Lord, and not unto men.",
            accentHex = "#3B76B0",
            keywords = listOf("work", "job", "career", "office", "labor", "boss", "overwhelmed"),
            relatedTopics = listOf("Rest", "Purpose", "Strength"),
            planId = "proverbs_in_31_days",
        ),
        BundledTopicEntry(
            topic = "Purpose",
            title = "All Things Work Together for Good",
            tag = "Purpose",
            bookId = "ROM",
            bookNumber = 45,
            bookName = "Romans",
            chapter = 8,
            verseStart = 28,
            verseEnd = 28,
            excerpt = "And we know that all things work together for good to them that love God.",
            accentHex = "#4A6BB0",
            keywords = listOf("purpose", "calling", "meaning", "plan", "destiny"),
            relatedTopics = listOf("Guidance", "Hope", "Work"),
            planId = "foundations_of_faith",
        ),
        BundledTopicEntry(
            topic = "Mercy",
            title = "New Every Morning",
            tag = "Mercy",
            bookId = "LAM",
            bookNumber = 25,
            bookName = "Lamentations",
            chapter = 3,
            verseStart = 22,
            verseEnd = 23,
            excerpt = "It is of the LORD's mercies that we are not consumed, because his compassions fail not. They are new every morning.",
            accentHex = "#B37044",
            keywords = listOf("mercy", "merciful", "compassion", "fresh", "renewal"),
            relatedTopics = listOf("Grace", "Forgiveness", "Love"),
            planId = "psalms_of_comfort",
        ),
    )

    val topicNames: List<String> = curatedTopics.map { it.topic }
    private val topicByLower = curatedTopics.associateBy { it.topic.lowercase() }
    private val referenceRegex = Regex("^([1-3]?\\s*[A-Za-z]+)\\s+(\\d{1,3})(?::(\\d{1,3})(?:-(\\d{1,3}))?)?$")

    fun parseDirectReference(rawQuery: String): SearchDirectReferenceDto? {
        val clean = rawQuery.trim().replace(Regex("\\s+"), " ")
        val match = referenceRegex.matchEntire(clean) ?: return null
        val bookToken = match.groupValues[1].lowercase().replace(" ", "")
        val (bookNumber, usfm) = bookAliases[bookToken] ?: return null
        val chapter = match.groupValues[2].toIntOrNull() ?: return null
        val maxChapters = chapterMaxByUsfm[usfm] ?: 150
        if (chapter !in 1..maxChapters) return null
        val verseStart = match.groupValues[3].toIntOrNull()?.coerceAtLeast(1) ?: 1
        val verseEnd = match.groupValues[4].toIntOrNull()?.coerceAtLeast(verseStart) ?: verseStart
        val bookName = bibleBooksFull.getOrNull(bookNumber - 1) ?: usfm
        val refLabel = when {
            match.groupValues[3].isEmpty() -> "$bookName $chapter"
            match.groupValues[4].isNotEmpty() -> "$bookName $chapter:$verseStart-$verseEnd"
            else -> "$bookName $chapter:$verseStart"
        }
        return SearchDirectReferenceDto(
            bookId = usfm,
            bookNumber = bookNumber,
            bookName = bookName,
            chapter = chapter,
            verseStart = verseStart,
            verseEnd = verseEnd,
            reference = refLabel,
        )
    }

    fun matchBooks(query: String, availableBooks: List<BookData>): List<SearchBookHitDto> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return emptyList()
        val directBookUsfm = parseDirectReference(q)?.bookId
        val strippedPrefix = q.replace(Regex("\\s+\\d+.*$"), "").trim()
        val searchTerm = strippedPrefix.ifBlank { q }
        val compact = searchTerm.replace(" ", "")
        if (availableBooks.isNotEmpty()) {
            return availableBooks.mapIndexedNotNull { idx, book ->
                val name = (book.name ?: book.cleanedName ?: book.bookId).trim()
                val nameLower = name.lowercase()
                val matchesRef = directBookUsfm != null && book.bookId.equals(directBookUsfm, ignoreCase = true)
                if (matchesRef || nameLower.contains(searchTerm) || nameLower.replace(" ", "").contains(compact) ||
                    book.bookId.equals(compact, ignoreCase = true)
                ) {
                    SearchBookHitDto(
                        bookId = book.bookId,
                        bookNumber = idx + 1,
                        bookName = name,
                        chapters = chapterMaxByUsfm[book.bookId.uppercase()] ?: 1,
                    )
                } else {
                    null
                }
            }.take(12)
        }
        return bibleBooksFull.mapIndexedNotNull { idx, name ->
            val number = idx + 1
            val usfm = usfmForBookNumber(number) ?: return@mapIndexedNotNull null
            val matchesRef = directBookUsfm != null && usfm.equals(directBookUsfm, ignoreCase = true)
            if (matchesRef || name.lowercase().contains(searchTerm) || name.lowercase().replace(" ", "").contains(compact) ||
                usfm.equals(compact, ignoreCase = true)
            ) {
                SearchBookHitDto(
                    bookId = usfm,
                    bookNumber = number,
                    bookName = name,
                    chapters = chapterMaxByUsfm[usfm] ?: 1,
                )
            } else {
                null
            }
        }.take(12)
    }

    fun allBookHits(availableBooks: List<BookData>): List<SearchBookHitDto> {
        if (availableBooks.isNotEmpty()) {
            return availableBooks.mapIndexed { idx, book ->
                val name = (book.name ?: book.cleanedName ?: book.bookId).trim()
                SearchBookHitDto(
                    bookId = book.bookId,
                    bookNumber = idx + 1,
                    bookName = name,
                    chapters = chapterMaxByUsfm[book.bookId.uppercase()] ?: 1,
                )
            }
        }
        return bibleBooksFull.mapIndexedNotNull { idx, name ->
            val number = idx + 1
            val usfm = usfmForBookNumber(number) ?: return@mapIndexedNotNull null
            SearchBookHitDto(
                bookId = usfm,
                bookNumber = number,
                bookName = name,
                chapters = chapterMaxByUsfm[usfm] ?: 1,
            )
        }
    }

    fun lookupBundledExcerpt(bookId: String, chapter: Int, verseStart: Int): String? {
        val usfm = bookId.trim().uppercase()
        return curatedTopics.firstOrNull { entry ->
            entry.bookId.equals(usfm, ignoreCase = true) &&
                entry.chapter == chapter &&
                verseStart in entry.verseStart..entry.verseEnd
        }?.excerpt ?: curatedTopics.firstOrNull { entry ->
            entry.bookId.equals(usfm, ignoreCase = true) && entry.chapter == chapter
        }?.excerpt
    }

    private fun toMasonryCard(entry: BundledTopicEntry, size: String): SearchMasonryCardDto {
        val ref = if (entry.verseEnd > entry.verseStart) {
            "${entry.bookName} ${entry.chapter}:${entry.verseStart}-${entry.verseEnd}"
        } else {
            "${entry.bookName} ${entry.chapter}:${entry.verseStart}"
        }
        return SearchMasonryCardDto(
            id = "topic:${entry.topic.lowercase()}",
            size = size,
            kind = "topic",
            tag = entry.tag,
            title = entry.title,
            reference = ref,
            excerpt = entry.excerpt,
            bookId = entry.bookId,
            bookNumber = entry.bookNumber,
            chapter = entry.chapter,
            verseStart = entry.verseStart,
            verseEnd = entry.verseEnd,
            accentHex = entry.accentHex,
            planId = entry.planId,
        )
    }

    fun featuredStarterCards(limit: Int = 5): List<SearchMasonryCardDto> {
        val preferredTopics = listOf("Peace", "Love", "Hope", "Wisdom", "Grace")
        val selected = preferredTopics.mapNotNull { topicByLower[it.lowercase()] }
            .ifEmpty { curatedTopics.take(limit) }
            .take(limit)
        return selected.mapIndexed { idx, entry ->
            val size = if (idx == 4 && !entry.planId.isNullOrBlank()) {
                "wide"
            } else if (idx % 2 == 0) {
                "tall"
            } else {
                "compact"
            }
            toMasonryCard(entry, size)
        }
    }

    fun relatedCardsForBook(bookId: String, limit: Int = 4): List<SearchMasonryCardDto> {
        val cleanBook = bookId.trim().uppercase()
        val matching = curatedTopics.filter { it.bookId.equals(cleanBook, ignoreCase = true) }
        val pool = (matching + curatedTopics.filterNot { it in matching }).distinctBy { it.topic }.take(limit)
        return pool.mapIndexed { idx, entry ->
            val size = if (idx % 2 == 0) "tall" else "compact"
            toMasonryCard(entry, size)
        }
    }

    fun searchLocalTopics(
        query: String,
        explicitTopics: List<String> = emptyList(),
        includeMasonry: Boolean = true,
    ): Pair<List<String>, List<SearchMasonryCardDto>> {
        if (!includeMasonry) return emptyList<String>() to emptyList()
        val q = query.trim().lowercase()
        val tokens = q.split(Regex("[^a-z0-9]+")).filter { it.length >= 2 }
        val explicitLower = explicitTopics.map { it.trim().lowercase() }.toSet()

        val scored = curatedTopics.mapNotNull { entry ->
            var score = 0
            val topicLower = entry.topic.lowercase()
            if (topicLower in explicitLower) score += 100
            if (topicLower == q) score += 120
            else if (q.length >= 2 && topicLower.contains(q)) score += 60
            for (kw in entry.keywords) {
                if (kw == q) score += 80
                else if (kw.length >= 3 && q.contains(kw)) score += 45
                else if (tokens.any { tok -> kw.startsWith(tok) || tok.startsWith(kw) }) score += 25
            }
            if (score > 0) entry to score else null
        }.sortedByDescending { it.second }.map { it.first }.toMutableList()

        if (scored.isNotEmpty() && scored.size < 4) {
            val seen = scored.map { it.topic }.toHashSet()
            for (relName in scored.first().relatedTopics) {
                val rel = topicByLower[relName.lowercase()]
                if (rel != null && seen.add(rel.topic)) {
                    scored.add(rel)
                }
            }
        }
        if (scored.isNotEmpty() && scored.size < 4) {
            val seen = scored.map { it.topic }.toHashSet()
            for (extra in curatedTopics) {
                if (scored.size >= 4) break
                if (seen.add(extra.topic)) {
                    scored.add(extra)
                }
            }
        }

        val cards = scored.take(5).mapIndexed { idx, entry ->
            val size = when {
                idx == 4 && !entry.planId.isNullOrBlank() -> "wide"
                idx % 2 == 0 -> "tall"
                else -> "compact"
            }
            toMasonryCard(entry, size)
        }
        return scored.take(4).map { it.topic } to cards
    }
}
