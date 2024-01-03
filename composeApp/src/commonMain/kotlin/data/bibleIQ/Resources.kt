package data.bibleIQ

import io.ktor.resources.Resource

//@Serializable
@Resource("/GetVersions")
class GetVersions()
//@Serializable
@Resource("/GetBooks")
class GetBooks(val language: String = "english")

@Resource("/GetChapters")
private class GetChapters()