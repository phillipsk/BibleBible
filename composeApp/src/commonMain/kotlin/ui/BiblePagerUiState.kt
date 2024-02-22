package ui

import data.bibleIQ.BibleChapterUIState

sealed interface BiblePagerUiState {
    data class Success(val bibleChapterUIState: BibleChapterUIState) : BiblePagerUiState

    data object Loading : BiblePagerUiState
    data class Error(val message: String) : BiblePagerUiState
}