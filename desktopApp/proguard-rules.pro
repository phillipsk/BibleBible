# Release 3.2 Desktop IP Hardening & Obfuscation
-obfuscationdictionary ../composeApp/obfuscation-dictionary.txt
-classobfuscationdictionary ../composeApp/obfuscation-dictionary.txt
-packageobfuscationdictionary ../composeApp/obfuscation-dictionary.txt
-allowaccessmodification

# Strip verbose debug logging in release desktop builds
-assumenosideeffects class io.github.aakira.napier.Napier {
    public static void v(...);
    public static void d(...);
    public static void i(...);
}

# Preserve annotations, generics, and Kotlin metadata needed by Compose & Serialization
-keepattributes *Annotation*,InnerClasses,Signature,AnnotationDefault,Exceptions

# Keep desktop entrypoint
-keep class email.kevinphillips.biblebible.BibleBibleKt {
    public static void main(java.lang.String[]);
}

# Keep Compose Multiplatform generated resources
-keep class composeapp.composeapp.generated.resources.** { *; }

# Keep Skiko / Skia JNI bindings
-keep class org.jetbrains.skia.** { *; }
-keep class org.jetbrains.skiko.** { *; }

# Keep SQLDelight & SQLite JDBC native driver bindings
-keep class org.sqlite.** { *; }
-keep class app.cash.sqldelight.** { *; }

# Keep Ktor engine, ServiceLoader, and plugins intact
-keep class io.ktor.** { *; }

# Kotlinx Serialization: keep serializer wiring while allowing DTO class/field obfuscation
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,allowobfuscation,includedescriptorclasses class data.** { *; }
-keep,allowobfuscation,includedescriptorclasses class data.**$$serializer { *; }
-keepclassmembers,allowobfuscation class data.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}

# Suppress optional / desktop-unused warnings
-dontwarn org.slf4j.**
-dontwarn io.netty.**
-dontwarn android.**
-dontwarn androidx.**
-dontwarn com.google.android.**
-dontwarn kotlinx.serialization.AnnotationsKt
-dontwarn org.jetbrains.skiko.**
-dontwarn org.sqlite.**
-dontwarn sun.misc.Unsafe
-dontwarn java.lang.invoke.**
