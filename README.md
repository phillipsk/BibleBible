# BibleBible
This is a Bible reading compose multiplatform app that demonstrates the power of Kotlin Multiplatform along with a shared Jetpack Compose UI.

Contributions by Kevin Phillips

## Download
[![App Store](previews/app_store_badge.svg)](https://apps.apple.com/us/app/my-biblebible/id6478799350)

<a href='https://play.google.com/store/apps/details?id=email.kevinphillips.biblebible'>
<img src="https://play.google.com/intl/en_gb/badges/static/images/badges/en_badge_web_generic.png" width=240 />
</a>

## Run the App
I am currently prioritizing the development of a minimal viable product (MVP) for this app. This means that the app's core functionality is the primary focus, and I will be adding additional features over time.

To run the app, you will need to provide your own API key from [IQ Bible](https://rapidapi.com/vibrantmiami/api/iq-bible) 
Additionally an API key from [Gemini AI](https://ai.google.dev/) will be needed for the AI summary feature
Once you have your API keys, add them to the file `local.properties` in the root directory of the project:
```
# local.properties
IQ_BIBLE_API_KEY=[YOUR_API_KEY_HERE]
GEMINI_API_KEY=[YOUR_API_KEY_HERE]
```

## Approach
To keep things simple and avoid third-party navigation and state management libraries, I have taken a unique approach. Instead of using a traditional navigation library or Koin DI framework, I manage the app's state with a Kotlin object and mimic navigation using animated visibility composables.

## UI and Performance
While developing this app, I have observed some interesting aspects of Compose multiplatform architecture. The styling, including fonts, margins, and spacing, may render slightly differently on iOS compared to Android. Additionally, I've noticed that the performance of the app, while generally smooth, may exhibit some lag, particularly in development builds.

## Future Development
Despite these considerations, I am excited about the potential of this app. As I continue to work on it, I will address these challenges and strive to provide an excellent Bible reading experience for users on both Android and iOS platforms.

* `/composeApp` is for code that will be shared across your Compose Multiplatform applications.
  It contains several subfolders:
  - `commonMain` is for code that’s common for all targets.
  - Other folders are for Kotlin code that will be compiled for only the platform indicated in the folder name.
    For example, if you want to use Apple’s CoreCrypto for the iOS part of your Kotlin app,
    `iosMain` would be the right folder for such calls.

* `/iosApp` contains iOS applications. Even if you’re sharing your UI with Compose Multiplatform, 
  you need this entry point for your iOS app. This is also where you should add SwiftUI code for your project.


Learn more about [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html)

## Previews
### Android (top) and iOS (bottom)
https://github.com/phillipsk/BibleBible/assets/7585083/a41d07b4-9dd4-447c-a873-49963f12f51d

https://github.com/phillipsk/BibleBible/assets/7585083/af616ab8-ec06-4b2b-a58b-13e7cac5d439


### API Usage Notice
This application uses the IQ Bible API to fetch Bible content. Use of this API is subject to the IQ Bible API's [Terms of Service](https://iqbible.com/). You may not use this API for commercial purposes or create a competing service based on this API data. Ensure compliance with their terms when using or modifying this application.



## Backlog
*   K2, upgrade KMP version, compose common tests
*   review IDE notificaitons
*   ~~socket timeout and api/db retries~~
*   decouple lazy row chapters from the animatedVisibility of the entire view so when the chapter 
*   is not cached and the remote call fails, the lazy row chapters are not visible
*   add a compiler flag to disable GC on iOS release build
*   crashlytics with touchlab CrashKiOS and kermit
*   back handler
*   read JSON files with library
*   remove material library in favor of material3
*   ~~add shimmer effect to scripture load~~
*   ~~merge socket timeout branch, revisit snackbar functionality~~
*   ~~A-z, OT-NT, sorted comparator~~
*   API success state
*   UI TESTS
*   mock errors; add Result type to API calls
*   use immutable state with compose
*   ~~Fix bible book abbreviation on home screen, change sort order~~
*   handle empty list and 200 error response from Rapid API
*   ~~when bibleVersion changes, does a call to Chapter count need to be made~~
*   ~~review duplicate API call on launch effect~~
*   supervisorScope coroutine exception handling 
*   Scroll bar on the right 
*   ~~No light mode only dark mode~~
*   ~~sort Books A-Z, disable selectedBook on homeScreen~~
*   ~~add strikethrough backlog~~
*   ~~open/close database connection pools~~
*   ~~revert AI summary on double tap~~
*   window insets on iOS
*   ~~back arrow on front layer page~~
*   ~~data class for AI Summary UI state~~
*   update README.md with screenshots
*   ~~font libraries added to shared module~~
*   ~~persist user settings~~
*   Select text to copy and paste does not work
*   ~~separate http config clients~~
*   ~~rename Desktop app window title > from BibleBible to My BibleBible~~ 
*   ~~desktop > larger font maximum~~
*   Post on stack overflow remember scroll position bug 
*   Rename app to ‘Zoom in Bible’ (except on Desktop as there is no zoom)
*   Line breaks and color the text that Jesus is speaking 
*   OpenAPI AI replacement refactor 
*   ~~If bible chapter is persisted, scroll to the top is delayed~~

## Nice to have
*   ~~add some UI home screen uniform design~~
*   ~~only need to query database by newly created databaseKey~~
*   ~~obfuscation, R8, Proguard~~
*   log api call stats, analytics
*   refactor database object model
*   review HTML, JSON, Text api queries
*   ~~pinch to zoom scriptures~~
*   ~~increase font size dynamically~~
*   Fill white space in the book of Psalms
*   Multi window debug/ PiP debug
*   ~~Color code parables on BookList composable; add Parables section~~
*   expect/actual for connectivityManager

## Checklist
*   Database retention limit
*   review non null assertion operators
*   review launched effect key
*   review lazy column key
*   review Napier logging for Debug vs. Prod builds
*   internal functions to reduce compile size on iOS from kotlin shared module
*   remove logging and delay() debugging lines
*   database retention
*   review light vs. dark mode
*   cut release branch
*   set functions to internal modifier
*   review logs
