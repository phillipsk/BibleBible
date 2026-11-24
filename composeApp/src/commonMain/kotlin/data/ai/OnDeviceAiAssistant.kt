package data.ai

internal data class OnDeviceTopicIntent(
    val topics: List<String>,
    val provider: String,
)

internal expect object OnDeviceAiAssistant {
    suspend fun isAvailable(): Boolean
    fun providerLabel(): String
    suspend fun extractTopicIntent(
        query: String,
        allowedTopics: List<String>,
    ): OnDeviceTopicIntent?

    suspend fun generateGroundedReflection(
        reference: String,
        chapterExcerpt: String,
    ): String?
}
