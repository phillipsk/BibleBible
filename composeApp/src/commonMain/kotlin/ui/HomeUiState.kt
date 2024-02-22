package ui

import data.bibleIQ.BibleIQBook
import data.bibleIQ.BibleIQVersion

data class HomeUiState(var version: BibleIQVersion? = null, val book: BibleIQBook? = null)
