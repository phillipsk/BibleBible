import java.util.Properties

plugins {
    alias(libs.plugins.androidApplication)
}

val localAppProperties = Properties().apply {
    rootProject.file("local.properties").takeIf { it.isFile }?.inputStream()?.use { load(it) }
}

android {
    namespace = "email.kevinphillips.biblebible.app"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "email.kevinphillips.biblebible"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 17
        versionName = "7.0"
    }
    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
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
                rootProject.file("composeApp/proguard-rules.pro")
            )
            signingConfigs.findByName("release")?.let { signingConfig = it }
        }
    }
    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    coreLibraryDesugaring(libs.desugar.jdk.libs)
    implementation(project(":composeApp"))
    debugImplementation(libs.compose.ui.tooling)
}
