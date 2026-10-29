-keep class io.ktor.** { *; }
-keep class kotlinx.serialization.** { *; }
-keep class org.slf4j.** { *; }


# Suppress SLF4J warnings
-dontwarn org.slf4j.**

## Suppress other class warnings (if needed)
#-dontwarn kotlinx.serialization.**
#-dontwarn io.ktor.**

-optimizations !code/simplification/arithmetic,!field/*,!class/merging/*