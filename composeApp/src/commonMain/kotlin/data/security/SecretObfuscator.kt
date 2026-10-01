package data.security

object SecretObfuscator {
    private const val MASK: Int = 0x5A

    /**
     * Deobfuscates a string that was XOR-masked during build time with MASK (0x5A).
     * Prevents static string scanners (JADX, mobsf, strings CLI) from finding plain text API keys in compiled binaries.
     */
    fun deobfuscate(obfuscated: String): String {
        if (obfuscated.isEmpty()) return ""
        return obfuscated.map { char ->
            (char.code xor MASK).toChar()
        }.joinToString("")
    }

    /**
     * Obfuscates a string with XOR mask (0x5A).
     */
    fun obfuscate(plain: String): String {
        if (plain.isEmpty()) return ""
        return plain.map { char ->
            (char.code xor MASK).toChar()
        }.joinToString("")
    }
}
