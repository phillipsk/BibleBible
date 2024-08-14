package data.apiBible

import email.kevinphillips.biblebible.cache.getUserTimeZone
import kotlinx.datetime.*

fun getTimeZone(): String {
    val currentInstant = Clock.System.now()
    val userTimeZone = getUserTimeZone()
    return currentInstant.toLocalDateTime(userTimeZone).toString()
}