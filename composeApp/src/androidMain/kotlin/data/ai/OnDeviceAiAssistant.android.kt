package data.ai

import android.os.Build
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.prompt.Generation
import com.google.mlkit.genai.prompt.GenerativeModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

internal actual object OnDeviceAiAssistant {
    @Volatile
    private var cachedAvailability: Boolean? = null

    private fun createClientOrNull(): GenerativeModel? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return null
        return runCatching { Generation.getClient() }.getOrNull()
    }

    actual suspend fun isAvailable(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return false
        cachedAvailability?.let { return it }
        val available = withContext(Dispatchers.Default) {
            runCatching {
                val client = createClientOrNull() ?: return@runCatching false
                try {
                    client.checkStatus() == FeatureStatus.AVAILABLE
                } finally {
                    runCatching { client.close() }
                }
            }.getOrDefault(false)
        }
        cachedAvailability = available
        return available
    }

    actual fun providerLabel(): String = "Android Gemini Nano"

    actual suspend fun extractTopicIntent(
        query: String,
        allowedTopics: List<String>,
    ): OnDeviceTopicIntent? {
        val cleanQuery = query.trim()
        if (cleanQuery.isBlank() || allowedTopics.isEmpty() || !isAvailable()) return null
        val allowedMap = allowedTopics.associateBy { it.lowercase() }
        val prompt = buildString {
            append("Map the user's Bible search phrase to 1 to 4 topic names chosen strictly from this list: ")
            append(allowedTopics.joinToString(", "))
            append(". Return only a comma-separated list of matching topic names from that list. ")
            append("Never output Bible verse quotes or extra prose. User phrase: \"")
            append(cleanQuery.take(140))
            append("\"")
        }

        val rawResponse = withTimeoutOrNull(3500L) {
            withContext(Dispatchers.Default) {
                runCatching {
                    val client = createClientOrNull() ?: return@runCatching null
                    try {
                        if (client.checkStatus() != FeatureStatus.AVAILABLE) return@runCatching null
                        client.generateContent(prompt).candidates.firstOrNull()?.text
                    } finally {
                        runCatching { client.close() }
                    }
                }.getOrNull()
            }
        } ?: return null

        val matched = rawResponse
            .split(",", "\n", ";")
            .mapNotNull { token ->
                val cleaned = token.trim().trim('"', '\'', '.', '[', ']', '-', '*')
                allowedMap[cleaned.lowercase()]
            }
            .distinct()
            .take(4)

        return if (matched.isEmpty()) null else OnDeviceTopicIntent(matched, providerLabel())
    }

    actual suspend fun generateGroundedReflection(
        reference: String,
        chapterExcerpt: String,
    ): String? {
        val cleanExcerpt = chapterExcerpt.trim().take(1200)
        if (cleanExcerpt.isBlank() || !isAvailable()) return null
        val prompt = buildString {
            append("Write one calm, practical reflection sentence grounded strictly in this passage (")
            append(reference)
            append("). Never invent other Bible verses or use em-dashes or double hyphens. Passage: ")
            append(cleanExcerpt)
        }

        val rawResponse = withTimeoutOrNull(4500L) {
            withContext(Dispatchers.Default) {
                runCatching {
                    val client = createClientOrNull() ?: return@runCatching null
                    try {
                        if (client.checkStatus() != FeatureStatus.AVAILABLE) return@runCatching null
                        client.generateContent(prompt).candidates.firstOrNull()?.text
                    } finally {
                        runCatching { client.close() }
                    }
                }.getOrNull()
            }
        } ?: return null

        return rawResponse
            .replace("\u2014", ", ")
            .replace("--", ", ")
            .trim()
            .takeIf { it.isNotBlank() }
    }
}
