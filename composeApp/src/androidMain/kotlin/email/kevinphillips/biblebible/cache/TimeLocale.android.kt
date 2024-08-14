package email.kevinphillips.biblebible.cache

import kotlinx.datetime.TimeZone
import java.util.TimeZone as JavaTimeZone

actual fun getUserTimeZone(): TimeZone {
    val timeZoneId = JavaTimeZone.getDefault().id
    return TimeZone.of(timeZoneId)
}