package data.shorts

import data.bible.GetBiblicalShorts
import data.config.CloudSessionManager
import data.httpClientCloudGateway
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.resources.get
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable

@Serializable
internal data class BiblicalShortSceneDto(
    val index: Int,
    val startSeconds: Int,
    val endSeconds: Int,
    val heading: String,
    val caption: String,
)

@Serializable
internal data class BiblicalShortDto(
    val id: String,
    val order: Int,
    val title: String,
    val category: String,
    val durationSeconds: Int,
    val scriptureReference: String,
    val bookId: Int,
    val bookUsfm: String,
    val chapter: Int,
    val verse: Int,
    val keyVerseText: String,
    val narrative: String,
    val lesson: String,
    val accentColorHex: String = "#3A5F8F",
    val videoUrl: String = "",
    val scenes: List<BiblicalShortSceneDto> = emptyList(),
)

@Serializable
internal data class BiblicalShortsFeedDto(
    val schemaVersion: Int = 1,
    val totalCount: Int = 10,
    val maxDurationSeconds: Int = 30,
    val categories: List<String> = emptyList(),
    val shorts: List<BiblicalShortDto> = emptyList(),
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
): BiblicalShortDto {
    val split1 = durationSeconds / 3
    val split2 = (durationSeconds * 2) / 3
    val cleanBase = gatewayBaseUrl.trim().trimEnd('/')
    val resolvedVideoUrl = if (cleanBase.isNotEmpty() && !cleanBase.contains("unconfigured.invalid")) {
        val prefix = if ("://" in cleanBase) cleanBase else "https://$cleanBase"
        "$prefix/v1/shorts/media/$id.mp4"
    } else {
        ""
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
        scenes = listOf(
            BiblicalShortSceneDto(1, 0, split1, "The Principle", narrative),
            BiblicalShortSceneDto(2, split1, split2, "The Scripture", "$scriptureReference — \"$keyVerseText\""),
            BiblicalShortSceneDto(3, split2, durationSeconds, "Walk It Out", lesson),
        ),
    )
}

internal fun defaultBiblicalShortsCatalog(
    gatewayBaseUrl: String = CloudSessionManager.configuredGatewayUrl,
): List<BiblicalShortDto> = listOf(
    shortItem(
        1,
        "01-1a-inhouse-moving-in-obedience",
        "Moving in Obedience",
        "Faith & Courage",
        16,
        "Joshua 1:3",
        6,
        "JOS",
        1,
        3,
        "Every place that the sole of your foot shall tread upon, that have I given unto you.",
        "God's promises rarely unfold while we stand frozen in yesterday's comfort and hesitation.",
        "Step forward today in faithful obedience; God meets motion with provision.",
        "#2F667A",
        gatewayBaseUrl,
    ),
    shortItem(
        2,
        "02-1a-inhouse-covenant-provision",
        "God's Covenant Provision",
        "Hope & Renewal",
        20,
        "Psalms 37:25",
        19,
        "PSA",
        37,
        25,
        "I have not seen the righteous forsaken, nor his seed begging bread.",
        "When expenses rise and tomorrow feels uncertain, remember every season God already carried you through.",
        "Remember His faithfulness, walk in stewardship, and trust His covenant provision.",
        "#2F6B4F",
        gatewayBaseUrl,
    ),
    shortItem(
        3,
        "03-1a-beginner-scripture-lamp",
        "A Lamp for Today's Step",
        "Wisdom & Peace",
        21,
        "Psalms 119:105",
        19,
        "PSA",
        119,
        105,
        "Thy word is a lamp unto my feet, and a light unto my path.",
        "You don't need the entire five-year map to walk with God—scripture gives clear light for the very next step.",
        "Open one chapter before the noise starts so truth illuminates today's decision.",
        "#744210",
        gatewayBaseUrl,
    ),
    shortItem(
        4,
        "04-1a-intermediate-guard-your-heart",
        "Guard Your Heart",
        "Wisdom & Peace",
        17,
        "Proverbs 4:23",
        20,
        "PRO",
        4,
        23,
        "Keep thy heart with all diligence; for out of it are the issues of life.",
        "What quietly leaks into your mind all week eventually shapes your words, your home, and your choices.",
        "Filter what feeds your attention so wisdom and peace govern every reaction.",
        "#3A5F8F",
        gatewayBaseUrl,
    ),
    shortItem(
        5,
        "05-1a-advanced-sanctification",
        "Declared vs. Shaped",
        "Grace & Truth",
        18,
        "Philippians 1:6",
        50,
        "PHP",
        1,
        6,
        "He which hath begun a good work in you will perform it until the day of Jesus Christ.",
        "Justification declares you forgiven in a moment; sanctification is God patiently shaping your character over a lifetime.",
        "Do not grow discouraged mid-process—God finishes every transforming work He starts.",
        "#5B4B8A",
        gatewayBaseUrl,
    ),
    shortItem(
        6,
        "06-1b-inhouse-purpose-in-pain",
        "Purpose Inside Your Pain",
        "Hope & Renewal",
        17,
        "Romans 8:28",
        45,
        "ROM",
        8,
        28,
        "All things work together for good to them that love God, to them who are the called according to his purpose.",
        "Hard seasons never mean God forgot you—sometimes the storm waters the roots meant to carry tomorrow's harvest.",
        "Hold steady in trial knowing God can redeem what wounded you for lasting good.",
        "#285E61",
        gatewayBaseUrl,
    ),
    shortItem(
        7,
        "07-1b-inhouse-abide-in-truth",
        "Abiding in the Shelter",
        "Hope & Renewal",
        19,
        "Psalms 91:1",
        19,
        "PSA",
        91,
        1,
        "He that dwelleth in the secret place of the most High shall abide under the shadow of the Almighty.",
        "Visiting God's presence only in emergencies leaves us anxious—peace belongs to those who dwell in His Word daily.",
        "Make scripture your dwelling place rather than a last-minute shelter.",
        "#8C4A27",
        gatewayBaseUrl,
    ),
    shortItem(
        8,
        "08-1b-inhouse-steadfast-unmovable",
        "Steadfast and Unmovable",
        "Faith & Courage",
        19,
        "1 Corinthians 15:58",
        46,
        "1CO",
        15,
        58,
        "Be ye stedfast, unmoveable, always abounding in the work of the Lord... your labour is not in vain.",
        "When quiet service feels unseen and exhausting, remember that heaven never loses track of faithful labor.",
        "Stand firm without drifting; quiet faithfulness carries eternal weight.",
        "#2C5282",
        gatewayBaseUrl,
    ),
    shortItem(
        9,
        "09-1b-intermediate-peace-guards-heart",
        "Peace That Stands Guard",
        "Wisdom & Peace",
        21,
        "Philippians 4:6-7",
        50,
        "PHP",
        4,
        6,
        "Be careful for nothing; but in every thing by prayer and supplication with thanksgiving let your requests be made known unto God.",
        "Anxiety replays worst-case futures in a loop—prayer interrupts the spiral by placing every detail in God's hands.",
        "Trade restless worry for prayerful thanksgiving and let God's peace guard your mind.",
        "#2A6F97",
        gatewayBaseUrl,
    ),
    shortItem(
        10,
        "10-1b-advanced-law-on-heart",
        "Written on the Heart",
        "Grace & Truth",
        22,
        "Jeremiah 31:33",
        24,
        "JER",
        31,
        33,
        "I will put my law in their inward parts, and write it in their hearts; and will be their God, and they shall be my people.",
        "External rules showed holiness from afar, while the New Covenant writes God's life within the believer's heart.",
        "Walk out of love and inward renewal rather than fear-driven obligation.",
        "#7A5230",
        gatewayBaseUrl,
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
    val response = client.get(GetBiblicalShorts())
    check(response.status.isSuccess()) {
        "Biblical Shorts unavailable (${response.status.value})."
    }
    return response.body()
}

internal val biblicalShortsRepository by lazy {
    BiblicalShortsRepository()
}
