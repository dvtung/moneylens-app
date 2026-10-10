# ProGuard Rules for MoneyLens (Release Obfuscation & Shrinking)

# Keep line numbers for meaningful crash reporting
-keepattributes SourceFile,LineNumberTable

# Room Database
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }

# Moshi JSON converter
-keepclasseswithmembers class * {
    @com.squareup.moshi.Json <fields>;
}
-keep @com.squareup.moshi.JsonClass class * { *; }
-dontwarn com.squareup.moshi.**

# Retrofit & OkHttp
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn okhttp3.**
-dontwarn okio.**

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# AndroidX Biometric
-keep class androidx.biometric.** { *; }

# Domain models & entities
-keep class com.dvt.moneylens.data.local.entity.** { *; }
-keep class com.dvt.moneylens.domain.model.** { *; }
