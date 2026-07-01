package fake

import data.apiBible.BookData
import data.apiBible.BibleAPIBook
import data.apiBible.ChapterContent
import data.apiBible.VerseData
import data.bibleIQ.BibleChapterUIState

object TestData {
    val testBooks = listOf(
        BookData(
            id = "gen",
            bibleId = "de4e12af7f28f599-02",
            abbreviation = "Gen",
            name = "Genesis",
            nameLong = "The First Book of Moses, Called Genesis",
            chapters = listOf(
                data.apiBible.Chapter(
                    name = "gen.1",
                    bibleId = "de4e12af7f28f599-02",
                    bookId = "gen",
                    number = "1"
                ),
                data.apiBible.Chapter(
                    name = "gen.2",
                    bibleId = "de4e12af7f28f599-02",
                    bookId = "gen",
                    number = "2"
                )
            )
        ),
        BookData(
            id = "exo",
            bibleId = "de4e12af7f28f599-02",
            abbreviation = "Exo",
            name = "Exodus",
            nameLong = "The Second Book of Moses, Called Exodus",
            chapters = listOf(
                data.apiBible.Chapter(
                    name = "exo.1",
                    bibleId = "de4e12af7f28f599-02",
                    bookId = "exo",
                    number = "1"
                )
            )
        ),
        BookData(
            id = "mat",
            bibleId = "de4e12af7f28f599-02",
            abbreviation = "Mat",
            name = "Matthew",
            nameLong = "The Gospel According to Saint Matthew",
            chapters = listOf(
                data.apiBible.Chapter(
                    name = "mat.1",
                    bibleId = "de4e12af7f28f599-02",
                    bookId = "mat",
                    number = "1"
                )
            )
        )
    )

    val testBibleAPIBook = BibleAPIBook(
        data = testBooks
    )

    val testChapterContent = ChapterContent(
        id = "gen.1",
        bibleId = "de4e12af7f28f599-02",
        number = "1",
        bookId = "gen",
        content = "In the beginning God created the heaven and the earth.",
        verseCount = 1,
        copyright = "Copyright",
        next = data.apiBible.NextData(
            id = "gen.2",
            number = "2",
            bookId = "gen"
        ),
        previous = null,
        verses = listOf(
            VerseData(
                id = "gen.1.1",
                orgId = "gen.1.1",
                bibleId = "de4e12af7f28f599-02",
                bookId = "gen",
                chapterId = "gen.1",
                text = "In the beginning God created the heaven and the earth.",
                verseNumber = "1",
                chapterNumber = "1",
                studyBibleId = null,
                keyId = "gen.1.1",
                next = data.apiBible.NextData(
                    id = "gen.1.2",
                    number = "2",
                    bookId = "gen"
                ),
                previous = null,
                content = "In the beginning God created the heaven and the earth."
            )
        )
    )

    val testBibleChapterUIState = BibleChapterUIState(
        bookId = "gen",
        chapterList = listOf(
            data.bibleIQ.ChapterData(
                id = "gen.1",
                number = "1",
                bookId = "gen"
            ),
            data.bibleIQ.ChapterData(
                id = "gen.2",
                number = "2",
                bookId = "gen"
            )
        )
    )
}
