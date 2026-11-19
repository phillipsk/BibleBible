package data.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import data.analytics.AnalyticsTracker
import data.config.CloudSessionManager
import data.httpClientCloudGateway
import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.native.concurrent.ThreadLocal

@ThreadLocal
object SyncRepository {
    private val syncMutex = Mutex()

    var isSyncing: Boolean by mutableStateOf(false)
        private set

    var lastSyncTimestamp: Long? by mutableStateOf(null)
        private set

    fun mergeRecordsLww(
        local: Map<String, SyncRecordDto>,
        remote: Map<String, SyncRecordDto>,
    ): Map<String, SyncRecordDto> {
        val merged = local.toMutableMap()
        for ((key, remoteRec) in remote) {
            val localRec = merged[key]
            if (localRec == null || remoteRec.updatedAt >= localRec.updatedAt) {
                merged[key] = remoteRec
            }
        }
        return merged
    }

    suspend fun syncNow(
        client: HttpClient = httpClientCloudGateway,
        localRecordsProvider: () -> Map<String, SyncRecordDto> = { emptyMap() },
        onApplyRemoteRecords: (Map<String, SyncRecordDto>) -> Unit = {},
    ): Boolean = withContext(Dispatchers.IO) {
        val userToken = CloudSessionManager.userSessionToken?.takeIf { it.isNotBlank() } ?: return@withContext false
        val baseUrl = CloudSessionManager.configuredGatewayUrl.takeIf { it.isNotBlank() } ?: return@withContext false

        syncMutex.withLock {
            isSyncing = true
            try {
                val syncEndpoint = "$baseUrl/v1/me/sync"

                // 1. Fetch remote sync state
                val getResponse = client.get {
                    expectSuccess = false
                    url(syncEndpoint)
                    header(HttpHeaders.Authorization, "Bearer $userToken")
                }

                if (!getResponse.status.isSuccess()) {
                    Napier.w("Sync GET returned HTTP ${getResponse.status.value}", tag = "SyncRepository")
                    return@withLock false
                }

                val remoteSyncDto = getResponse.body<GatewaySyncResponseDto>()
                val localRecords = localRecordsProvider()

                // 2. Perform Last-Write-Wins Merge
                val mergedRecords = mergeRecordsLww(localRecords, remoteSyncDto.records)

                // 3. Push merged/local changes if needed
                val putResponse = client.put {
                    expectSuccess = false
                    url(syncEndpoint)
                    contentType(ContentType.Application.Json)
                    header(HttpHeaders.Authorization, "Bearer $userToken")
                    setBody(GatewaySyncPutRequestDto(records = mergedRecords))
                }

                if (putResponse.status.isSuccess()) {
                    val finalDto = putResponse.body<GatewaySyncResponseDto>()
                    onApplyRemoteRecords(finalDto.records)
                    lastSyncTimestamp = data.apiBible.currentTimeMillis()
                    AnalyticsTracker.track(
                        "auth_sync_completed",
                        "auth",
                        mapOf("keysCount" to finalDto.records.size.toString()),
                    )
                    true
                } else {
                    false
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Napier.w("Sync execution failed: ${e.message}", tag = "SyncRepository")
                false
            } finally {
                isSyncing = false
            }
        }
    }
}
