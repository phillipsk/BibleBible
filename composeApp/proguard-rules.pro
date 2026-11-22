# Ktor ProGuard rules
-keepattributes Signature
-keepattributes AnnotationDefault
-dontwarn io.ktor.**
-keep class io.ktor.** { *; }

# Kotlinx Serialization (preserve DTOs & generated serializers under R8 minification)
-keepattributes *Annotation*,InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class data.**$$serializer { *; }
-keepclassmembers class data.** {
    *** Companion;
}
-keepclasseswithmembers class data.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# SQLDelight & Coroutines rules
-dontwarn app.cash.sqldelight.**
-dontwarn kotlinx.coroutines.**
-keep class kotlinx.coroutines.** { *; }

# Generative AI SDK rules
-dontwarn com.google.ai.client.generativeai.**
-keep class com.google.ai.client.generativeai.** { *; }
