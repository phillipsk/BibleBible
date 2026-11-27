package data.bible

import email.kevinphillips.biblebible.cache.getUserTimeZone
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.datetime.toLocalDateTime

internal fun getTimeZone(): String {
    val currentInstant = Clock.System.now()
    val userTimeZone = getUserTimeZone()
    return currentInstant.toLocalDateTime(userTimeZone).toString()
}

internal fun currentTimeMillis(): Long = Clock.System.now().toEpochMilliseconds()

internal fun currentLocalDateKey(nowMs: Long = currentTimeMillis()): String =
    Instant.fromEpochMilliseconds(nowMs).toLocalDateTime(getUserTimeZone()).date.toString()
