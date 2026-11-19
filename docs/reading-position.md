# Reading position

The reader saves the canonical book ID, chapter, Bible version, visible text anchor,
and last-active timestamp in a single `SavedReadingPosition` row. The book ID is
independent of the A–Z display order. Font and Bible-version preferences remain in
`UserPreferences`.

On startup, a valid reading session opens the saved chapter without inserting a
duplicate reading-history entry. Rotation, text reflow, and activity recreation
preserve the text anchor provided by the scroll-position change in PR #43.
Selecting another chapter or version starts its anchor at the beginning. Returning
to the book list saves that choice instead of reopening the previous chapter.

A session expires once **24 hours have elapsed** since its last activity/background
save. Resuming after that interval shows the book list. Resuming does not refresh
the timestamp before checking expiry. Future timestamps, removed books/versions,
invalid chapters, and invalid anchors are rejected safely.

The lifecycle observer is shared by Android and iOS through Compose's lifecycle
owner. It captures the latest visible anchor and saves on `ON_STOP`, then checks
expiry on `ON_START`. Saves outlive individual composables and pending scroll
updates are conflated. Database drivers live for the application lifetime so a
background save cannot close a connection used by another repository.

Migration `6.sqm` adds the session table to released schema 6, producing schema 7.
Existing font preferences, cached chapters, and reading history are preserved.
Historical migration files are unchanged.

## Validation

`./gradlew :composeApp:desktopTest` includes database close/reopen and migration
tests, canonical-ID lookup with reordered books, exact expiry-boundary tests,
clock rollback, invalid data, and the common reader reflow regressions. Desktop
tests use a database under their Gradle temporary directory rather than the user's
database. A development desktop instance may override its database with
`-Dbiblebible.database.path=/absolute/path/BibleBibleDB.db`.

On Android and iOS devices, verify the following before release:

1. Open a later chapter, scroll into its middle, background the app, terminate it,
   and reopen it. Check the book, chapter, version, and visible text.
2. Repeat after changing book order to A–Z and after changing the font size.
3. Rotate while reading, return to portrait, then background/reopen the app.
4. Return to the book list before closing and confirm that it stays on the list.
5. Leave the app in the background for 24 hours and confirm that it returns to the
   book list. Check the same behavior after process termination.

Linux cannot run an iOS simulator. Shared Kotlin tests do not substitute for the
UIKit lifecycle and native-driver checks on a macOS simulator or physical iPhone.
