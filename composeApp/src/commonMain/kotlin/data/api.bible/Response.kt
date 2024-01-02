package data.api.bible

import kotlinx.serialization.Serializable

@Serializable
data class Response(val data: List<DataItem>? = null)

@Serializable
data class DataItem(
    val id: String? = null,
    val dblId: String? = null,
    val abbreviation: String? = null,
    val abbreviationLocal: String? = null,
    val language: Language? = null,
    val countries: List<Country>? = null,
    val name: String? = null,
    val nameLocal: String? = null,
    val description: String? = null,
    val descriptionLocal: String? = null,
    val relatedDbl: String? = null,
    val type: String? = null,
    val updatedAt: String? = null,
    val audioBibles: List<AudioBible>? = null
)

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

@Serializable
data class AudioBible(
    val id: String? = null,
    val name: String? = null,
    val nameLocal: String? = null,
    val description: String? = null,
    val descriptionLocal: String? = null
)

