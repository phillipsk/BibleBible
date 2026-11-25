import java.util.Properties
import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.kotlinComposeCompiler)
}

val localAppProperties = Properties().apply {
    rootProject.file("local.properties").takeIf { it.isFile }?.inputStream()?.use { load(it) }
}

fun desktopConfigValue(key: String, default: String = ""): String =
    providers.gradleProperty(key)
        .orElse(localAppProperties.getProperty(key) ?: default)
        .get()
        .trim()

val macAppStoreSignEnabled = desktopConfigValue("MAC_APPSTORE_SIGN", "false").equals("true", ignoreCase = true)
val macSignIdentity = desktopConfigValue("MAC_APPSTORE_SIGN_IDENTITY", "Kevin Phillips (7JJ7U4528N)")
val macKeychainPath = desktopConfigValue("MAC_KEYCHAIN_PATH")
val macAppProfilePath = desktopConfigValue("MAC_APP_PROVISIONING_PROFILE")
val macRuntimeProfilePath = desktopConfigValue("MAC_RUNTIME_PROVISIONING_PROFILE")
val desktopJavaHome = desktopConfigValue("DESKTOP_JAVA_HOME").ifEmpty {
    javaToolchains.launcherFor {
        languageVersion.set(JavaLanguageVersion.of(21))
    }.get().metadata.installationPath.asFile.absolutePath
}

kotlin {
    jvmToolchain(21)
    jvm {
        compilerOptions.jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
    sourceSets {
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(project(":composeApp"))
        }
    }
}

compose.desktop {
    application {
        javaHome = desktopJavaHome
        mainClass = "email.kevinphillips.biblebible.BibleBibleKt"
        buildTypes.release.proguard {
            obfuscate.set(true)
            optimize.set(false)
            configurationFiles.from(project.file("proguard-rules.pro"))
        }

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb, TargetFormat.Pkg)
            modules(
                "java.instrument",
                "java.logging",
                "java.management",
                "java.naming",
                "java.prefs",
                "java.sql",
                "jdk.crypto.ec",
                "jdk.unsupported",
            )
            packageName = "BibleBible"
            packageVersion = "3.2.0"
            description = "Daily Bible Reading App"
            copyright = "© 2026 Kevin Phillips"
            vendor = "Kevin Phillips"
            licenseFile.set(project.file("../LICENSE.md"))

            val iconsRoot = project.file("desktop-icons")
            macOS {
                iconFile.set(iconsRoot.resolve("BibleBible_ico_iv_rounded.icns"))
                bundleID = "email.kevinphillips.biblebible"
                packageName = "BibleBible"
                dockName = "My BibleBible"
                appCategory = "public.app-category.reference"
                minimumSystemVersion = "12.0"
                packageBuildVersion = "25"
                pkgPackageVersion = "3.2.0"
                pkgPackageBuildVersion = "25"
                appStore = macAppStoreSignEnabled
                entitlementsFile.set(project.file("entitlements.plist"))
                runtimeEntitlementsFile.set(project.file("runtime-entitlements.plist"))
                infoPlist {
                    extraKeysRawXml = """
                        <key>ITSAppUsesNonExemptEncryption</key>
                        <false/>
                        <key>NSHighResolutionCapable</key>
                        <true/>
                    """.trimIndent()
                }
                if (macAppStoreSignEnabled) {
                    signing {
                        sign.set(true)
                        identity.set(macSignIdentity)
                        if (macKeychainPath.isNotEmpty()) {
                            keychain.set(macKeychainPath)
                        }
                    }
                    if (macAppProfilePath.isNotEmpty()) {
                        val stagedAppProfile = layout.buildDirectory
                            .file("compose/tmp/mac-signing/embedded.provisionprofile")
                            .get()
                            .asFile
                        val sourceAppProfile = file(macAppProfilePath)
                        if (sourceAppProfile.isFile) {
                            stagedAppProfile.parentFile.mkdirs()
                            sourceAppProfile.copyTo(stagedAppProfile, overwrite = true)
                            provisioningProfile.set(stagedAppProfile)
                        } else {
                            provisioningProfile.set(sourceAppProfile)
                        }
                    }
                    if (macRuntimeProfilePath.isNotEmpty()) {
                        runtimeProvisioningProfile.set(file(macRuntimeProfilePath))
                    }
                }
            }
            windows {
                iconFile.set(iconsRoot.resolve("BibleBible_ico_iv_rounded.ico"))
                menuGroup = "Bible Bible"
                upgradeUuid = "18159995-d968-4CD2-8885-77BFA97CFC7F"
            }
            linux {
                iconFile.set(iconsRoot.resolve("BibleBible_ico_iv_rounded.png"))
            }
        }
    }
}
