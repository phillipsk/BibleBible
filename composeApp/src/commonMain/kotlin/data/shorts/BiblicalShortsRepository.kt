package data.shorts

import data.bibleIQ.GetBiblicalShorts
import data.config.CloudSessionManager
import data.httpClientBibleIQ
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
    val totalCount: Int = 50,
    val maxDurationSeconds: Int = 30,
    val categories: List<String> = emptyList(),
    val shorts: List<BiblicalShortDto> = emptyList(),
)

internal val biblicalShortsCategories = listOf(
    "All",
    "Faith & Courage",
    "Grace & Forgiveness",
    "Wisdom & Peace",
    "Miracles of Jesus",
    "Hope & Renewal",
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
            BiblicalShortSceneDto(1, 0, split1, "The Narrative", narrative),
            BiblicalShortSceneDto(2, split1, split2, "The Scripture", "$scriptureReference — \"$keyVerseText\""),
            BiblicalShortSceneDto(3, split2, durationSeconds, "The Lesson", lesson),
        ),
    )
}

internal fun defaultBiblicalShortsCatalog(
    gatewayBaseUrl: String = CloudSessionManager.configuredGatewayUrl,
): List<BiblicalShortDto> = listOf(
    shortItem(1, "short-01-creation-light", "Creation & The Power of Light", "Hope & Renewal", 24, "Genesis 1:3-5", 1, "GEN", 1, 3,
        "And God said, Let there be light: and there was light.",
        "Before order existed, God spoke light into formless darkness and called creation good.",
        "No season is too dark for God to speak fresh clarity, order, and purpose into your life.", "#3A5F8F", gatewayBaseUrl),
    shortItem(2, "short-02-noah-perseverance", "Noah's Quiet Perseverance", "Faith & Courage", 25, "Genesis 6:22", 1, "GEN", 6, 22,
        "Thus did Noah; according to all that God commanded him, so did he.",
        "Long before the first raindrop fell, Noah faithfully built an ark one plank at a time.",
        "Obedience rarely looks urgent to the crowd, yet daily faithfulness prepares you for tomorrow's storm.", "#2F667A", gatewayBaseUrl),
    shortItem(3, "short-03-abraham-step-of-faith", "Abraham's Step Into the Unknown", "Faith & Courage", 24, "Genesis 12:1-2", 1, "GEN", 12, 1,
        "Get thee out of thy country... unto a land that I will shew thee.",
        "God called Abram to leave everything familiar without giving him a map of the destination.",
        "God often reveals the next mile only after you take the first step in trust.", "#5B4B8A", gatewayBaseUrl),
    shortItem(4, "short-04-jacobs-ladder", "Jacob's Ladder & Unexpected Grace", "Grace & Forgiveness", 23, "Genesis 28:15-16", 1, "GEN", 28, 16,
        "Surely the LORD is in this place; and I knew it not.",
        "Fleeing his own mistakes in the desert, Jacob dreamed of heaven touching earth right where he slept.",
        "Even in your lonely detours, God has not abandoned you—grace meets you right where you are.", "#3E5C76", gatewayBaseUrl),
    shortItem(5, "short-05-joseph-forgiveness", "Joseph: From Pit to Purpose", "Grace & Forgiveness", 26, "Genesis 50:20", 1, "GEN", 50, 20,
        "Ye thought evil against me; but God meant it unto good.",
        "Betrayed by his brothers and falsely imprisoned, Joseph chose forgiveness when given power over them.",
        "What others intended to break you can become the very classroom God uses to equip you.", "#7A5230", gatewayBaseUrl),
    shortItem(6, "short-06-burning-bush", "Moses & The Burning Bush", "Faith & Courage", 25, "Exodus 3:11-12", 2, "EXO", 3, 12,
        "Certainly I will be with thee.",
        "Feeling inadequate after forty years in Midian, Moses objected—and God answered with His own presence.",
        "Your calling does not depend on your eloquence; it rests on who goes with you.", "#8C4A27", gatewayBaseUrl),
    shortItem(7, "short-07-red-sea-deliverance", "Stand Still at the Red Sea", "Faith & Courage", 24, "Exodus 14:13-14", 2, "EXO", 14, 14,
        "The LORD shall fight for you, and ye shall hold your peace.",
        "Trapped between Pharaoh's chariots and deep water, Israel saw a dry road open straight through the sea.",
        "When every human exit seems blocked, stillness and trust make room for God's deliverance.", "#2A6F97", gatewayBaseUrl),
    shortItem(8, "short-08-ten-commandments-wisdom", "Boundaries That Protect Joy", "Wisdom & Peace", 23, "Exodus 20:2-3", 2, "EXO", 20, 2,
        "I am the LORD thy God, which have brought thee out of the land of Egypt.",
        "At Sinai, God gave Israel freedom first, then commandments designed to protect community and peace.",
        "God's boundaries are never cages—they are guardrails that keep what matters most intact.", "#4A5568", gatewayBaseUrl),
    shortItem(9, "short-09-joshua-courage", "Joshua: Strong & Courageous", "Faith & Courage", 22, "Joshua 1:9", 6, "JOS", 1, 9,
        "Be strong and of a good courage... for the LORD thy God is with thee.",
        "Stepping into Moses' shoes before an uncertain frontier, Joshua received a promise stronger than fear.",
        "Courage is not the absence of fear; it is refusing to let fear write your next decision.", "#6B4E71", gatewayBaseUrl),
    shortItem(10, "short-10-gideon-jars-of-clay", "Gideon & The 300 Torches", "Faith & Courage", 25, "Judges 7:7", 7, "JDG", 7, 7,
        "By the three hundred men that lapped will I save you.",
        "God trimmed Gideon's army from 32,000 down to 300 carrying clay jars and torches.",
        "Small resources in surrendered hands overcome overwhelming odds so no one mistakes the source of victory.", "#8B5A2B", gatewayBaseUrl),
    shortItem(11, "short-11-ruth-loyalty", "Ruth's Steadfast Loyalty", "Grace & Forgiveness", 24, "Ruth 1:16", 8, "RUT", 1, 16,
        "Whither thou goest, I will go; and where thou lodgest, I will lodge.",
        "In a season of grief and scarcity, Ruth chose sacrificial loyalty to Naomi and was woven into royal history.",
        "Faithful love shown in quiet, ordinary seasons carries generational weight.", "#5C6B73", gatewayBaseUrl),
    shortItem(12, "short-12-hannah-prayer", "Hannah's Honest Prayer", "Hope & Renewal", 23, "1 Samuel 1:27", 9, "1SA", 1, 27,
        "For this child I prayed; and the LORD hath given me my petition.",
        "Misunderstood even by the priest, Hannah poured out silent tears at Shiloh and walked away at peace.",
        "You never have to polish your pain before bringing it to God; honest prayer invites real peace.", "#3D5A80", gatewayBaseUrl),
    shortItem(13, "short-13-samuel-listening", "Samuel: Learning to Listen", "Wisdom & Peace", 22, "1 Samuel 3:10", 9, "1SA", 3, 10,
        "Speak; for thy servant heareth.",
        "In the stillness of night, young Samuel learned to recognize God's voice over the noise of routine.",
        "Wisdom begins when we pause our talking long enough to answer, 'Speak, Lord, I am listening.'", "#2C5282", gatewayBaseUrl),
    shortItem(14, "short-14-david-and-goliath", "David & Goliath: Unseen Confidence", "Faith & Courage", 26, "1 Samuel 17:45", 9, "1SA", 17, 45,
        "I come to thee in the name of the LORD of hosts.",
        "While seasoned soldiers measured Goliath's height, a shepherd boy measured God's faithfulness.",
        "Remembering past quiet victories equips you to face loud giants without borrowed armor.", "#744210", gatewayBaseUrl),
    shortItem(15, "short-15-psalm-23-shepherd", "Psalm 23: The Shepherd's Care", "Wisdom & Peace", 25, "Psalms 23:1-4", 19, "PSA", 23, 1,
        "The LORD is my shepherd; I shall not want.",
        "David wrote not from a throne of ease, but as one who knew both green pastures and dark valleys.",
        "Peace does not require the absence of valleys; it flows from knowing who walks beside you through them.", "#2F6B4F", gatewayBaseUrl),
    shortItem(16, "short-16-solomon-wisdom", "Solomon: Asking for a Listening Heart", "Wisdom & Peace", 24, "1 Kings 3:9", 11, "1KI", 3, 9,
        "Give therefore thy servant an understanding heart to judge thy people.",
        "Offered anything he wished, young Solomon asked neither for wealth nor victory, but wisdom to serve.",
        "When your primary aim is serving others well, clarity and integrity follow naturally.", "#553C9A", gatewayBaseUrl),
    shortItem(17, "short-17-elijah-still-voice", "Elijah & The Still Small Voice", "Wisdom & Peace", 26, "1 Kings 19:12", 11, "1KI", 19, 12,
        "And after the fire a still small voice.",
        "Burned out and hiding in a cave, Elijah saw wind, quake, and fire pass—yet God spoke in a whisper.",
        "Rest and quiet are not spiritual laziness; often they are where exhausted souls hear God clearest.", "#3B5266", gatewayBaseUrl),
    shortItem(18, "short-18-naaman-humble-obedience", "Naaman & Simple Obedience", "Grace & Forgiveness", 24, "2 Kings 5:13", 12, "2KI", 5, 13,
        "If the prophet had bid thee do some great thing, wouldest thou not have done it?",
        "A celebrated commander nearly missed healing because dipping seven times in the Jordan felt too ordinary.",
        "Do not despise simple, humble steps of healing while waiting for dramatic spectacles.", "#2B6CB0", gatewayBaseUrl),
    shortItem(19, "short-19-esther-such-a-time", "Esther: For Such a Time as This", "Faith & Courage", 25, "Esther 4:14", 17, "EST", 4, 14,
        "Who knoweth whether thou art come to the kingdom for such a time as this?",
        "Risking her royal position and life, Esther spoke up to rescue an entire people from destruction.",
        "Your current seat of influence is rarely accidental—use your voice for people who cannot speak.", "#7B341E", gatewayBaseUrl),
    shortItem(20, "short-20-job-endurance", "Job: Anchored Beyond Circumstances", "Hope & Renewal", 25, "Job 19:25", 18, "JOB", 19, 25,
        "For I know that my redeemer liveth.",
        "Stripped of possessions, health, and easy answers, Job refused to pretend, yet held fast to God.",
        "Real faith is not denial of grief; it is holding on to hope when the explanations run out.", "#4A5568", gatewayBaseUrl),
    shortItem(21, "short-21-daniel-lions-den", "Daniel's Daily Integrity", "Faith & Courage", 24, "Daniel 6:10", 27, "DAN", 6, 10,
        "He kneeled upon his knees three times a day, and prayed... as he did aforetime.",
        "When royal decree banned prayer, Daniel kept the quiet habits that had anchored him for decades.",
        "Courage in a crisis is forged long before the crisis through steady daily character.", "#2C5282", gatewayBaseUrl),
    shortItem(22, "short-22-fiery-furnace", "Three Friends in the Furnace", "Faith & Courage", 25, "Daniel 3:17-18", 27, "DAN", 3, 18,
        "But if not, be it known unto thee, O king, that we will not serve thy gods.",
        "Shadrach, Meshach, and Abednego trusted God to rescue them—and trusted Him even if He did not.",
        "Unshakeable peace comes when your faithfulness is no longer conditional on an easy outcome.", "#9C4221", gatewayBaseUrl),
    shortItem(23, "short-23-jonah-second-chance", "Jonah & The God of Second Chances", "Grace & Forgiveness", 24, "Jonah 3:1-2", 32, "JON", 3, 1,
        "And the word of the LORD came unto Jonah the second time.",
        "After running in the opposite direction, a humbled Jonah received his calling a second time—and Nineveh repented.",
        "Failure is not the final chapter; mercy specializes in writing second beginnings.", "#285E61", gatewayBaseUrl),
    shortItem(24, "short-24-isaiah-wait-on-lord", "Strength Renewed Like Eagles", "Hope & Renewal", 23, "Isaiah 40:31", 23, "ISA", 40, 31,
        "They that wait upon the LORD shall renew their strength.",
        "Speaking to weary exiles, Isaiah reminded them that the Creator never grows faint or tired.",
        "Waiting on God is not passive delay—it is exchanging depleted willpower for enduring strength.", "#3A5F8F", gatewayBaseUrl),
    shortItem(25, "short-25-jeremiah-future-hope", "Jeremiah: Plans for Hope in Exile", "Hope & Renewal", 24, "Jeremiah 29:11", 24, "JER", 29, 11,
        "For I know the thoughts that I think toward you... to give you an expected end.",
        "Written to captives facing seventy years in Babylon, this promise called them to build and pray right there.",
        "You do not have to wait for ideal conditions to sow peace, build community, and trust God's future.", "#4C51BF", gatewayBaseUrl),
    shortItem(26, "short-26-nativity-humility", "The Manger: Greatness in Humility", "Grace & Forgiveness", 24, "Luke 2:10-11", 42, "LUK", 2, 10,
        "Behold, I bring you good tidings of great joy, which shall be to all people.",
        "Heaven announced the arrival of the King not in a palace, but to night-shift shepherds outside Bethlehem.",
        "God draws nearest to the overlooked; never confuse lowliness with insignificance.", "#2B4C7E", gatewayBaseUrl),
    shortItem(27, "short-27-baptism-beloved-identity", "Beloved Before Any Achievement", "Grace & Forgiveness", 23, "Matthew 3:17", 40, "MAT", 3, 17,
        "This is my beloved Son, in whom I am well pleased.",
        "Before Jesus performed a single public miracle, the Father spoke affirmation and love over Him.",
        "Live from belonging rather than performing for approval—identity comes before assignment.", "#2A6F97", gatewayBaseUrl),
    shortItem(28, "short-28-wilderness-truth", "Standing Firm in the Wilderness", "Faith & Courage", 24, "Matthew 4:4", 40, "MAT", 4, 4,
        "Man shall not live by bread alone, but by every word that proceedeth out of the mouth of God.",
        "Hungry and tested alone for forty days, Jesus answered shortcuts and illusions with rooted Scripture.",
        "Store truth in your heart during peaceful seasons so it steadies you in moments of temptation.", "#7A5230", gatewayBaseUrl),
    shortItem(29, "short-29-beatitudes-upside-down", "The Beatitudes: Kingdom Joy", "Wisdom & Peace", 25, "Matthew 5:8-9", 40, "MAT", 5, 9,
        "Blessed are the peacemakers: for they shall be called the children of God.",
        "On a Galilean hillside, Jesus turned status upside down, blessing the meek, merciful, and peacemakers.",
        "True flourishing grows from humility, mercy, and active peacemaking rather than status.", "#3182CE", gatewayBaseUrl),
    shortItem(30, "short-30-salt-and-light", "Salt & Light Where You Are", "Wisdom & Peace", 22, "Matthew 5:14-16", 40, "MAT", 5, 16,
        "Let your light so shine before men, that they may see your good works.",
        "A tiny candle transforms a dark room, and a pinch of salt preserves everything it touches.",
        "Everyday kindness, integrity, and encouragement quietly illuminate your neighborhood and workplace.", "#975A16", gatewayBaseUrl),
    shortItem(31, "short-31-lilies-let-go-of-worry", "Lilies of the Field: Release Worry", "Wisdom & Peace", 25, "Matthew 6:33-34", 40, "MAT", 6, 34,
        "Take therefore no thought for the morrow... Sufficient unto the day is the evil thereof.",
        "Pointing to wild birds and blossoms, Jesus invited anxious listeners to trade tomorrow's dread for daily trust.",
        "Worry borrows tomorrow's weight without adding an ounce of strength; focus on faithfulness today.", "#2F855A", gatewayBaseUrl),
    shortItem(32, "short-32-friends-lowered-roof", "Faith That Opens a Roof", "Miracles of Jesus", 24, "Mark 2:4-5", 41, "MRK", 2, 5,
        "When Jesus saw their faith, he said unto the sick of the palsy, Son, thy sins be forgiven thee.",
        "Four friends refused to let a crowded doorway stop them—they carried their paralyzed friend onto the roof.",
        "Be the kind of friend whose persistent love carries others closer to hope when they cannot walk alone.", "#3A5F8F", gatewayBaseUrl),
    shortItem(33, "short-33-calming-the-storm", "Peace, Be Still in the Storm", "Miracles of Jesus", 24, "Mark 4:39-40", 41, "MRK", 4, 39,
        "And he arose, and rebuked the wind, and said unto the sea, Peace, be still.",
        "Seasoned fishermen panicked as waves filled the boat, while Jesus stood up and spoke calm over the gale.",
        "The same voice that calms raging seas can quiet anxious storms inside your mind.", "#2B6CB0", gatewayBaseUrl),
    shortItem(34, "short-34-jairus-do-not-fear", "Do Not Fear, Only Believe", "Miracles of Jesus", 25, "Mark 5:36", 41, "MRK", 5, 36,
        "Be not afraid, only believe.",
        "When devastating news arrived before Jesus reached Jairus' home, Jesus steadied a grieving father's heart.",
        "Delay is not defeat—hold on to faith even when worst-case headlines arrive early.", "#5B4B8A", gatewayBaseUrl),
    shortItem(35, "short-35-feeding-five-thousand", "Five Loaves in Capable Hands", "Miracles of Jesus", 25, "John 6:9-11", 43, "JHN", 6, 11,
        "And Jesus took the loaves; and when he had given thanks, he distributed to the disciples.",
        "A boy's modest lunch looked laughable next to five thousand hungry people until Jesus blessed and broke it.",
        "Never withhold what little you have because it seems insufficient; gratitude multiplies offered gifts.", "#744210", gatewayBaseUrl),
    shortItem(36, "short-36-walking-on-water", "Eyes Fixed Above the Waves", "Miracles of Jesus", 24, "Matthew 14:29-31", 40, "MAT", 14, 31,
        "Immediately Jesus stretched forth his hand, and caught him.",
        "Peter stepped onto rough waters toward Jesus—and the instant he sank, Jesus immediately caught his hand.",
        "Even when your focus slips mid-step, grace reaches down faster than your fear pulls you under.", "#2A6F97", gatewayBaseUrl),
    shortItem(37, "short-37-good-samaritan", "The Good Samaritan: Active Compassion", "Grace & Forgiveness", 26, "Luke 10:33-34", 42, "LUK", 10, 33,
        "When he saw him, he had compassion on him.",
        "On a dangerous Jericho road, an outsider stopped, bandaged wounds, and paid the cost to restore a stranger.",
        "True neighborliness crosses social lines and turns quiet sympathy into practical kindness.", "#8C4A27", gatewayBaseUrl),
    shortItem(38, "short-38-martha-and-mary", "Choosing What Matters Most", "Wisdom & Peace", 24, "Luke 10:41-42", 42, "LUK", 10, 42,
        "One thing is needful: and Mary hath chosen that good part.",
        "Amidst frantic preparations in Bethany, Jesus gently reminded Martha not to trade presence for busyness.",
        "Do not let urgent checklists crowd out the stillness that nourishes your soul.", "#4A5568", gatewayBaseUrl),
    shortItem(39, "short-39-good-shepherd-john-10", "The Good Shepherd Knows Your Name", "Hope & Renewal", 24, "John 10:11", 43, "JHN", 10, 11,
        "I am the good shepherd: the good shepherd giveth his life for the sheep.",
        "Unlike hired hands who flee when danger arrives, Jesus stands watch and leads His flock by name.",
        "You are never an anonymous number; you are known, guarded, and deeply valued.", "#2F6B4F", gatewayBaseUrl),
    shortItem(40, "short-40-prodigal-son-welcome", "The Prodigal Son: Running Father", "Grace & Forgiveness", 27, "Luke 15:20", 42, "LUK", 15, 20,
        "When he was yet a great way off, his father saw him, and had compassion, and ran.",
        "Expecting servant wages after squandering everything, the younger son met a father sprinting down the road.",
        "No distance you have wandered outruns the Father's readiness to welcome you home.", "#7B341E", gatewayBaseUrl),
    shortItem(41, "short-41-persistent-prayer", "Keep Praying, Never Lose Heart", "Faith & Courage", 23, "Luke 18:1", 42, "LUK", 18, 1,
        "Men ought always to pray, and not to faint.",
        "Through a parable of a determined widow, Jesus taught disciples not to quit when answers seem slow.",
        "Keep knocking on heaven's door; persistence deepens trust even while you wait.", "#3E5C76", gatewayBaseUrl),
    shortItem(42, "short-42-zacchaeus-restored", "Zacchaeus: Seen & Transformed", "Grace & Forgiveness", 25, "Luke 19:5-9", 42, "LUK", 19, 5,
        "Zacchaeus, make haste, and come down; for to day I must abide at thy house.",
        "Despised in Jericho, a tax collector climbed a sycamore tree—and Jesus invited Himself to dinner.",
        "Unconditional grace transforms hearts far faster than public shame ever could.", "#285E61", gatewayBaseUrl),
    shortItem(43, "short-43-lazarus-resurrection-life", "Lazarus: Hope Beyond Delay", "Miracles of Jesus", 26, "John 11:25", 43, "JHN", 11, 25,
        "I am the resurrection, and the life: he that believeth in me, though he were dead, yet shall he live.",
        "Jesus wept beside Mary and Martha at a four-day-old tomb, then called Lazarus back into daylight.",
        "Even what feels four days too late is never beyond the reach of resurrection life.", "#553C9A", gatewayBaseUrl),
    shortItem(44, "short-44-widows-two-mites", "The Widow's Two Small Coins", "Wisdom & Peace", 23, "Mark 12:43-44", 41, "MRK", 12, 43,
        "This poor widow hath cast more in, than all they which have cast into the treasury.",
        "Passing over loud displays of surplus wealth, Jesus honored two tiny copper coins given from the heart.",
        "Heaven measures generosity not by headline size, but by love and wholehearted devotion.", "#744210", gatewayBaseUrl),
    shortItem(45, "short-45-washing-feet", "A Towel & Basin: Servant Leadership", "Grace & Forgiveness", 25, "John 13:14-15", 43, "JHN", 13, 15,
        "For I have given you an example, that ye should do as I have done to you.",
        "Hours before the cross, the Lord of the universe knelt with a towel to wash dusty travel-worn feet.",
        "True greatness is never measured by how many serve you, but by how humbly you lift others.", "#3A5F8F", gatewayBaseUrl),
    shortItem(46, "short-46-gethsemane-surrender", "Gethsemane: Peace Through Surrender", "Faith & Courage", 25, "Luke 22:42", 42, "LUK", 22, 42,
        "Nevertheless not my will, but thine, be done.",
        "In deep anguish beneath olive trees, Jesus prayed honestly for another way—then surrendered to love's mission.",
        "Surrendering your plan to God does not shrink your courage; it anchors it in unbreakable peace.", "#2C5282", gatewayBaseUrl),
    shortItem(47, "short-47-cross-mercy", "Father, Forgive Them", "Grace & Forgiveness", 25, "Luke 23:34", 42, "LUK", 23, 34,
        "Father, forgive them; for they know not what they do.",
        "In the sharpest suffering imaginable, Jesus met hostility not with retaliation, but interceding mercy.",
        "Forgiveness sets both the offended and offender free—release bitterness before it hardens your soul.", "#7B341E", gatewayBaseUrl),
    shortItem(48, "short-48-empty-tomb", "The Empty Tomb: Death Defeated", "Hope & Renewal", 26, "Luke 24:5-6", 42, "LUK", 24, 6,
        "He is not here, but is risen.",
        "At dawn on the first day of the week, the rolled-away stone declared that darkness never gets the final word.",
        "Because hope walked out of an empty grave, no sorrow in your life is permanent.", "#2F667A", gatewayBaseUrl),
    shortItem(49, "short-49-road-to-emmaus", "Road to Emmaus: Walking Beside Us", "Hope & Renewal", 25, "Luke 24:32", 42, "LUK", 24, 32,
        "Did not our heart burn within us, while he talked with us by the way?",
        "Two heartbroken travelers walked away from Jerusalem until the risen Jesus joined their steps and opened Scripture.",
        "Even when your hopes feel shattered, Christ quietly walks beside you to rekindle your heart.", "#6B4E71", gatewayBaseUrl),
    shortItem(50, "short-50-all-things-new", "All Things Made New", "Hope & Renewal", 25, "Revelation 21:5", 66, "REV", 21, 5,
        "Behold, I make all things new.",
        "Scripture closes with tears wiped away, fractured nations healed, and creation made whole forever.",
        "You are living inside a story heading toward restoration—press forward today with unshakeable hope.", "#3A5F8F", gatewayBaseUrl),
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
    client: HttpClient = httpClientBibleIQ,
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
