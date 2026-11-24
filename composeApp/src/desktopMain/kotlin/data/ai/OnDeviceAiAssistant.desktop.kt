package data.ai

internal actual object OnDeviceAiAssistant {
    actual suspend fun isAvailable(): Boolean = false
    actual fun providerLabel(): String = "Desktop"
    actual suspend fun extractTopicIntent(
        query: String,
        allowedTopics: List<String>,
    ): OnDeviceTopicIntent? = null

    actual suspend fun generateGroundedReflection(
        reference: String,
        chapterExcerpt: String,
    ): String? = null
}
