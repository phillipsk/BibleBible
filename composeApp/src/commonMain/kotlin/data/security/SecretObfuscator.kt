package data.security

object SecretObfuscator {
    private const val MASK: Int = 0x5A

    /**
     * Deobfuscates a string that was XOR-masked during build time with MASK (0x5A).
     * Avoids literal key strings in generated source. The fixed mask is reversible and is not a security boundary.
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
