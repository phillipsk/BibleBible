package data.security

import data.bible.BibleState
import java.io.File
import java.lang.management.ManagementFactory

internal actual object AppIntegrityProvider {
    actual fun inspectRuntimeEnvironment(): RuntimeResilienceStatus {
        val isUnitTest = !System.getProperty("biblebible.database.path").isNullOrBlank()
        if (!BibleState.RELEASE_BUILD || isUnitTest) {
            return RuntimeResilienceStatus(compromised = false, signals = emptyList())
        }
        val signals = mutableListOf<String>()

        val dyldInsert = System.getenv("DYLD_INSERT_LIBRARIES").orEmpty().trim()
        val ldPreload = System.getenv("LD_PRELOAD").orEmpty().trim()
        if (dyldInsert.isNotEmpty() || ldPreload.isNotEmpty()) {
            signals += "dyld_insert_libraries"
        }

        val jvmArgs = runCatching { ManagementFactory.getRuntimeMXBean().inputArguments }.getOrDefault(emptyList())
        if (jvmArgs.any { arg ->
                arg.contains("-agentlib:jdwp", ignoreCase = true) ||
                    arg.contains("-Xrunjdwp", ignoreCase = true)
            }
        ) {
            signals += "debugger_attached"
        }
        if (jvmArgs.any { arg -> arg.contains("-javaagent:", ignoreCase = true) }) {
            signals += "java_agent_attached"
        }

        val suspiciousFiles = listOf(
            "/usr/lib/frida/frida-agent.dylib",
            "/Library/MobileSubstrate/MobileSubstrate.dylib",
        )
        if (suspiciousFiles.any { path -> runCatching { File(path).exists() }.getOrDefault(false) }) {
            signals += "instrumentation_dylib"
        }

        return RuntimeResilienceStatus(compromised = signals.isNotEmpty(), signals = signals)
    }

    actual suspend fun attestChallenge(nonce: String): ClientAttestationDto? = null
}
