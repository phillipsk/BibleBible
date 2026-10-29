# Ktor ProGuard rules
-keepattributes Signature
-keepattributes AnnotationDefault
-dontwarn io.ktor.**
-keep class io.ktor.** { *; }

# Generative AI SDK rules
-dontwarn com.google.ai.client.generativeai.**
-keep class com.google.ai.client.generativeai.** { *; }

# Coroutines rules
-dontwarn kotlinx.coroutines.**
-keep class kotlinx.coroutines.** { *; }
