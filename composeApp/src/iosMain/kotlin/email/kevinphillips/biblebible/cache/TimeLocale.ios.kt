package email.kevinphillips.biblebible.cache

import kotlinx.datetime.TimeZone
import platform.Foundation.NSTimeZone
import platform.Foundation.localTimeZone

actual fun getUserTimeZone(): TimeZone {
    val timeZoneId = NSTimeZone.localTimeZone.name
    return TimeZone.of(timeZoneId)
}