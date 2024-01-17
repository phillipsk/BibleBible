package data.apiBible

import kotlinx.serialization.Serializable


@Serializable
data class BibleAPIBibles(
    val data: List<BibleAPIVersion>? = null
) {
    @Serializable
    data class BibleAPIVersion(
        val id: String? = null,
        val dblId: String? = null,
        val relatedDbl: String? = null,
        val name: String? = null,
        val nameLocal: String? = null,
        val abbreviation: String? = null,
        val abbreviationLocal: String? = null,
        val description: String? = null,
        val descriptionLocal: String? = null,
        val language: Language? = null,
        val countries: List<Country>? = null,
        val type: String? = null,
        val updatedAt: String? = null,
        val audioBibles: List<AudioBible> = emptyList()
    ) {
        @Serializable
        data class Language(
            val id: String? = null,
            val name: String? = null,
            val nameLocal: String? = null,
            val script: String? = null,
            val scriptDirection: String? = null
        )

        @Serializable
        data class Country(
            val id: String? = null,
            val name: String? = null,
            val nameLocal: String? = null
        )
    }

    @Serializable
    data class AudioBible(
        val id: String? = null,
        val name: String? = null,
        val nameLocal: String? = null,
        val dblId: String? = null
    )
}






