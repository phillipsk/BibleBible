import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.jetbrainsCompose)
}
kotlin {
    jvm()
    sourceSets {
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(project(":composeApp"))
        }
    }
}

compose.desktop {
    application {
        mainClass = "email.kevinphillips.biblebible.BibleBibleKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "BibleBible"
            packageVersion = "1.0.0"

            val iconsRoot = project.file("desktop-icons")
            macOS {
                iconFile.set(iconsRoot.resolve("BibleBible_ico_iv_rounded.icns"))
            }
            windows {
                iconFile.set(iconsRoot.resolve("BibleBible_ico_iv_rounded.ico"))
                menuGroup = "Bible Bible"
                // see https://wixtoolset.org/documentation/manual/v3/howtos/general/generate_guids.html
                upgradeUuid = "18159995-d968-4CD2-8885-77BFA97CFC7G"

            }
            linux {
                iconFile.set(iconsRoot.resolve("BibleBible_ico_iv_rounded.png"))
            }
        }
    }
}
