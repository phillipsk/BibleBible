package data.security

import data.bible.BibleState
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.CoreCrypto.CC_SHA256
import platform.CoreCrypto.CC_SHA256_DIGEST_LENGTH
import platform.DeviceCheck.DCAppAttestService
import platform.Foundation.NSBundle
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.NSProcessInfo
import platform.Foundation.NSUserDefaults
import platform.Foundation.base64EncodedStringWithOptions
import platform.Foundation.create
import kotlin.coroutines.resume
import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.Platform

internal actual object AppIntegrityProvider {
    private const val EXPECTED_RELEASE_BUNDLE_ID = "email.kevinphillips.biblebible"
    private const val KEY_ATTEST_KEY_ID = "email.kevinphillips.biblebible.appattest.key_id"
    private const val KEY_ATTEST_REGISTERED = "email.kevinphillips.biblebible.appattest.registered"

    @OptIn(ExperimentalNativeApi::class)
    actual fun inspectRuntimeEnvironment(): RuntimeResilienceStatus {
        if (!BibleState.RELEASE_BUILD || Platform.isDebugBinary) {
            return RuntimeResilienceStatus(compromised = false, signals = emptyList())
        }
        val signals = mutableListOf<String>()
        val bundleId = NSBundle.mainBundle.bundleIdentifier
        if (bundleId != null && bundleId != EXPECTED_RELEASE_BUNDLE_ID && !bundleId.endsWith(".debug")) {
            signals += "unexpected_bundle"
        }
        val dyldInsert = NSProcessInfo.processInfo.environment["DYLD_INSERT_LIBRARIES"]?.toString().orEmpty()
        if (dyldInsert.isNotBlank()) {
            signals += "dyld_insert_libraries"
        }
        val loadedPaths = (NSBundle.allFrameworks + NSBundle.allBundles)
            .mapNotNull { (it as? NSBundle)?.bundlePath }
        val suspiciousMarkers = listOf("FridaGadget", "frida-agent", "MobileSubstrate", "cynject", "libcycript", "ElleKit")
        val fm = NSFileManager.defaultManager
        val suspiciousFiles = listOf(
            "/Library/MobileSubstrate/MobileSubstrate.dylib",
            "/usr/lib/frida/frida-agent.dylib",
        )
        if (loadedPaths.any { path -> suspiciousMarkers.any { marker -> path.contains(marker, ignoreCase = true) } } ||
            suspiciousFiles.any { fm.fileExistsAtPath(it) }
        ) {
            signals += "instrumentation_dylib"
        }
        return RuntimeResilienceStatus(compromised = signals.isNotEmpty(), signals = signals)
    }

    @OptIn(ExperimentalNativeApi::class)
    actual suspend fun attestChallenge(nonce: String): ClientAttestationDto? {
        if (nonce.isBlank()) return null
        if (BibleState.RELEASE_BUILD && !Platform.isDebugBinary && inspectRuntimeEnvironment().compromised) {
            return null
        }
        val bundleId = NSBundle.mainBundle.bundleIdentifier ?: ""
        if (bundleId.endsWith(".debug")) return null

        val service = DCAppAttestService.sharedService
        if (!service.supported) return null

        val defaults = NSUserDefaults.standardUserDefaults
        val storedKeyId = defaults.stringForKey(KEY_ATTEST_KEY_ID)?.takeIf { it.isNotBlank() }
        val isRegistered = storedKeyId != null && defaults.boolForKey(KEY_ATTEST_REGISTERED)

        if (isRegistered && storedKeyId != null) {
            val assertion = generateAssertion(service, storedKeyId, nonce)
            if (assertion != null) return assertion
            defaults.removeObjectForKey(KEY_ATTEST_REGISTERED)
        }

        val keyId = storedKeyId ?: generateKey(service) ?: return null
        defaults.setObject(keyId, forKey = KEY_ATTEST_KEY_ID)

        val attested = attestKey(service, keyId, nonce)
        if (attested != null) {
            defaults.setBool(true, forKey = KEY_ATTEST_REGISTERED)
        }
        return attested
    }

    @OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
    private fun sha256Data(input: ByteArray): NSData {
        val digest = ByteArray(CC_SHA256_DIGEST_LENGTH)
        input.usePinned { inPinned ->
            digest.usePinned { outPinned ->
                CC_SHA256(
                    inPinned.addressOf(0),
                    input.size.toUInt(),
                    outPinned.addressOf(0).reinterpret(),
                )
            }
        }
        return digest.usePinned { outPinned ->
            NSData.create(bytes = outPinned.addressOf(0), length = digest.size.toULong())
        }
    }

    @OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
    private fun ByteArray.toNSData(): NSData =
        if (isEmpty()) {
            NSData()
        } else {
            usePinned { pinned ->
                NSData.create(bytes = pinned.addressOf(0), length = size.toULong())
            }
        }

    private suspend fun generateKey(service: DCAppAttestService): String? =
        suspendCancellableCoroutine { cont ->
            service.generateKeyWithCompletionHandler { keyId, error ->
                if (cont.isActive) {
                    cont.resume(if (error == null && !keyId.isNullOrBlank()) keyId else null)
                }
            }
        }

    private suspend fun attestKey(
        service: DCAppAttestService,
        keyId: String,
        nonce: String,
    ): ClientAttestationDto? {
        val challengeHash = sha256Data(nonce.encodeToByteArray())
        return suspendCancellableCoroutine { cont ->
            service.attestKey(keyId, clientDataHash = challengeHash) { attestationObj, error ->
                if (!cont.isActive) return@attestKey
                if (error == null && attestationObj != null) {
                    cont.resume(
                        ClientAttestationDto(
                            platform = "ios",
                            mode = "attest",
                            nonce = nonce,
                            keyId = keyId,
                            attestationObjectB64 = attestationObj.base64EncodedStringWithOptions(0u),
                        )
                    )
                } else {
                    cont.resume(null)
                }
            }
        }
    }

    private suspend fun generateAssertion(
        service: DCAppAttestService,
        keyId: String,
        nonce: String,
    ): ClientAttestationDto? {
        val clientDataBytes = """{"nonce":"$nonce"}""".encodeToByteArray()
        val clientDataHash = sha256Data(clientDataBytes)
        val clientDataB64 = clientDataBytes.toNSData().base64EncodedStringWithOptions(0u)
        return suspendCancellableCoroutine { cont ->
            service.generateAssertion(keyId, clientDataHash = clientDataHash) { assertionObj, error ->
                if (!cont.isActive) return@generateAssertion
                if (error == null && assertionObj != null) {
                    cont.resume(
                        ClientAttestationDto(
                            platform = "ios",
                            mode = "assertion",
                            nonce = nonce,
                            keyId = keyId,
                            clientDataB64 = clientDataB64,
                            assertionObjectB64 = assertionObj.base64EncodedStringWithOptions(0u),
                        )
                    )
                } else {
                    cont.resume(null)
                }
            }
        }
    }
}
