# Aggressive R8 Full Mode obfuscation & package flattening
-repackageclasses 'o'
-allowaccessmodification
-obfuscationdictionary obfuscation-dictionary.txt
-classobfuscationdictionary obfuscation-dictionary.txt
-packageobfuscationdictionary obfuscation-dictionary.txt
-assumenosideeffects class io.github.aakira.napier.Napier {
    public static void v(...);
    public static void d(...);
    public static void i(...);
}

# Preserve annotations & generics only as required by kotlinx.serialization & Compose runtime
-keepattributes *Annotation*,InnerClasses,Signature,AnnotationDefault
-dontwarn org.slf4j.**
-dontwarn io.ktor.**
-dontwarn app.cash.sqldelight.**
-dontwarn kotlinx.coroutines.**
-dontwarn com.google.android.play.core.**
-dontnote kotlinx.serialization.AnnotationsKt

# Ktor engine ServiceLoader discovery
-keep class * implements io.ktor.client.HttpClientEngineContainer { *; }
-keep class io.ktor.client.engine.okhttp.** { *; }

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
