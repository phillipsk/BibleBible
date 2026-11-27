package data.security

import android.content.pm.ApplicationInfo
import android.os.Debug
import com.google.android.play.core.integrity.IntegrityManagerFactory
import com.google.android.play.core.integrity.IntegrityTokenRequest
import data.bible.BibleState
import email.kevinphillips.biblebible.cache.DriverFactory
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import kotlin.coroutines.resume

internal actual object AppIntegrityProvider {
    private const val CLOUD_PROJECT_NUMBER = 687901748717L
    private const val EXPECTED_RELEASE_PACKAGE = "email.kevinphillips.biblebible"

    actual fun inspectRuntimeEnvironment(): RuntimeResilienceStatus {
        if (!BibleState.RELEASE_BUILD) {
            return RuntimeResilienceStatus(compromised = false, signals = emptyList())
        }
        val signals = mutableListOf<String>()
        val debuggerAttached = runCatching {
            Debug.isDebuggerConnected() || Debug.waitingForDebugger()
        }.getOrDefault(false)
        if (debuggerAttached) {
            signals += "debugger_attached"
        }
        val ctx = runCatching { DriverFactory.context.applicationContext }.getOrNull()
        if (ctx != null) {
            val pkg = ctx.packageName
            if (pkg != EXPECTED_RELEASE_PACKAGE && !pkg.endsWith(".debug")) {
                signals += "unexpected_package"
            }
            val isDebuggable = (ctx.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
            if (isDebuggable && pkg == EXPECTED_RELEASE_PACKAGE) {
                signals += "unexpected_debuggable"
            }
        }
        val hookedLibrary = runCatching {
            val maps = File("/proc/self/maps")
            if (!maps.isFile) {
                false
            } else {
                maps.useLines { lines ->
                    lines.any { line ->
                        line.contains("frida", ignoreCase = true) ||
                            line.contains("xposed", ignoreCase = true)
                    }
                }
            }
        }.getOrDefault(false)
        if (hookedLibrary) {
            signals += "instrumentation_hook"
        }
        return RuntimeResilienceStatus(compromised = signals.isNotEmpty(), signals = signals)
    }

    actual suspend fun attestChallenge(nonce: String): ClientAttestationDto? {
        if (nonce.isBlank()) return null
        if (BibleState.RELEASE_BUILD && inspectRuntimeEnvironment().compromised) {
            return null
        }
        val ctx = runCatching { DriverFactory.context.applicationContext }.getOrNull() ?: return null
        if (ctx.packageName.endsWith(".debug")) {
            return null
        }
        return runCatching {
            suspendCancellableCoroutine<ClientAttestationDto?> { cont ->
                val manager = IntegrityManagerFactory.create(ctx)
                val request = IntegrityTokenRequest.builder()
                    .setNonce(nonce)
                    .setCloudProjectNumber(CLOUD_PROJECT_NUMBER)
                    .build()
                manager.requestIntegrityToken(request)
                    .addOnSuccessListener { response ->
                        if (cont.isActive) {
                            cont.resume(
                                ClientAttestationDto(
                                    platform = "android",
                                    nonce = nonce,
                                    integrityToken = response.token(),
                                )
                            )
                        }
                    }
                    .addOnFailureListener {
                        if (cont.isActive) cont.resume(null)
                    }
            }
        }.getOrNull()
    }
}
