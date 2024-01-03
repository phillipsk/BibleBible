package ui

import data.bibleIQ.BibleBook
import data.bibleIQ.BibleVersion

data class HomeUiState(var version: BibleVersion? = null, val book: BibleBook? = null)
