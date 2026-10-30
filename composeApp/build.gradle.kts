
import java.util.Properties
import com.codingfeline.buildkonfig.compiler.FieldSpec.Type.STRING
import com.codingfeline.buildkonfig.compiler.FieldSpec.Type.BOOLEAN

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKmpLibrary)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.kotlinComposeCompiler)
    alias(libs.plugins.buildKonfig)
    alias(libs.plugins.kotlinX.serialization.plugin)
    alias(libs.plugins.sqlDelight)
}

val localAppProperties = Properties().apply {
    rootProject.file("local.properties").takeIf { it.isFile }?.inputStream()?.use { load(it) }
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }
    jvmToolchain(21)
    android {
        namespace = "email.kevinphillips.biblebible"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
        androidResources.enable = true
        compilerOptions.jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        withHostTestBuilder { }.configure {
            isIncludeAndroidResources = true
        }
        withDeviceTestBuilder {
            sourceSetTreeName = "test"
        }.configure {
            instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }
    }

    jvm("desktop") {
        compilerOptions.jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    sourceSets {
        val desktopMain = getByName("desktopMain")

        androidMain.dependencies {
            implementation(libs.compose.ui.tooling.preview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.sqlDelight.driver.android)
        }
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material)
            implementation(libs.compose.material.icons)
            implementation(libs.compose.ui.core)
            implementation(libs.compose.resources)
            implementation(libs.kotlinX.coroutines)
            implementation(libs.kotlinX.datetime)
            implementation(libs.ktor.core)
            implementation(libs.ktor.logging)
            implementation(libs.ktor.resources)
            implementation(libs.ktor.contentNegotiation)
            implementation(libs.ktor.json)
            implementation(libs.kotlinX.serialization)
            implementation(libs.napier)
            implementation(libs.androidx.lifecycle.runtime.compose)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
            implementation(libs.sqlDelight.driver.native)
        }
        desktopMain.dependencies {
            implementation(libs.sqlDelight.driver.desktop)
            implementation(compose.desktop.currentOs)
            implementation(libs.ktor.client.cio)
            implementation(libs.kotlinX.coroutines.swing)
        }
        getByName("androidHostTest") {
            kotlin.srcDir("src/test/kotlin")
            resources.srcDir("src/test/resources")
            dependencies {
                implementation(libs.robolectric)
                implementation(libs.compose.ui.test.junit4)
            }
        }
        getByName("androidDeviceTest") {
            kotlin.srcDirs("src/androidTest/kotlin", "src/androidInstrumentedTest/kotlin", "src/androidDebug/kotlin")
            dependencies {
                implementation(libs.androidx.test.runner)
                implementation(libs.androidx.test.junit)
                implementation(libs.androidx.espresso.core)
                implementation(libs.compose.ui.test.junit4)
            }
        }
        commonTest {
            dependencies {
                implementation(libs.kotlin.test)
                implementation(libs.kotlinX.coroutines.test)
                implementation(libs.ktor.mock)
                implementation(libs.compose.ui.test)
            }
        }
    }
}


val aiSummariesEnabled = providers.gradleProperty("AI_SUMMARIES_ENABLED")
    .orElse(localAppProperties.getProperty("AI_SUMMARIES_ENABLED") ?: "true")
    .get().toBooleanStrict()
val diagnosticLoggingEnabled = providers.gradleProperty("ENABLE_DIAGNOSTIC_LOGGING")
    .orElse(localAppProperties.getProperty("ENABLE_DIAGNOSTIC_LOGGING") ?: "false")
    .get().toBooleanStrict()

fun obfuscateSecret(plain: String): String {
    val mask = 0x5A
    return plain.map { (it.code xor mask).toChar() }.joinToString("")
}

val rawIqKey = localAppProperties.getProperty("IQ_BIBLE_API_KEY") ?: ""
val rawApiBibleKey = localAppProperties.getProperty("api_key_api_bible") ?: ""
val rawGeminiKey = if (aiSummariesEnabled) localAppProperties.getProperty("GEMINI_API_KEY") ?: "" else ""

buildkonfig {
    packageName = "email.kevinphillips.biblebible"

    defaultConfigs {
        buildConfigField(
            STRING,
            "API_KEY",
            obfuscateSecret(rawIqKey)
        )
        buildConfigField(
            STRING,
            "API_KEY_API_BIBLE",
            obfuscateSecret(rawApiBibleKey)
        )
        buildConfigField(
            STRING,
            "GEMINI_API_KEY",
            obfuscateSecret(rawGeminiKey)
        )
        buildConfigField(BOOLEAN, "AI_SUMMARIES_ENABLED", aiSummariesEnabled.toString())
        buildConfigField(BOOLEAN, "ENABLE_DIAGNOSTIC_LOGGING", diagnosticLoggingEnabled.toString())
    }
}

sqldelight {
    databases {
        create("BibleBibleDatabase") {
            packageName.set("email.kevinphillips.biblebible.db")
            // Schema versions are derived from migration filenames: 6.sqm produces schema 7.
        }
    }
}

// CI can select an available simulator by UDID; local builds keep Kotlin's default.
tasks.withType<org.jetbrains.kotlin.gradle.targets.native.tasks.KotlinNativeSimulatorTest>().configureEach {
    providers.gradleProperty("iosSimulatorDevice").orNull?.let { device.set(it) }
}

tasks.withType<org.gradle.api.tasks.testing.Test>().configureEach {
    // Exercise session writes against a test-owned database, never the user's DB.
    systemProperty("biblebible.database.path", temporaryDir.resolve("reading-session.db").absolutePath)
}
