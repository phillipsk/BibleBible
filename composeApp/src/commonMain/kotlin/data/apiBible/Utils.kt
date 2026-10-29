package data.apiBible

import email.kevinphillips.biblebible.cache.getUserTimeZone
import kotlin.time.Clock
import kotlinx.datetime.toLocalDateTime

fun getTimeZone(): String {
    val currentInstant = Clock.System.now()
    val userTimeZone = getUserTimeZone()
    return currentInstant.toLocalDateTime(userTimeZone).toString()
}