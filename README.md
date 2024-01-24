# BibleBible
This is a Bible reading compose multiplatform app that demonstrates the power of Kotlin Multiplatform along with a shared Jetpack Compose UI.

Contributions by Kevin Phillips

## About the App
I am currently prioritizing the development of a minimal viable product (MVP) for this app. This means that the app's core functionality is the primary focus, and I will be adding additional features over time.

To run the app, you will need to provide your own API key from [Bible API](https://scripture.api.bible/). Once you have your API key, add the key to the file `local.properties` in the root directory of the project:
```
# local.properties
api_key_api_bible=YOUR_API_KEY_HERE
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
