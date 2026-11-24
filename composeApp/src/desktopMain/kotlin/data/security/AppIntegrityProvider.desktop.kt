package data.security

internal actual object AppIntegrityProvider {
    actual fun inspectRuntimeEnvironment(): RuntimeResilienceStatus =
        RuntimeResilienceStatus(compromised = false, signals = emptyList())

    actual suspend fun attestChallenge(nonce: String): ClientAttestationDto? = null
}
