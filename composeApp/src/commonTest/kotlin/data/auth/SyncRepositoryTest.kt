package data.auth

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class SyncRepositoryTest {

    @Test
    fun testMergeRecordsLwwAppliesNewerUpdates() {
        val local = mapOf(
            "savedPosition" to SyncRecordDto(value = """{"chapter":1}""", updatedAt = 1000L),
            "preferences" to SyncRecordDto(value = """{"fontSize":18}""", updatedAt = 500L),
        )
        val remote = mapOf(
            "savedPosition" to SyncRecordDto(value = """{"chapter":3}""", updatedAt = 2000L),
            "readingHistory" to SyncRecordDto(value = """[]""", updatedAt = 1500L),
        )

        val merged = SyncRepository.mergeRecordsLww(local, remote)

        // savedPosition: remote (2000L) is newer than local (1000L) -> should be 3
        assertEquals("""{"chapter":3}""", merged["savedPosition"]?.value)
        assertEquals(2000L, merged["savedPosition"]?.updatedAt)

        // preferences: local only -> retained
        assertEquals("""{"fontSize":18}""", merged["preferences"]?.value)
        assertEquals(500L, merged["preferences"]?.updatedAt)

        // readingHistory: remote only -> added
        assertNotNull(merged["readingHistory"])
        assertEquals(1500L, merged["readingHistory"]?.updatedAt)
    }

    @Test
    fun testMergeRecordsLwwIgnoresOlderUpdates() {
        val local = mapOf(
            "savedPosition" to SyncRecordDto(value = """{"chapter":5}""", updatedAt = 3000L),
        )
        val remote = mapOf(
            "savedPosition" to SyncRecordDto(value = """{"chapter":2}""", updatedAt = 1000L),
        )

        val merged = SyncRepository.mergeRecordsLww(local, remote)

        assertEquals("""{"chapter":5}""", merged["savedPosition"]?.value)
        assertEquals(3000L, merged["savedPosition"]?.updatedAt)
    }
}
