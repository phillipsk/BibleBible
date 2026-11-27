package data.ai

import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume
import kotlin.native.concurrent.ThreadLocal

@ThreadLocal
internal object IosOnDeviceAiBridgeRegistry {
    var availabilityCheck: (() -> Boolean)? = null
    var topicExtractor: ((query: String, allowedCsv: String, onResult: (String?) -> Unit) -> Unit)? = null
    var reflectionGenerator: ((reference: String, excerpt: String, onResult: (String?) -> Unit) -> Unit)? = null
}

internal actual object OnDeviceAiAssistant {
    actual suspend fun isAvailable(): Boolean =
        runCatching { IosOnDeviceAiBridgeRegistry.availabilityCheck?.invoke() == true }
            .getOrDefault(false)

    actual fun providerLabel(): String = "Apple Foundation Models"

    actual suspend fun extractTopicIntent(
        query: String,
        allowedTopics: List<String>,
    ): OnDeviceTopicIntent? {
        val extractor = IosOnDeviceAiBridgeRegistry.topicExtractor ?: return null
        val cleanQuery = query.trim()
        if (cleanQuery.isBlank() || allowedTopics.isEmpty() || !isAvailable()) return null
        val allowedMap = allowedTopics.associateBy { it.lowercase() }
        val allowedCsv = allowedTopics.joinToString(",")

        val rawCsv = withTimeoutOrNull(4000L) {
            suspendCancellableCoroutine<String?> { cont ->
                extractor(cleanQuery.take(140), allowedCsv) { result ->
                    if (cont.isActive) cont.resume(result)
                }
            }
        } ?: return null

        val matched = rawCsv
            .split(",", "\n")
            .mapNotNull { token -> allowedMap[token.trim().lowercase()] }
            .distinct()
            .take(4)

        return if (matched.isEmpty()) null else OnDeviceTopicIntent(matched, providerLabel())
    }

    actual suspend fun generateGroundedReflection(
        reference: String,
        chapterExcerpt: String,
    ): String? {
        val generator = IosOnDeviceAiBridgeRegistry.reflectionGenerator ?: return null
        val cleanExcerpt = chapterExcerpt.trim().take(1200)
        if (cleanExcerpt.isBlank() || !isAvailable()) return null

        val raw = withTimeoutOrNull(4500L) {
            suspendCancellableCoroutine<String?> { cont ->
                generator(reference, cleanExcerpt) { result ->
                    if (cont.isActive) cont.resume(result)
                }
            }
        } ?: return null

        return raw
            .replace("\u2014", ", ")
            .replace("--", ", ")
            .trim()
            .takeIf { it.isNotBlank() }
    }
}
