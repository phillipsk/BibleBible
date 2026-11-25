package data.plans

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import data.analytics.AnalyticsTracker
import data.bible.currentTimeMillis
import data.config.CloudSessionManager
import data.httpClientCloudGateway
import email.kevinphillips.biblebible.cache.DriverFactory
import email.kevinphillips.biblebible.db.BibleBibleDatabase
import email.kevinphillips.biblebible.security.ProtectedEndpoints
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.request.get
import io.ktor.client.request.url
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt
import kotlin.native.concurrent.ThreadLocal

@ThreadLocal
internal object ReadingPlanRepository {
    var plans: List<ReadingPlanDto> by mutableStateOf(BundledReadingPlans.starterPlans)
        private set

    var activePlanId: String? by mutableStateOf(null)
        private set

    var completedReadingIds: Set<String> by mutableStateOf(emptySet())
        private set

    private val localLoadMutex = Mutex()
    private val catalogRefreshMutex = Mutex()
    private var localLoadedFromDb = false
    private var remoteCatalogLoaded = false

    var isLoaded: Boolean by mutableStateOf(false)
        private set

    fun activeOrDefaultPlan(): ReadingPlanDto {
        val currentId = activePlanId
        return plans.firstOrNull { it.id == currentId } ?: plans.first()
    }

    fun hasJoinedActivePlan(): Boolean = activePlanId != null

    fun progressFor(plan: ReadingPlanDto): PlanProgressSummary {
        val allReadings = plan.days.flatMap { day -> day.readings.map { day to it } }
        val total = allReadings.size.coerceAtLeast(1)
        val completedInPlan = allReadings.map { it.second.id }.filter { it in completedReadingIds }.toSet()
        val pct = ((completedInPlan.size.toFloat() / total.toFloat()) * 100f).roundToInt().coerceIn(0, 100)
        val nextPair = allReadings.firstOrNull { (_, ref) -> ref.id !in completedReadingIds } ?: allReadings.lastOrNull()
        return PlanProgressSummary(
            plan = plan,
            completedReadingIds = completedInPlan,
            completedCount = completedInPlan.size,
            totalCount = allReadings.size,
            percentComplete = pct,
            nextReadingDay = nextPair?.first,
            nextReading = nextPair?.second,
        )
    }

    fun nextReadingAfter(planId: String, currentReadingId: String): ActivePlanReadingContext? {
        val plan = plans.firstOrNull { it.id == planId } ?: return null
        val flattened = plan.days.flatMap { day -> day.readings.map { day to it } }
        val idx = flattened.indexOfFirst { it.second.id == currentReadingId }
        val nextPair = if (idx >= 0) {
            flattened.drop(idx + 1).firstOrNull { it.second.id !in completedReadingIds }
                ?: flattened.getOrNull(idx + 1)
        } else {
            flattened.firstOrNull { it.second.id !in completedReadingIds }
        } ?: return null
        return ActivePlanReadingContext(
            planId = plan.id,
            planTitle = plan.title,
            dayNumber = nextPair.first.dayNumber,
            dayTitle = nextPair.first.title,
            reading = nextPair.second,
            reflectionPrompt = nextPair.first.reflectionPrompt,
            totalReadingsInPlan = flattened.size,
        )
    }

    fun contextFor(planId: String, day: ReadingPlanDayDto, reading: ReadingPlanVerseRefDto): ActivePlanReadingContext {
        val plan = plans.firstOrNull { it.id == planId } ?: activeOrDefaultPlan()
        val totalReadings = plan.days.sumOf { it.readings.size }
        return ActivePlanReadingContext(
            planId = plan.id,
            planTitle = plan.title,
            dayNumber = day.dayNumber,
            dayTitle = day.title,
            reading = reading,
            reflectionPrompt = day.reflectionPrompt,
            totalReadingsInPlan = totalReadings,
        )
    }

    suspend fun loadLocalState() = localLoadMutex.withLock {
        if (localLoadedFromDb) return@withLock
        try {
            val (loadedActiveId, loadedCompleted) = withContext(Dispatchers.IO) {
                val driver = DriverFactory.createDriver() ?: return@withContext (null to emptySet<String>())
                try {
                    val db = BibleBibleDatabase(driver)
                    val active = db.bibleBibleDatabaseQueries.selectActiveReadingPlan().executeAsOneOrNull()?.plan_id
                    val completed = db.bibleBibleDatabaseQueries
                        .selectAllReadingPlanProgress()
                        .executeAsList()
                        .map { it.reading_id }
                        .toSet()
                    active to completed
                } finally {
                    DriverFactory.closeDB()
                }
            }
            activePlanId = loadedActiveId
            completedReadingIds = loadedCompleted
            localLoadedFromDb = true
        } catch (_: Exception) {
            // Keep safe in-memory state if DB unavailable in unit tests
        } finally {
            isLoaded = true
        }
    }

    suspend fun refreshRemoteCatalog(client: HttpClient = httpClientCloudGateway) = catalogRefreshMutex.withLock {
        if (remoteCatalogLoaded && client === httpClientCloudGateway) return@withLock
        if (!CloudSessionManager.readingPlansEnabled || !CloudSessionManager.isGatewayConfigured) return@withLock
        val baseUrl = CloudSessionManager.configuredGatewayUrl
            .takeIf { CloudSessionManager.parseSecureGatewayUrl(it) != null }
            ?: return@withLock
        val prefix = if ("://" in baseUrl) baseUrl else "https://$baseUrl"
        val endpoint = "$prefix${ProtectedEndpoints.plansPath()}"
        try {
            val response = withContext(Dispatchers.IO) {
                client.get {
                    expectSuccess = false
                    url(endpoint)
                }
            }
            if (!response.status.isSuccess()) return@withLock
            val catalog = response.body<ReadingPlansCatalogResponseDto>()
            if (catalog.plans.isNotEmpty()) {
                plans = catalog.plans
            }
            remoteCatalogLoaded = true
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // Keep bundled starter plans when offline
        }
    }

    suspend fun selectActivePlan(planId: String) {
        activePlanId = planId
        val now = currentTimeMillis()
        AnalyticsTracker.track(
            name = "reading_plan_selected",
            category = "reading_plans",
            properties = mapOf("planId" to planId),
        )
        try {
            withContext(Dispatchers.IO) {
                val driver = DriverFactory.createDriver() ?: return@withContext
                try {
                    val db = BibleBibleDatabase(driver)
                    db.bibleBibleDatabaseQueries.deactivateAllReadingPlans()
                    db.bibleBibleDatabaseQueries.upsertActiveReadingPlan(planId, now)
                } finally {
                    DriverFactory.closeDB()
                }
            }
        } catch (_: Exception) {
        }
    }

    suspend fun markReadingCompleted(
        planId: String,
        dayNumber: Int,
        readingId: String,
        source: String,
    ) {
        if (readingId in completedReadingIds && activePlanId == planId) return
        if (activePlanId == null || activePlanId != planId) {
            activePlanId = planId
        }
        completedReadingIds = completedReadingIds + readingId
        val now = currentTimeMillis()
        AnalyticsTracker.track(
            name = "reading_plan_item_completed",
            category = "reading_plans",
            properties = mapOf(
                "planId" to planId,
                "day" to dayNumber.toString(),
                "source" to source,
            ),
        )
        try {
            withContext(Dispatchers.IO) {
                val driver = DriverFactory.createDriver() ?: return@withContext
                try {
                    val db = BibleBibleDatabase(driver)
                    db.bibleBibleDatabaseQueries.deactivateAllReadingPlans()
                    db.bibleBibleDatabaseQueries.upsertActiveReadingPlan(planId, now)
                    db.bibleBibleDatabaseQueries.insertReadingPlanProgress(
                        reading_id = readingId,
                        plan_id = planId,
                        day_number = dayNumber.toLong(),
                        completed_at = now,
                        source = source,
                    )
                } finally {
                    DriverFactory.closeDB()
                }
            }
        } catch (_: Exception) {
        }
    }

    suspend fun toggleReadingCompletion(
        planId: String,
        dayNumber: Int,
        readingId: String,
    ) {
        if (readingId in completedReadingIds) {
            completedReadingIds = completedReadingIds - readingId
            try {
                withContext(Dispatchers.IO) {
                    val driver = DriverFactory.createDriver() ?: return@withContext
                    try {
                        val db = BibleBibleDatabase(driver)
                        db.bibleBibleDatabaseQueries.deleteReadingPlanProgress(readingId)
                    } finally {
                        DriverFactory.closeDB()
                    }
                }
            } catch (_: Exception) {
            }
        } else {
            markReadingCompleted(
                planId = planId,
                dayNumber = dayNumber,
                readingId = readingId,
                source = "manual_toggle",
            )
        }
    }

    internal fun resetForTesting(
        starter: List<ReadingPlanDto> = BundledReadingPlans.starterPlans,
        activeId: String? = null,
        completedIds: Set<String> = emptySet(),
    ) {
        plans = starter
        activePlanId = activeId
        completedReadingIds = completedIds
        localLoadedFromDb = false
        remoteCatalogLoaded = false
        isLoaded = true
    }
}
