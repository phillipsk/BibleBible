package data.notifications

import data.config.CloudSessionManager
import data.createHttpClient
import data.AppHttpClientConfig
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PushRegistrationRepositoryTest {

    @AfterTest
    fun tearDown() {
        PushRegistrationRepository.resetForTesting()
        CloudSessionManager.resetForTesting()
    }

    @Test
    fun registerDeviceTokenSendsTimezoneTopicsAndDeviceHeaders() = runTest {
        CloudSessionManager.overrideGatewayUrlForTesting("https://gateway.example.test")
        var capturedPath = ""
        var capturedPlatformHeader = ""
        var capturedDeviceHeader = ""
        var capturedBody = ""

        val engine = MockEngine { request ->
            capturedPath = request.url.encodedPath
            capturedPlatformHeader = request.headers["X-Client-Platform"].orEmpty()
            capturedDeviceHeader = request.headers["X-Client-Device-Id"].orEmpty()
            capturedBody = (request.body as? TextContent)?.text.orEmpty()
            respond(
                content = """{"registered":true}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val client = createHttpClient(
            AppHttpClientConfig(baseUrl = "gateway.example.test"),
            engine = engine,
            enableLogging = false,
        )

        val ok = PushRegistrationRepository.registerDeviceToken(
            token = "fcm_token_abcdef_123456",
            platform = "android",
            isDebugBuild = true,
            timezone = "America/New_York",
            reminderHour = 6,
            reminderMinute = 0,
            remindersEnabled = true,
            versionId = "bsb",
            deviceId = "android-test-device-1",
            client = client,
        )

        assertTrue(ok)
        assertEquals("/v1/notifications/register", capturedPath)
        assertEquals("android", capturedPlatformHeader)
        assertEquals("android-test-device-1", capturedDeviceHeader)

        val json = Json.parseToJsonElement(capturedBody).jsonObject
        assertEquals("fcm_token_abcdef_123456", json["token"]?.jsonPrimitive?.content)
        assertEquals("debug", json["appBuild"]?.jsonPrimitive?.content)
        assertEquals("BSB", json["versionId"]?.jsonPrimitive?.content)
        assertEquals("America/New_York", json["timezone"]?.jsonPrimitive?.content)
        assertEquals(6, json["reminderHour"]?.jsonPrimitive?.intOrNull)
        assertEquals(0, json["reminderMinute"]?.jsonPrimitive?.intOrNull)
        assertEquals(true, json["remindersEnabled"]?.jsonPrimitive?.booleanOrNull)
        val topics = json["topics"]?.jsonArray?.map { it.jsonPrimitive.content }.orEmpty()
        assertTrue("votd_daily" in topics)
        assertTrue("reading_engagement" in topics)
        assertTrue("biblical_words" in topics)
        assertTrue("announcements" in topics)
        assertTrue("debug_test" in topics)
    }

    @Test
    fun releaseDefaultTopicsExcludeDebugTestAndUnregisterPostsToken() = runTest {
        assertFalse("debug_test" in PushRegistrationRepository.defaultTopics(isDebugBuild = false))
        CloudSessionManager.overrideGatewayUrlForTesting("https://gateway.example.test")

        var capturedPath = ""
        val engine = MockEngine { request ->
            capturedPath = request.url.encodedPath
            respond(
                content = """{"unregistered":true}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val client = createHttpClient(
            AppHttpClientConfig(baseUrl = "gateway.example.test"),
            engine = engine,
            enableLogging = false,
        )

        val ok = PushRegistrationRepository.unregisterDeviceToken(
            token = "apns_fcm_token_12345678",
            platform = "ios",
            deviceId = "ios-test-device-1",
            client = client,
        )
        assertTrue(ok)
        assertEquals("/v1/notifications/unregister", capturedPath)
    }

    @Test
    fun registerDeviceTokenDebouncesIdenticalCallsAndAllowsUpdates() = runTest {
        CloudSessionManager.overrideGatewayUrlForTesting("https://gateway.example.test")
        var fakeNow = 1_000_000L
        PushRegistrationRepository.nowProvider = { fakeNow }
        PushRegistrationRepository.dedupeWindowMs = 300_000L

        var postCount = 0
        val engine = MockEngine {
            postCount += 1
            respond(
                content = """{"registered":true}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val client = createHttpClient(
            AppHttpClientConfig(baseUrl = "gateway.example.test"),
            engine = engine,
            enableLogging = false,
        )

        // Repeated identical registrations within the dedupe window execute only once.
        assertTrue(
            PushRegistrationRepository.registerDeviceToken(
                token = "ios_token_repeat_1234",
                platform = "ios",
                isDebugBuild = true,
                timezone = "America/New_York",
                reminderHour = 6,
                reminderMinute = 0,
                remindersEnabled = true,
                versionId = "KJV",
                deviceId = "ios-device-dedupe",
                client = client,
            )
        )
        assertTrue(
            PushRegistrationRepository.registerDeviceToken(
                token = "ios_token_repeat_1234",
                platform = "ios",
                isDebugBuild = true,
                timezone = "America/New_York",
                reminderHour = 6,
                reminderMinute = 0,
                remindersEnabled = true,
                versionId = "KJV",
                deviceId = "ios-device-dedupe",
                client = client,
            )
        )
        assertEquals(1, postCount)

        // Changing reminder time bypasses dedupe immediately.
        assertTrue(
            PushRegistrationRepository.registerDeviceToken(
                token = "ios_token_repeat_1234",
                platform = "ios",
                isDebugBuild = true,
                timezone = "America/New_York",
                reminderHour = 7,
                reminderMinute = 30,
                remindersEnabled = true,
                versionId = "KJV",
                deviceId = "ios-device-dedupe",
                client = client,
            )
        )
        assertEquals(2, postCount)

        // Unregistering clears dedupe cache so re-enabling sends immediately.
        assertTrue(
            PushRegistrationRepository.unregisterDeviceToken(
                token = "ios_token_repeat_1234",
                platform = "ios",
                deviceId = "ios-device-dedupe",
                client = client,
            )
        )
        assertTrue(
            PushRegistrationRepository.registerDeviceToken(
                token = "ios_token_repeat_1234",
                platform = "ios",
                isDebugBuild = true,
                timezone = "America/New_York",
                reminderHour = 7,
                reminderMinute = 30,
                remindersEnabled = true,
                versionId = "KJV",
                deviceId = "ios-device-dedupe",
                client = client,
            )
        )
        assertEquals(4, postCount)
    }

    @Test
    fun notificationDeliveryPolicyPreventsLocalDailyDuplicateWhenRemoteTokenExists() {
        assertTrue(
            NotificationDeliveryPolicy.shouldScheduleLocalDailyVotd(
                remindersEnabled = true,
                hasRemotePushToken = false,
            )
        )
        assertFalse(
            NotificationDeliveryPolicy.shouldScheduleLocalDailyVotd(
                remindersEnabled = true,
                hasRemotePushToken = true,
            )
        )
        assertFalse(
            NotificationDeliveryPolicy.shouldScheduleLocalDailyVotd(
                remindersEnabled = false,
                hasRemotePushToken = false,
            )
        )
    }
}
