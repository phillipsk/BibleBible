package data.plans

import data.bible.bibleBooksFull
import data.notifications.bookNumberForUsfm

internal object BundledReadingPlans {
    private fun reading(
        planId: String,
        dayNumber: Int,
        slotIndex: Int,
        bookId: String,
        chapter: Int,
    ): ReadingPlanVerseRefDto {
        val usfm = bookId.uppercase()
        val bookNumber = bookNumberForUsfm(usfm) ?: 1
        val bookName = bibleBooksFull.getOrNull(bookNumber - 1) ?: usfm
        return ReadingPlanVerseRefDto(
            id = "$planId:d$dayNumber:r$slotIndex",
            bookId = usfm,
            bookNumber = bookNumber,
            bookName = bookName,
            chapter = chapter,
            label = "$bookName $chapter",
        )
    }

    private fun buildProverbs31(): ReadingPlanDto {
        val themes = listOf(
            "The Beginning of Knowledge", "The Value of Wisdom", "Trust in the Lord With All Your Heart",
            "Guard Your Heart Above All Else", "Stay on the Path of Integrity", "Diligence and Honesty",
            "Wisdom Calls Out", "The Blessings of Wisdom", "Wisdom Builds Her House",
            "Wise Words Bring Life", "Integrity Guides the Upright", "Loving Discipline and Truth",
            "Hope and Wise Counsel", "Fearing the Lord Gives Security", "A Soft Answer Turns Away Wrath",
            "Commit Your Works to the Lord", "A Friend Loves at All Times", "The Name of the Lord Is a Strong Tower",
            "Patience and Kindness", "Listen to Counsel", "Justice and Steadfast Love",
            "A Good Name and Generosity", "Apply Your Heart to Instruction", "Built on Wisdom",
            "Self-Control and Peace", "Speak Truth With Integrity", "Iron Sharpens Iron",
            "Walking Blamelessly", "Righteous Leadership", "Humility and Gratitude", "Strength and Honor",
        )
        return ReadingPlanDto(
            id = "proverbs_in_31_days",
            title = "Proverbs in 31 Days",
            subtitle = "Practical daily wisdom from Solomon's Proverbs.",
            category = "Wisdom",
            durationDays = 31,
            sourceAttribution = "Public domain canonical chapter schedule",
            accentHex = "#B59442",
            days = themes.mapIndexed { idx, title ->
                val day = idx + 1
                ReadingPlanDayDto(
                    dayNumber = day,
                    title = title,
                    reflectionPrompt = "Choose one proverb from chapter $day to practice in conversation or work today.",
                    readings = listOf(reading("proverbs_in_31_days", day, 1, "PRO", day)),
                )
            },
        )
    }

    private fun buildGospels30(): ReadingPlanDto {
        val schedule = listOf(
            "Birth and Preparation of Jesus" to listOf("MAT" to 1, "MAT" to 2),
            "Baptism and Temptation" to listOf("MAT" to 3, "MAT" to 4),
            "The Beatitudes and Salt of the Earth" to listOf("MAT" to 5),
            "Prayer, Generosity, and Trust" to listOf("MAT" to 6),
            "The Golden Rule and Firm Foundation" to listOf("MAT" to 7),
            "Authority and Faith" to listOf("MAT" to 8, "MAT" to 9),
            "Sending the Twelve" to listOf("MAT" to 10, "MAT" to 11),
            "Parables of the Kingdom" to listOf("MAT" to 13),
            "Walking on Water and Confession" to listOf("MAT" to 14, "MAT" to 16),
            "Humility and Forgiveness" to listOf("MAT" to 18),
            "The Greatest Commandment" to listOf("MAT" to 22, "MAT" to 25),
            "The Cross and the Empty Tomb" to listOf("MAT" to 27, "MAT" to 28),
            "The Servant King Begins" to listOf("MRK" to 1, "MRK" to 2),
            "Calming the Storm" to listOf("MRK" to 4, "MRK" to 5),
            "Feeding the Five Thousand" to listOf("MRK" to 6, "MRK" to 8),
            "Serving Others in Humility" to listOf("MRK" to 9, "MRK" to 10),
            "Watch and Pray" to listOf("MRK" to 13, "MRK" to 14),
            "He Is Risen" to listOf("MRK" to 15, "MRK" to 16),
            "Songs of Promise" to listOf("LUK" to 1, "LUK" to 2),
            "Good News to the Poor" to listOf("LUK" to 4, "LUK" to 6),
            "The Good Samaritan" to listOf("LUK" to 10),
            "The Prodigal Son" to listOf("LUK" to 15),
            "Persistent Prayer and Zacchaeus" to listOf("LUK" to 18, "LUK" to 19),
            "The Road to Emmaus" to listOf("LUK" to 24),
            "The Word Became Flesh" to listOf("JHN" to 1),
            "God So Loved the World" to listOf("JHN" to 3, "JHN" to 4),
            "The Bread of Life" to listOf("JHN" to 6),
            "The Good Shepherd" to listOf("JHN" to 10, "JHN" to 11),
            "The True Vine and Peace" to listOf("JHN" to 14, "JHN" to 15),
            "High Priestly Prayer and Restoration" to listOf("JHN" to 17, "JHN" to 21),
        )
        return ReadingPlanDto(
            id = "gospels_in_30_days",
            title = "Gospels in 30 Days",
            subtitle = "Walk through the life, parables, and resurrection of Jesus.",
            category = "Classic",
            durationDays = schedule.size,
            sourceAttribution = "Public domain canonical Gospel reading schedule",
            accentHex = "#B04F66",
            days = schedule.mapIndexed { idx, (title, refs) ->
                val day = idx + 1
                ReadingPlanDayDto(
                    dayNumber = day,
                    title = title,
                    reflectionPrompt = "Notice how Jesus speaks to people in ${title.lowercase()} and carry one lesson into your day.",
                    readings = refs.mapIndexed { slotIdx, (usfm, ch) ->
                        reading("gospels_in_30_days", day, slotIdx + 1, usfm, ch)
                    },
                )
            },
        )
    }

    private fun buildMcCheyneStarter(): ReadingPlanDto {
        val days = (1..14).map { day ->
            ReadingPlanDayDto(
                dayNumber = day,
                title = "Daily Four-Stream Reading: Day $day",
                reflectionPrompt = "Connect today's Old Testament history, Gospel reading, and Psalm in prayer.",
                readings = listOf(
                    reading("mcheyne_14_day_start", day, 1, "GEN", day),
                    reading("mcheyne_14_day_start", day, 2, "MAT", day),
                    reading("mcheyne_14_day_start", day, 3, "PSA", day),
                    reading("mcheyne_14_day_start", day, 4, "ACT", day),
                ),
            )
        }
        return ReadingPlanDto(
            id = "mcheyne_14_day_start",
            title = "M'Cheyne 14-Day Start",
            subtitle = "Classic four-stream schedule pairing Old Testament, Gospels, Psalms, and Acts.",
            category = "Classic",
            durationDays = 14,
            sourceAttribution = "Inspired by Robert Murray M'Cheyne's 1842 public domain reading calendar",
            accentHex = "#4A6BB0",
            days = days,
        )
    }

    val starterPlans: List<ReadingPlanDto> = listOf(
        ReadingPlanDto(
            id = "peace_in_every_season",
            title = "Peace in Every Season",
            subtitle = "Calm anxious thoughts through Psalms, Isaiah, the Gospels, and Paul's letters.",
            category = "Theme",
            durationDays = 7,
            sourceAttribution = "Curated BibleBible thematic study",
            accentHex = "#2E6F73",
            days = listOf(
                ReadingPlanDayDto(1, "The Shepherd Who Leads Beside Still Waters", "Read slowly and thank God for one area where He is guiding you right now.", listOf(reading("peace_in_every_season", 1, 1, "PSA", 23))),
                ReadingPlanDayDto(2, "Guard Your Heart With Peace", "Turn one lingering worry into a simple, honest prayer of thanksgiving.", listOf(reading("peace_in_every_season", 2, 1, "PHP", 4))),
                ReadingPlanDayDto(3, "Perfect Peace for the Steadfast Mind", "Recall Isaiah 26:3 when your attention starts racing today.", listOf(reading("peace_in_every_season", 3, 1, "ISA", 26))),
                ReadingPlanDayDto(4, "My Peace I Give Unto You", "Notice the contrast Jesus draws between the world's peace and His own.", listOf(reading("peace_in_every_season", 4, 1, "JHN", 14))),
                ReadingPlanDayDto(5, "Seek First the Kingdom, Release Tomorrow", "Release tomorrow's checklist to God's care before closing your day.", listOf(reading("peace_in_every_season", 5, 1, "MAT", 6))),
                ReadingPlanDayDto(6, "Be Still and Know That I Am God", "Take thirty quiet seconds to pause after verse 10.", listOf(reading("peace_in_every_season", 6, 1, "PSA", 46))),
                ReadingPlanDayDto(7, "Cast All Your Care Upon Him", "Carry the peace of this week forward by encouraging someone else today.", listOf(reading("peace_in_every_season", 7, 1, "1PE", 5))),
            ),
        ),
        buildGospels30(),
        buildProverbs31(),
        ReadingPlanDto(
            id = "psalms_of_comfort",
            title = "Psalms of Comfort & Strength",
            subtitle = "Seven anchored psalms for hard days, sleepless nights, and renewed hope.",
            category = "Devotional",
            durationDays = 7,
            sourceAttribution = "Curated BibleBible devotional",
            accentHex = "#3E8E89",
            days = listOf(
                ReadingPlanDayDto(1, "The Lord Is My Light", "Ask God for steady courage in whatever feels uncertain.", listOf(reading("psalms_of_comfort", 1, 1, "PSA", 27))),
                ReadingPlanDayDto(2, "Near to the Brokenhearted", "Bring any grief or disappointment openly before the Lord.", listOf(reading("psalms_of_comfort", 2, 1, "PSA", 34))),
                ReadingPlanDayDto(3, "Create in Me a Clean Heart", "Receive God's mercy and begin today with a clean slate.", listOf(reading("psalms_of_comfort", 3, 1, "PSA", 51))),
                ReadingPlanDayDto(4, "Under the Shadow of the Almighty", "Rest in God's protection as you read verses 1 and 2.", listOf(reading("psalms_of_comfort", 4, 1, "PSA", 91))),
                ReadingPlanDayDto(5, "Bless the Lord, O My Soul", "List three mercies you have seen in your life this month.", listOf(reading("psalms_of_comfort", 5, 1, "PSA", 103))),
                ReadingPlanDayDto(6, "My Help Cometh From the Lord", "Look up from your routine and remember who watches over you.", listOf(reading("psalms_of_comfort", 6, 1, "PSA", 121))),
                ReadingPlanDayDto(7, "Searched and Known by God", "Thank God that He knows you completely and walks with you.", listOf(reading("psalms_of_comfort", 7, 1, "PSA", 139))),
            ),
        ),
        ReadingPlanDto(
            id = "foundations_of_faith",
            title = "Foundations of Faith",
            subtitle = "Ten essential chapters tracing creation, grace, faith, and new life in Christ.",
            category = "Study",
            durationDays = 10,
            sourceAttribution = "Curated BibleBible study plan",
            accentHex = "#458C6E",
            days = listOf(
                ReadingPlanDayDto(1, "In the Beginning God", "Reflect on God's intentional goodness in creation.", listOf(reading("foundations_of_faith", 1, 1, "GEN", 1))),
                ReadingPlanDayDto(2, "The Word Was With God", "See how John echoes Genesis to introduce Jesus.", listOf(reading("foundations_of_faith", 2, 1, "JHN", 1))),
                ReadingPlanDayDto(3, "Born Again Through Faith", "Pause at John 3:16 and reflect on God's love.", listOf(reading("foundations_of_faith", 3, 1, "JHN", 3))),
                ReadingPlanDayDto(4, "No Condemnation in Christ", "Read Romans 8 slowly as a promise of security.", listOf(reading("foundations_of_faith", 4, 1, "ROM", 8))),
                ReadingPlanDayDto(5, "A Living Sacrifice", "Consider one practical way to renew your mind this week.", listOf(reading("foundations_of_faith", 5, 1, "ROM", 12))),
                ReadingPlanDayDto(6, "The Greatest of These Is Love", "Read 1 Corinthians 13 with a specific relationship in mind.", listOf(reading("foundations_of_faith", 6, 1, "1CO", 13))),
                ReadingPlanDayDto(7, "Fruit of the Spirit", "Ask the Spirit to grow one fruit in you today.", listOf(reading("foundations_of_faith", 7, 1, "GAL", 5))),
                ReadingPlanDayDto(8, "Saved by Grace", "Remember that salvation is God's gift, not earned by effort.", listOf(reading("foundations_of_faith", 8, 1, "EPH", 2))),
                ReadingPlanDayDto(9, "Heroes of Faith", "Draw encouragement from how ordinary people trusted God.", listOf(reading("foundations_of_faith", 9, 1, "HEB", 11))),
                ReadingPlanDayDto(10, "Faith Working Through Action", "Choose one small act of kindness or wisdom to live out today.", listOf(reading("foundations_of_faith", 10, 1, "JAS", 1))),
            ),
        ),
        buildMcCheyneStarter(),
    )
}
