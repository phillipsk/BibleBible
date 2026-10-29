
import com.android.build.gradle.internal.cxx.configure.gradleLocalProperties
import com.codingfeline.buildkonfig.compiler.FieldSpec.Type.STRING
import com.codingfeline.buildkonfig.compiler.FieldSpec.Type.BOOLEAN
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.gradle.api.tasks.testing.Test

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.kotlinComposeCompiler)
    alias(libs.plugins.buildKonfig)
    alias(libs.plugins.kotlinX.serialization.plugin)
    alias(libs.plugins.sqlDelight)
}

val localAppProperties = gradleLocalProperties(rootDir, providers)

kotlin {
    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi::class)
    androidTarget {
        instrumentedTestVariant.sourceSetTree.set(org.jetbrains.kotlin.gradle.plugin.KotlinSourceSetTree.test)
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_1_8)
        }
    }

    jvm("desktop")

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    sourceSets {
        val desktopMain by getting

        androidMain.dependencies {
            implementation(libs.compose.ui.tooling.preview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.sqlDelight.driver.android)
            implementation(libs.generativeai)
        }
        commonMain.dependencies {
            implementation(libs.androidx.lifecycle.runtime.compose)
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(libs.kotlinX.coroutines)
            implementation(libs.kotlinX.datetime)
            implementation(libs.ktor.core)
            implementation(libs.ktor.logging)
            implementation(libs.ktor.resources)
            implementation(libs.ktor.contentNegotiation)
            implementation(libs.ktor.json)
            implementation(libs.ktor.serialization)
            implementation(libs.kotlinX.serialization)
            implementation(libs.napier)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
            implementation(libs.sqlDelight.driver.native)
            implementation(libs.generativeai)
        }
        desktopMain.dependencies {
            implementation(libs.sqlDelight.driver.desktop)
            implementation(compose.desktop.currentOs)
            implementation(libs.ktor.client.cio)
            implementation(libs.kotlinX.coroutines.swing)
        }
        val commonTest by getting {
            dependencies {
                implementation(libs.kotlin.test)
                implementation(libs.kotlinX.coroutines.test)
                implementation(libs.ktor.mock)
                @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
                implementation(compose.uiTest)
            }
        }
        val androidInstrumentedTest by getting {
            dependencies {
                implementation(libs.compose.ui.test.junit4)
                implementation(libs.androidx.test.junit)
                implementation("androidx.test:runner:1.6.2")
            }
        }
    }
}

android {
    namespace = "email.kevinphillips.biblebible"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    sourceSets["debug"].manifest.srcFile("src/androidDebug/AndroidManifest.xml")
    sourceSets["main"].res.srcDirs("src/commonMain/resources", "src/androidMain/res")
    sourceSets["main"].resources.srcDirs("src/commonMain/resources")

    defaultConfig {
        applicationId = "email.kevinphillips.biblebible"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 17
        versionName = "7.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    val releaseStorePath = localAppProperties.getProperty("ANDROID_RELEASE_STORE_FILE")
    if (!releaseStorePath.isNullOrBlank()) {
        signingConfigs {
            create("release") {
                storeFile = rootProject.file(releaseStorePath)
                storePassword = localAppProperties.getProperty("ANDROID_RELEASE_STORE_PASSWORD")
                keyAlias = localAppProperties.getProperty("ANDROID_RELEASE_KEY_ALIAS")
                keyPassword = localAppProperties.getProperty("ANDROID_RELEASE_KEY_PASSWORD")
            }
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfigs.findByName("release")?.let { signingConfig = it }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    dependencies {
        debugImplementation(libs.compose.ui.tooling)
        testImplementation(libs.robolectric)
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

tasks.withType<Test>().configureEach {
    // Desktop UI tests exercise session updates; never write the user's database.
    systemProperty("biblebible.database.path", temporaryDir.resolve("reading-session.db").absolutePath)
}
