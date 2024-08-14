
import com.android.build.gradle.internal.cxx.configure.gradleLocalProperties
import com.codingfeline.buildkonfig.compiler.FieldSpec.Type.STRING
import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.buildKonfig)
    alias(libs.plugins.kotlinX.serialization.plugin)
    alias(libs.plugins.sqlDelight)
}

kotlin {
    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "1.8"
            }
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
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(libs.kotlinX.coroutines)
            implementation(libs.kotlinX.datetime)
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
            implementation(compose.desktop.currentOs)
            implementation(libs.ktor.client.cio)
            implementation(libs.kotlinX.coroutines.swing)
        }
    }
}

android {
    namespace = "email.kevinphillips.biblebible"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    sourceSets["main"].res.srcDirs("src/commonMain/resources", "src/androidMain/res")
    sourceSets["main"].resources.srcDirs("src/commonMain/resources")

    defaultConfig {
        applicationId = "email.kevinphillips.biblebible"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 6
        versionName = "6"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
//            isMinifyEnabled = true
//            proguardFiles(
//                getDefaultProguardFile("proguard-android-optimize.txt"),
//                "proguard-rules.pro"
//            )
            signingConfig = signingConfigs.getByName("debug")
//            signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    dependencies {
        debugImplementation(libs.compose.ui.tooling)
    }
}

compose.desktop {
    application {
        mainClass = "MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "email.kevinphillips.biblebible"
            packageVersion = "1.0.0"
        }
    }
}

buildkonfig {
    packageName = "email.kevinphillips.biblebible"

    defaultConfigs {
        buildConfigField(
            STRING,
            "API_KEY",
            gradleLocalProperties(rootDir).getProperty("IQ_BIBLE_API_KEY") ?: ""
        )
        buildConfigField(
            STRING,
            "API_KEY_API_BIBLE",
            gradleLocalProperties(rootDir).getProperty("api_key_api_bible") ?: ""
        )
        buildConfigField(
            STRING,
            "GEMINI_API_KEY",
            gradleLocalProperties(rootDir).getProperty("GEMINI_API_KEY") ?: ""
        )
    }
}

sqldelight {
    databases {
        create("BibleBibleDatabase") {
            packageName.set("email.kevinphillips.biblebible.db")
            version = 3
        }
    }
}