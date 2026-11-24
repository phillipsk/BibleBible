package data.shorts

import data.bible.getBiblicalShorts
import data.config.CloudSessionManager
import data.httpClientCloudGateway
import email.kevinphillips.biblebible.security.ProtectedEndpoints
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class BiblicalShortSceneDto(
    @SerialName("index") val index: Int,
    @SerialName("startSeconds") val startSeconds: Int,
    @SerialName("endSeconds") val endSeconds: Int,
    @SerialName("heading") val heading: String,
    @SerialName("caption") val caption: String,
)

@Serializable
internal data class BiblicalShortDto(
    @SerialName("id") val id: String,
    @SerialName("order") val order: Int,
    @SerialName("title") val title: String,
    @SerialName("category") val category: String,
    @SerialName("durationSeconds") val durationSeconds: Int,
    @SerialName("scriptureReference") val scriptureReference: String,
    @SerialName("bookId") val bookId: Int,
    @SerialName("bookUsfm") val bookUsfm: String,
    @SerialName("chapter") val chapter: Int,
    @SerialName("verse") val verse: Int,
    @SerialName("keyVerseText") val keyVerseText: String,
    @SerialName("narrative") val narrative: String,
    @SerialName("lesson") val lesson: String,
    @SerialName("accentColorHex") val accentColorHex: String = "#3A5F8F",
    @SerialName("videoUrl") val videoUrl: String = "",
    @SerialName("bodyDurationSeconds") val bodyDurationSeconds: Double? = null,
    @SerialName("promoEndCardSeconds") val promoEndCardSeconds: Double = 0.0,
    @SerialName("scene2StartSeconds") val scene2StartSeconds: Int? = null,
    @SerialName("scenes") val scenes: List<BiblicalShortSceneDto> = emptyList(),
) {
    /**
     * Returns the playback timestamp (in seconds) right before a social promotional end card,
     * allowing the app to reuse the exact same MP4 file while looping cleanly inside the app.
     */
    fun inAppCutoffSeconds(): Double? {
        val explicit = bodyDurationSeconds
        if (explicit != null && explicit > 0.0) {
            return explicit
        }
        if (promoEndCardSeconds > 0.0 && durationSeconds.toDouble() > promoEndCardSeconds + 2.0) {
            return (durationSeconds.toDouble() - promoEndCardSeconds).coerceAtLeast(2.0)
        }
        return null
    }

    fun effectiveInAppDurationSeconds(): Int =
        inAppCutoffSeconds()?.toInt()?.coerceAtLeast(1) ?: durationSeconds.coerceAtLeast(1)

    fun effectiveScene2StartSeconds(): Int {
        val total = effectiveInAppDurationSeconds()
        val raw = scene2StartSeconds ?: maxOf(5, (total * 0.36).toInt())
        return raw.coerceIn(1, (total - 1).coerceAtLeast(1))
    }
}

@Serializable
internal data class BiblicalShortsFeedDto(
    @SerialName("schemaVersion") val schemaVersion: Int = 1,
    @SerialName("totalCount") val totalCount: Int = 10,
    @SerialName("maxDurationSeconds") val maxDurationSeconds: Int = 30,
    @SerialName("categories") val categories: List<String> = emptyList(),
    @SerialName("shorts") val shorts: List<BiblicalShortDto> = emptyList(),
)

internal val biblicalShortsCategories = listOf(
    "All",
    "Faith & Courage",
    "Wisdom & Peace",
    "Hope & Renewal",
    "Grace & Truth",
)

private fun shortItem(
    order: Int,
    id: String,
    title: String,
    category: String,
    durationSeconds: Int,
    scriptureReference: String,
    bookId: Int,
    bookUsfm: String,
    chapter: Int,
    verse: Int,
    keyVerseText: String,
    narrative: String,
    lesson: String,
    accentColorHex: String,
    gatewayBaseUrl: String,
    bodyDurationSeconds: Double? = null,
    promoEndCardSeconds: Double = 0.0,
    scene2StartSeconds: Int? = null,
    directVideoUrl: String = "",
): BiblicalShortDto {
    val inAppDur = (bodyDurationSeconds?.toInt() ?: durationSeconds).coerceAtLeast(1)
    val split1 = (scene2StartSeconds ?: (inAppDur / 3)).coerceIn(1, (inAppDur - 1).coerceAtLeast(1))
    val split2 = maxOf(split1 + 2, (inAppDur * 2) / 3).coerceAtMost(inAppDur)
    val cleanBase = gatewayBaseUrl.trim().trimEnd('/')
    val resolvedVideoUrl = directVideoUrl.trim().ifEmpty {
        if (cleanBase.isNotEmpty() && !cleanBase.contains("unconfigured.invalid")) {
            val prefix = if ("://" in cleanBase) cleanBase else "https://$cleanBase"
            "$prefix${ProtectedEndpoints.shortsMediaPrefix()}$id.mp4"
        } else {
            ""
        }
    }
    return BiblicalShortDto(
        id = id,
        order = order,
        title = title,
        category = category,
        durationSeconds = durationSeconds,
        scriptureReference = scriptureReference,
        bookId = bookId,
        bookUsfm = bookUsfm,
        chapter = chapter,
        verse = verse,
        keyVerseText = keyVerseText,
        narrative = narrative,
        lesson = lesson,
        accentColorHex = accentColorHex,
        videoUrl = resolvedVideoUrl,
        bodyDurationSeconds = bodyDurationSeconds,
        promoEndCardSeconds = promoEndCardSeconds,
        scene2StartSeconds = scene2StartSeconds,
        scenes = listOf(
            BiblicalShortSceneDto(1, 0, split1, "The Principle", narrative),
            BiblicalShortSceneDto(2, split1, split2, "The Scripture", "$scriptureReference: \"$keyVerseText\""),
            BiblicalShortSceneDto(3, split2, inAppDur, "Walk It Out", lesson),
        ),
    )
}

internal fun defaultBiblicalShortsCatalog(
    gatewayBaseUrl: String = CloudSessionManager.configuredGatewayUrl,
): List<BiblicalShortDto> = listOf(
    shortItem(
        order = 1,
        id = "01-col3-bond-that-holds",
        title = "The Bond That Holds",
        category = "Grace & Truth",
        durationSeconds = 20,
        scriptureReference = "Colossians 3:14",
        bookId = 51,
        bookUsfm = "COL",
        chapter = 3,
        verse = 14,
        keyVerseText = "And above all these things put on charity, which is the bond of perfectness.",
        narrative = "Knowledge alone unravels. Steadfast love binds every virtue into one piece.",
        lesson = "Let patient charity lead every home and church conversation today.",
        accentColorHex = "#8C4A27",
        gatewayBaseUrl = gatewayBaseUrl,
        bodyDurationSeconds = 17.69,
        promoEndCardSeconds = 2.5,
        scene2StartSeconds = 8,
        directVideoUrl = "https://storage.googleapis.com/gen-lang-client-0150678391-biblebible-shorts/review-final-v1/media/01-col3-bond-that-holds.mp4?v=1795471116844",
    ),
    shortItem(
        order = 2,
        id = "02-2ti1-retain-testimony",
        title = "Retain the Testimony",
        category = "Wisdom & Peace",
        durationSeconds = 23,
        scriptureReference = "2 Timothy 1:7",
        bookId = 55,
        bookUsfm = "2TI",
        chapter = 1,
        verse = 7,
        keyVerseText = "For God hath not given us the spirit of fear; but of power, and of love, and of a sound mind.",
        narrative = "A sound mind holds onto peace under pressure without leaking.",
        lesson = "Guard your quiet habits so fear never drains your testimony.",
        accentColorHex = "#2C5282",
        gatewayBaseUrl = gatewayBaseUrl,
        bodyDurationSeconds = 20.73,
        promoEndCardSeconds = 2.5,
        scene2StartSeconds = 9,
        directVideoUrl = "https://storage.googleapis.com/gen-lang-client-0150678391-biblebible-shorts/review-final-v1/media/02-2ti1-retain-testimony.mp4?v=1795471116844",
    ),
    shortItem(
        order = 3,
        id = "03-mat6-honest-prayer",
        title = "Honest Prayer in Secret",
        category = "Wisdom & Peace",
        durationSeconds = 20,
        scriptureReference = "Matthew 6:6",
        bookId = 40,
        bookUsfm = "MAT",
        chapter = 6,
        verse = 6,
        keyVerseText = "But thou, when thou prayest, enter into thy closet, and when thou hast shut thy door, pray to thy Father which is in secret...",
        narrative = "Prayer isn't a speech to impress anyone. It starts in secret with your Father.",
        lesson = "Praise, Repent, Ask, and Yield. Five quiet minutes changes your day.",
        accentColorHex = "#5B4B8A",
        gatewayBaseUrl = gatewayBaseUrl,
        bodyDurationSeconds = 17.41,
        promoEndCardSeconds = 2.5,
        scene2StartSeconds = 8,
        directVideoUrl = "https://storage.googleapis.com/gen-lang-client-0150678391-biblebible-shorts/review-final-v1/media/03-mat6-honest-prayer.mp4?v=1795471116844",
    ),
    shortItem(
        order = 4,
        id = "04-eph4-truth-in-love",
        title = "Truth Anchored in Love",
        category = "Grace & Truth",
        durationSeconds = 26,
        scriptureReference = "Ephesians 4:15",
        bookId = 49,
        bookUsfm = "EPH",
        chapter = 4,
        verse = 15,
        keyVerseText = "But speaking the truth in love, may grow up into him in all things, which is the head, even Christ:",
        narrative = "Truth without love turns harsh; love without truth leaves us drifting.",
        lesson = "Speak honestly without cruelty; love deeply without compromise.",
        accentColorHex = "#2F6B4F",
        gatewayBaseUrl = gatewayBaseUrl,
        bodyDurationSeconds = 23.37,
        promoEndCardSeconds = 2.5,
        scene2StartSeconds = 9,
        directVideoUrl = "https://storage.googleapis.com/gen-lang-client-0150678391-biblebible-shorts/review-final-v1/media/04-eph4-truth-in-love.mp4?v=1795471116844",
    ),
    shortItem(
        order = 5,
        id = "05-rom8-already-not-yet",
        title = "The Already and Not Yet",
        category = "Hope & Renewal",
        durationSeconds = 25,
        scriptureReference = "Romans 8:23",
        bookId = 45,
        bookUsfm = "ROM",
        chapter = 8,
        verse = 23,
        keyVerseText = "Ourselves also, which have the firstfruits of the Spirit, even we ourselves groan within ourselves, waiting for the adoption, to wit, the redemption of our body.",
        narrative = "Why do forgiven believers still groan? Welcome to the Kingdom tension.",
        lesson = "Already indwelt by the Spirit, awaiting full renewal with hope.",
        accentColorHex = "#285E61",
        gatewayBaseUrl = gatewayBaseUrl,
        bodyDurationSeconds = 22.89,
        promoEndCardSeconds = 2.5,
        scene2StartSeconds = 10,
        directVideoUrl = "https://storage.googleapis.com/gen-lang-client-0150678391-biblebible-shorts/review-final-v1/media/05-rom8-already-not-yet.mp4?v=1795471116844",
    ),
    shortItem(
        order = 6,
        id = "06-psa37-covenant-provision",
        title = "God's Covenant Provision",
        category = "Hope & Renewal",
        durationSeconds = 27,
        scriptureReference = "Psalm 37:25",
        bookId = 19,
        bookUsfm = "PSA",
        chapter = 37,
        verse = 25,
        keyVerseText = "I have been young, and now am old; yet have I not seen the righteous forsaken, nor his seed begging bread.",
        narrative = "Before fearing tomorrow's bills, remember yesterday's faithful provision.",
        lesson = "Walk in faithful stewardship. Your Provider never runs short.",
        accentColorHex = "#2F6B4F",
        gatewayBaseUrl = gatewayBaseUrl,
        bodyDurationSeconds = 24.05,
        promoEndCardSeconds = 2.5,
        scene2StartSeconds = 11,
        directVideoUrl = "https://storage.googleapis.com/gen-lang-client-0150678391-biblebible-shorts/review-final-v1/media/06-psa37-covenant-provision.mp4?v=1795471116844",
    ),
    shortItem(
        order = 7,
        id = "07-psa119-lamp-to-feet",
        title = "A Lamp for Today's Step",
        category = "Wisdom & Peace",
        durationSeconds = 23,
        scriptureReference = "Psalm 119:105",
        bookId = 19,
        bookUsfm = "PSA",
        chapter = 119,
        verse = 105,
        keyVerseText = "Thy word is a lamp unto my feet, and a light unto my path.",
        narrative = "Scripture doesn't hand you a five-year map. It lights today's next step.",
        lesson = "Open one quiet chapter before tomorrow's noise starts demanding clarity.",
        accentColorHex = "#744210",
        gatewayBaseUrl = gatewayBaseUrl,
        bodyDurationSeconds = 20.88,
        promoEndCardSeconds = 2.5,
        scene2StartSeconds = 11,
        directVideoUrl = "https://storage.googleapis.com/gen-lang-client-0150678391-biblebible-shorts/review-final-v1/media/07-psa119-lamp-to-feet.mp4?v=1795471116844",
    ),
    shortItem(
        order = 8,
        id = "08-php1-declared-and-shaped",
        title = "Declared vs. Shaped",
        category = "Grace & Truth",
        durationSeconds = 25,
        scriptureReference = "Philippians 1:6",
        bookId = 50,
        bookUsfm = "PHP",
        chapter = 1,
        verse = 6,
        keyVerseText = "Being confident of this very thing, that he which hath begun a good work in you will perform it until the day of Jesus Christ:",
        narrative = "Justification is instant grace; sanctification is lifelong craftsmanship.",
        lesson = "God never walks away from a work He started in you.",
        accentColorHex = "#5B4B8A",
        gatewayBaseUrl = gatewayBaseUrl,
        bodyDurationSeconds = 22.53,
        promoEndCardSeconds = 2.5,
        scene2StartSeconds = 10,
        directVideoUrl = "https://storage.googleapis.com/gen-lang-client-0150678391-biblebible-shorts/review-final-v1/media/08-php1-declared-and-shaped.mp4?v=1795471116844",
    ),
    shortItem(
        order = 9,
        id = "09-rom8-purpose-in-pain",
        title = "Purpose Inside Your Pain",
        category = "Hope & Renewal",
        durationSeconds = 23,
        scriptureReference = "Romans 8:28",
        bookId = 45,
        bookUsfm = "ROM",
        chapter = 8,
        verse = 28,
        keyVerseText = "And we know that all things work together for good to them that love God, to them who are the called according to his purpose.",
        narrative = "Even heavy storms water the roots meant to carry tomorrow's harvest.",
        lesson = "Hold on to hope. Nothing wasted escapes His purpose.",
        accentColorHex = "#285E61",
        gatewayBaseUrl = gatewayBaseUrl,
        bodyDurationSeconds = 20.85,
        promoEndCardSeconds = 2.5,
        scene2StartSeconds = 9,
        directVideoUrl = "https://storage.googleapis.com/gen-lang-client-0150678391-biblebible-shorts/review-final-v1/media/09-rom8-purpose-in-pain.mp4?v=1795471116844",
    ),
    shortItem(
        order = 10,
        id = "10-psa91-abiding-shelter",
        title = "Abiding in the Shelter",
        category = "Hope & Renewal",
        durationSeconds = 23,
        scriptureReference = "Psalm 91:1",
        bookId = 19,
        bookUsfm = "PSA",
        chapter = 91,
        verse = 1,
        keyVerseText = "He that dwelleth in the secret place of the most High shall abide under the shadow of the Almighty.",
        narrative = "Peace belongs to those who dwell in God's Word daily, not only in crisis.",
        lesson = "Stay anchored in truth and refuse to drift from His shelter.",
        accentColorHex = "#8C4A27",
        gatewayBaseUrl = gatewayBaseUrl,
        bodyDurationSeconds = 20.97,
        promoEndCardSeconds = 2.5,
        scene2StartSeconds = 11,
        directVideoUrl = "https://storage.googleapis.com/gen-lang-client-0150678391-biblebible-shorts/review-final-v1/media/10-psa91-abiding-shelter.mp4?v=1795471116844",
    ),
    shortItem(
        order = 11,
        id = "11-php4-peace-guards-heart",
        title = "Peace That Stands Guard",
        category = "Wisdom & Peace",
        durationSeconds = 27,
        scriptureReference = "Philippians 4:6-7",
        bookId = 50,
        bookUsfm = "PHP",
        chapter = 4,
        verse = 6,
        keyVerseText = "Be careful for nothing; but in every thing by prayer and supplication with thanksgiving let your requests be made known unto God. And the peace of God... shall keep your hearts and minds through Christ Jesus.",
        narrative = "Prayer interrupts anxiety by handing every burden to God with thanks.",
        lesson = "Trade endless mental loops for specific grateful prayer today.",
        accentColorHex = "#2A6F97",
        gatewayBaseUrl = gatewayBaseUrl,
        bodyDurationSeconds = 24.37,
        promoEndCardSeconds = 2.5,
        scene2StartSeconds = 11,
        directVideoUrl = "https://storage.googleapis.com/gen-lang-client-0150678391-biblebible-shorts/review-final-v1/media/11-php4-peace-guards-heart.mp4?v=1795471116844",
    ),
    shortItem(
        order = 12,
        id = "12-jer31-written-on-heart",
        title = "Written on the Heart",
        category = "Grace & Truth",
        durationSeconds = 27,
        scriptureReference = "Jeremiah 31:33",
        bookId = 24,
        bookUsfm = "JER",
        chapter = 31,
        verse = 33,
        keyVerseText = "Saith the LORD, I will put my law in their inward parts, and write it in their hearts; and will be their God, and they shall be my people.",
        narrative = "Stone tablets revealed God's standard, yet grace writes it inside the heart.",
        lesson = "New-Covenant obedience flows from loving the God who dwells within.",
        accentColorHex = "#7A5230",
        gatewayBaseUrl = gatewayBaseUrl,
        bodyDurationSeconds = 24.73,
        promoEndCardSeconds = 2.5,
        scene2StartSeconds = 10,
        directVideoUrl = "https://storage.googleapis.com/gen-lang-client-0150678391-biblebible-shorts/review-final-v1/media/12-jer31-written-on-heart.mp4?v=1795471116844",
    ),
    shortItem(
        order = 13,
        id = "13-isa40-strength-before-shift",
        title = "Strength Before the Shift",
        category = "Hope & Renewal",
        durationSeconds = 28,
        scriptureReference = "Isaiah 40:31",
        bookId = 23,
        bookUsfm = "ISA",
        chapter = 40,
        verse = 31,
        keyVerseText = "But they that wait upon the LORD shall renew their strength; they shall mount up with wings as eagles; they shall run, and not be weary; and they shall walk, and not faint.",
        narrative = "Caring for others drains an empty soul. Wait on God before the rush.",
        lesson = "Pause in His Word before pouring yourself out for others.",
        accentColorHex = "#2F667A",
        gatewayBaseUrl = gatewayBaseUrl,
        bodyDurationSeconds = 25.49,
        promoEndCardSeconds = 2.5,
        scene2StartSeconds = 10,
        directVideoUrl = "https://storage.googleapis.com/gen-lang-client-0150678391-biblebible-shorts/review-final-v1/media/13-isa40-strength-before-shift.mp4?v=1795471116844",
    ),
    shortItem(
        order = 14,
        id = "14-mat6-daily-bread",
        title = "Trusting God for Today",
        category = "Faith & Courage",
        durationSeconds = 22,
        scriptureReference = "Matthew 6:11",
        bookId = 40,
        bookUsfm = "MAT",
        chapter = 6,
        verse = 11,
        keyVerseText = "Give us this day our daily bread.",
        narrative = "Anxiety carries next month today. Faith receives fresh grace for right now.",
        lesson = "God supplies enough wisdom, strength, and peace for today.",
        accentColorHex = "#8C4A27",
        gatewayBaseUrl = gatewayBaseUrl,
        bodyDurationSeconds = 19.32,
        promoEndCardSeconds = 2.5,
        scene2StartSeconds = 11,
        directVideoUrl = "https://storage.googleapis.com/gen-lang-client-0150678391-biblebible-shorts/review-final-v1/media/14-mat6-daily-bread.mp4?v=1795471116844",
    ),
    shortItem(
        order = 15,
        id = "15-isa64-potter-hands",
        title = "Shaped by Patient Hands",
        category = "Hope & Renewal",
        durationSeconds = 27,
        scriptureReference = "Isaiah 64:8",
        bookId = 23,
        bookUsfm = "ISA",
        chapter = 64,
        verse = 8,
        keyVerseText = "But now, O LORD, thou art our father; we are the clay, and thou our potter; and we all are the work of thy hand.",
        narrative = "Steady pressure under God's hand is often what gives your life shape.",
        lesson = "Stay yieldable. The Master never wastes pressure on the wheel.",
        accentColorHex = "#7A5230",
        gatewayBaseUrl = gatewayBaseUrl,
        bodyDurationSeconds = 24.41,
        promoEndCardSeconds = 2.5,
        scene2StartSeconds = 9,
        directVideoUrl = "https://storage.googleapis.com/gen-lang-client-0150678391-biblebible-shorts/review-final-v1/media/15-isa64-potter-hands.mp4?v=1795471116844",
    ),
    shortItem(
        order = 16,
        id = "16-act2-ministry-at-table",
        title = "Ministry Starts at Dinner",
        category = "Grace & Truth",
        durationSeconds = 21,
        scriptureReference = "Acts 2:46",
        bookId = 44,
        bookUsfm = "ACT",
        chapter = 2,
        verse = 46,
        keyVerseText = "And they, continuing daily with one accord in the temple, and breaking bread from house to house, did eat their meat with gladness and singleness of heart,",
        narrative = "Real ministry moves from church pews onto everyday dining tables.",
        lesson = "Pull up an extra chair this week and let kindness preach.",
        accentColorHex = "#2F6B4F",
        gatewayBaseUrl = gatewayBaseUrl,
        bodyDurationSeconds = 18.81,
        promoEndCardSeconds = 2.5,
        scene2StartSeconds = 9,
        directVideoUrl = "https://storage.googleapis.com/gen-lang-client-0150678391-biblebible-shorts/review-final-v1/media/16-act2-ministry-at-table.mp4?v=1795471116844",
    ),
    shortItem(
        order = 17,
        id = "17-luk16-faithful-small-things",
        title = "Faithful in Small Details",
        category = "Faith & Courage",
        durationSeconds = 26,
        scriptureReference = "Luke 16:10",
        bookId = 42,
        bookUsfm = "LUK",
        chapter = 16,
        verse = 10,
        keyVerseText = "He that is faithful in that which is least is faithful also in much: and he that is unjust in the least is unjust also in much.",
        narrative = "Character is proven in quiet details when nobody checks behind you.",
        lesson = "Honor God in every unseen task before praying for bigger doors.",
        accentColorHex = "#3A5F8F",
        gatewayBaseUrl = gatewayBaseUrl,
        bodyDurationSeconds = 23.48,
        promoEndCardSeconds = 2.5,
        scene2StartSeconds = 9,
        directVideoUrl = "https://storage.googleapis.com/gen-lang-client-0150678391-biblebible-shorts/review-final-v1/media/17-luk16-faithful-small-things.mp4?v=1795471116844",
    ),
    shortItem(
        order = 18,
        id = "18-mat7-built-on-rock",
        title = "Built Before the Storm",
        category = "Faith & Courage",
        durationSeconds = 27,
        scriptureReference = "Matthew 7:24",
        bookId = 40,
        bookUsfm = "MAT",
        chapter = 7,
        verse = 24,
        keyVerseText = "Therefore whosoever heareth these sayings of mine, and doeth them, I will liken him unto a wise man, which built his house upon a rock:",
        narrative = "Storms don't invent structural flaws. They reveal what was laid in calm days.",
        lesson = "Don't just read a chapter. Put one verse into practice today.",
        accentColorHex = "#2C5282",
        gatewayBaseUrl = gatewayBaseUrl,
        bodyDurationSeconds = 24.25,
        promoEndCardSeconds = 2.5,
        scene2StartSeconds = 10,
        directVideoUrl = "https://storage.googleapis.com/gen-lang-client-0150678391-biblebible-shorts/review-final-v1/media/18-mat7-built-on-rock.mp4?v=1795471116844",
    ),
    shortItem(
        order = 19,
        id = "19-jer2-living-fountain",
        title = "Return to the Fountain",
        category = "Grace & Truth",
        durationSeconds = 24,
        scriptureReference = "Jeremiah 2:13",
        bookId = 24,
        bookUsfm = "JER",
        chapter = 2,
        verse = 13,
        keyVerseText = "For my people have committed two evils; they have forsaken me the fountain of living waters, and hewed them out cisterns, broken cisterns, that can hold no water.",
        narrative = "Earthly distractions are broken cisterns that cannot hold real peace.",
        lesson = "Step back to the living spring that never runs dry.",
        accentColorHex = "#2A6F97",
        gatewayBaseUrl = gatewayBaseUrl,
        bodyDurationSeconds = 21.36,
        promoEndCardSeconds = 2.5,
        scene2StartSeconds = 9,
        directVideoUrl = "https://storage.googleapis.com/gen-lang-client-0150678391-biblebible-shorts/review-final-v1/media/19-jer2-living-fountain.mp4?v=1795471116844",
    ),
    shortItem(
        order = 20,
        id = "20-psa27-set-upon-rock",
        title = "Set Upon a Rock",
        category = "Faith & Courage",
        durationSeconds = 26,
        scriptureReference = "Psalm 27:5",
        bookId = 19,
        bookUsfm = "PSA",
        chapter = 27,
        verse = 5,
        keyVerseText = "For in the time of trouble he shall hide me in his pavilion: in the secret of his tabernacle shall he hide me; he shall set me up upon a rock.",
        narrative = "Stop hiding from God. Step honestly into His unshakeable shelter.",
        lesson = "Truth brings peace; honest prayer stands on solid ground.",
        accentColorHex = "#5B4B8A",
        gatewayBaseUrl = gatewayBaseUrl,
        bodyDurationSeconds = 23.69,
        promoEndCardSeconds = 2.5,
        scene2StartSeconds = 9,
        directVideoUrl = "https://storage.googleapis.com/gen-lang-client-0150678391-biblebible-shorts/review-final-v1/media/20-psa27-set-upon-rock.mp4?v=1795471116844",
    ),
    shortItem(
        order = 21,
        id = "21-jas1-master-response",
        title = "Master Your Response",
        category = "Wisdom & Peace",
        durationSeconds = 27,
        scriptureReference = "James 1:19",
        bookId = 59,
        bookUsfm = "JAS",
        chapter = 1,
        verse = 19,
        keyVerseText = "Wherefore, my beloved brethren, let every man be swift to hear, slow to speak, slow to wrath:",
        narrative = "An instant reaction feels strong for seconds and costs weeks of peace.",
        lesson = "Listen twice, pray once, and protect unity before replying.",
        accentColorHex = "#285E61",
        gatewayBaseUrl = gatewayBaseUrl,
        bodyDurationSeconds = 24.05,
        promoEndCardSeconds = 2.5,
        scene2StartSeconds = 10,
        directVideoUrl = "https://storage.googleapis.com/gen-lang-client-0150678391-biblebible-shorts/review-final-v1/media/21-jas1-master-response.mp4?v=1795471116844",
    ),
    shortItem(
        order = 22,
        id = "22-1co15-steadfast-unmovable",
        title = "Steadfast and Unmovable",
        category = "Faith & Courage",
        durationSeconds = 27,
        scriptureReference = "1 Corinthians 15:58",
        bookId = 46,
        bookUsfm = "1CO",
        chapter = 15,
        verse = 58,
        keyVerseText = "Therefore, my beloved brethren, be ye stedfast, unmoveable, always abounding in the work of the Lord, forasmuch as ye know that your labour is not in vain in the Lord.",
        narrative = "Even quiet, unseen faithfulness is never wasted in God's Kingdom.",
        lesson = "Stay planted where God called you. He keeps every record.",
        accentColorHex = "#2C5282",
        gatewayBaseUrl = gatewayBaseUrl,
        bodyDurationSeconds = 24.81,
        promoEndCardSeconds = 2.5,
        scene2StartSeconds = 9,
        directVideoUrl = "https://storage.googleapis.com/gen-lang-client-0150678391-biblebible-shorts/review-final-v1/media/22-1co15-steadfast-unmovable.mp4?v=1795471116844",
    ),
    shortItem(
        order = 23,
        id = "23-jos1-moving-obedience",
        title = "Moving in Obedience",
        category = "Faith & Courage",
        durationSeconds = 27,
        scriptureReference = "Joshua 1:3",
        bookId = 6,
        bookUsfm = "JOS",
        chapter = 1,
        verse = 3,
        keyVerseText = "Every place that the sole of your foot shall tread upon, that have I given unto you, as I said unto Moses.",
        narrative = "Promises rarely unfold while we stand frozen in yesterday's hesitation.",
        lesson = "Take today's obedient step. God meets faith already in motion.",
        accentColorHex = "#2F667A",
        gatewayBaseUrl = gatewayBaseUrl,
        bodyDurationSeconds = 24.57,
        promoEndCardSeconds = 2.5,
        scene2StartSeconds = 10,
        directVideoUrl = "https://storage.googleapis.com/gen-lang-client-0150678391-biblebible-shorts/review-final-v1/media/23-jos1-moving-obedience.mp4?v=1795471116844",
    ),
    shortItem(
        order = 24,
        id = "24-pro4-guard-your-heart",
        title = "Guard Your Heart",
        category = "Wisdom & Peace",
        durationSeconds = 24,
        scriptureReference = "Proverbs 4:23",
        bookId = 20,
        bookUsfm = "PRO",
        chapter = 4,
        verse = 23,
        keyVerseText = "Keep thy heart with all diligence; for out of it are the issues of life.",
        narrative = "What enters your thoughts all week shapes your words and decisions.",
        lesson = "Guard what gets access to your quiet hours before tomorrow.",
        accentColorHex = "#3A5F8F",
        gatewayBaseUrl = gatewayBaseUrl,
        bodyDurationSeconds = 21.57,
        promoEndCardSeconds = 2.5,
        scene2StartSeconds = 11,
        directVideoUrl = "https://storage.googleapis.com/gen-lang-client-0150678391-biblebible-shorts/review-final-v1/media/24-pro4-guard-your-heart.mp4?v=1795471116844",
    ),
)

internal class BiblicalShortsRepository(
    private val fetch: suspend () -> BiblicalShortsFeedDto = { fetchBiblicalShortsFeed() },
    private val fallbackProvider: () -> List<BiblicalShortDto> = { defaultBiblicalShortsCatalog() },
) {
    private val mutex = Mutex()
    private var cachedShorts: List<BiblicalShortDto>? = null

    suspend fun getShorts(forceRefresh: Boolean = false): List<BiblicalShortDto> = mutex.withLock {
        if (!forceRefresh) {
            cachedShorts?.let { return@withLock it }
        }
        val loaded = try {
            val remote = fetch()
            if (remote.shorts.isNotEmpty()) remote.shorts else fallbackProvider()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            fallbackProvider()
        }
        cachedShorts = loaded
        loaded
    }

    fun filterByCategory(shorts: List<BiblicalShortDto>, category: String): List<BiblicalShortDto> {
        val clean = category.trim()
        if (clean.isEmpty() || clean.equals("All", ignoreCase = true)) {
            return shorts
        }
        return shorts.filter { it.category.equals(clean, ignoreCase = true) }
    }
}

internal suspend fun fetchBiblicalShortsFeed(
    client: HttpClient = httpClientCloudGateway,
): BiblicalShortsFeedDto {
    val response = client.getBiblicalShorts()
    check(response.status.isSuccess()) {
        "Biblical Shorts unavailable (${response.status.value})."
    }
    return response.body()
}

internal val biblicalShortsRepository by lazy {
    BiblicalShortsRepository()
}
