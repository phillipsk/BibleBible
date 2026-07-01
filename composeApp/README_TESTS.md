# BibleBible UI Tests

This document provides instructions for running UI tests in the BibleBible project.

## Test Structure

The UI tests are organized as follows:

- `src/commonTest/kotlin/ui/` - Common UI tests that run on all platforms
- `src/androidTest/kotlin/` - Android-specific instrumentation tests
- `src/commonTest/kotlin/fake/` - Fake data for testing

## Running Tests

### Android Instrumented Tests

To run Android instrumented tests, you need an Android emulator or connected device:

```bash
# Run all Android instrumented tests
./gradlew :composeApp:connectedAndroidTest

# Run a specific test class
./gradlew :composeApp:connectedAndroidTest --tests "ui.ExampleUiTest"

# Run tests with specific device
./gradlew :composeApp:connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=ui.ExampleUiTest
```

### iOS Simulator Tests (macOS only)

To run iOS simulator tests:

```bash
# Run iOS simulator tests
./gradlew :composeApp:iosSimulatorArm64Test

# Run with specific simulator
./gradlew :composeApp:iosSimulatorArm64Test -Pios.simulator.name="iPhone 15"
```

### Desktop Tests

To run desktop tests:

```bash
# Run desktop tests
./gradlew :composeApp:desktopTest
```

## Test Data

All tests use test data from `fake.TestData` to avoid network calls during testing. The test data includes:

- Sample Bible books (Genesis, Exodus, Matthew)
- Sample chapter content
- Sample verse data

## Test Tags

The UI components use test tags for stable test selectors:

- `TestTags.BookList` - Book list container
- `TestTags.BookItem(id)` - Individual book items
- `TestTags.ChapterList` - Chapter list container
- `TestTags.ChapterItem(id)` - Individual chapter items
- `TestTags.VerseText` - Verse text content
- `TestTags.BibleHomeScreen` - Main home screen
- `TestTags.BibleScripturesPager` - Scriptures pager

## Writing Tests

When writing new UI tests:

1. Use `createComposeRule()` for Android tests
2. Use `runComposeUiTest` for common tests
3. Use test tags for stable selectors
4. Use fake data instead of real API calls
5. Test user interactions and state changes

Example test structure:

```kotlin
@RunWith(AndroidJUnit4::class)
class MyUiTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testMyComponent() {
        composeTestRule.setContent {
            MyComponent()
        }

        composeTestRule.onNodeWithTag(TestTags.MyComponent).assertIsDisplayed()
    }
}
```

## Troubleshooting

### Common Issues

1. **Emulator not running**: Make sure an Android emulator is running before executing `connectedAndroidTest`
2. **iOS Simulator not available**: Ensure Xcode is installed and iOS Simulator is available
3. **Test failures**: Check that test tags are properly added to UI components
4. **Dependency issues**: Run `./gradlew clean` and rebuild if you encounter dependency conflicts

### Debugging Tests

To debug tests, you can:

1. Add `@get:Rule val composeTestRule = createComposeRule()` with debug options
2. Use `composeTestRule.onRoot().printToLog("TEST")` to print the component tree
3. Add breakpoints in test methods
4. Use `composeTestRule.waitForIdle()` to wait for animations to complete
