
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
            implementation(libs.androidx.media3.exoplayer)
            implementation(libs.play.integrity)
            implementation(libs.mlkit.genai.prompt)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.sqlDelight.driver.android)
            implementation(project.dependencies.platform(libs.firebase.bom))
            implementation(libs.firebase.messaging)
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
val cloudGatewayUrl = providers.gradleProperty("CLOUD_GATEWAY_URL")
    .orElse(localAppProperties.getProperty("CLOUD_GATEWAY_URL") ?: "https://biblebible-api-5jdy3d7zua-uc.a.run.app")
    .get()
    .trim()

composeCompiler {
    includeSourceInformation.set(diagnosticLoggingEnabled)
}

val protectedEndpointsDir = layout.buildDirectory.dir("generated/protectedEndpoints/commonMain/kotlin")
val generateProtectedEndpoints = tasks.register("generateProtectedEndpoints") {
    val gatewayInput = cloudGatewayUrl
    val outputDir = protectedEndpointsDir
    inputs.property("gatewayUrl", gatewayInput)
    inputs.property("schemaVersion", 4)
    outputs.dir(outputDir)
    doLast {
        val outFile = outputDir.get()
            .file("email/kevinphillips/biblebible/security/ProtectedEndpoints.kt")
            .asFile
        outFile.parentFile.mkdirs()
        val seed = intArrayOf(0x5A, 0x3C, 0x6F, 0x19, 0x71, 0x2D, 0x4E, 0x63)
        fun encodeLiteral(raw: String, salt: Int): String {
            val bytes = raw.encodeToByteArray()
            return bytes.indices.joinToString(", ") { i ->
                val b = bytes[i].toInt() and 0xFF
                val mask = (seed[i % seed.size] xor ((i * 31 + salt) and 0xFF)) and 0xFF
                (b xor mask).toString()
            }
        }
        val entries = listOf(
            "gatewayUrl" to gatewayInput,
            "configPath" to "/v1/config",
            "authChallengePath" to "/v1/auth/challenge",
            "authTokenPath" to "/v1/auth/token",
            "geminiSummaryPath" to "/v1/gemini/summary",
            "analyticsEventsPath" to "/v1/analytics/events",
            "shortsFeedPath" to "/GetBiblicalShorts",
            "shortsMediaPrefix" to "/v1/shorts/media/",
            "versionsPath" to "/GetVersions",
            "chapterPath" to "/GetChapter",
            "chapterCountPath" to "/GetChapterCount",
            "dailyVersePath" to "/GetVerseOfTheDay",
            "votdPreviewPath" to "/v1/notifications/preview",
            "pushRegisterPath" to "/v1/notifications/register",
            "pushUnregisterPath" to "/v1/notifications/unregister",
            "searchPath" to "/v1/search",
            "plansPath" to "/v1/plans",
            "headerClientPlatform" to "X-Client-Platform",
            "headerClientDeviceId" to "X-Client-Device-Id",
            "headerClientIntegrity" to "X-Client-Integrity",
        )
        val functions = entries.mapIndexed { idx, (fnName, value) ->
            val salt = (idx + 1) * 17
            "    internal fun $fnName(): String = d(intArrayOf(${encodeLiteral(value, salt)}), $salt)"
        }.joinToString("\n")
        outFile.writeText(
            """
            package email.kevinphillips.biblebible.security

            internal object ProtectedEndpoints {
                private val m = intArrayOf(0x5A, 0x3C, 0x6F, 0x19, 0x71, 0x2D, 0x4E, 0x63)

                private fun d(data: IntArray, salt: Int): String {
                    val out = ByteArray(data.size)
                    for (i in data.indices) {
                        val mask = (m[i % m.size] xor ((i * 31 + salt) and 0xFF)) and 0xFF
                        out[i] = (data[i] xor mask).toByte()
                    }
                    return out.decodeToString()
                }

            $functions
            }
            """.trimIndent() + "\n"
        )
    }
}

kotlin.sourceSets.getByName("commonMain").kotlin.srcDir(generateProtectedEndpoints)

buildkonfig {
    packageName = "email.kevinphillips.biblebible"

    defaultConfigs {
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
    val defaultUserHome = file(System.getProperty("user.home"))
    if (!defaultUserHome.canWrite()) {
        val testUserHome = layout.buildDirectory.dir("test-user-home").get().asFile
        systemProperty("user.home", testUserHome.absolutePath)
        doFirst { testUserHome.mkdirs() }
    }
    // Robolectric resolves its Android SDK in the test JVM, so forward the
    // Gradle proxy settings used by managed and corporate build environments.
    listOf("http.proxyHost", "http.proxyPort", "https.proxyHost", "https.proxyPort").forEach { key ->
        System.getProperty(key)?.let { systemProperty(key, it) }
    }
}
