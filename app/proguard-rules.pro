# Proguard / R8 Optimization & Obfuscation Rules for NoiChuan (Voice Filter)

# 1. Attributes & Annotations
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod,SourceFile,LineNumberTable

# 2. Vosk Speech Recognizer (JNI & Native Interface)
-keep class org.vosk.** { *; }
-keepclassmembers class org.vosk.** { *; }
-keepclasseswithmembernames class * {
    native <methods>;
}

# 3. Room Persistence Library
-keep class androidx.room.** { *; }
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-dontwarn androidx.room.paging.**

# 4. Koin Dependency Injection
-keep class org.koin.** { *; }
-keepclassmembers class org.koin.** { *; }
-dontwarn org.koin.**

# 5. Apache Commons Codec (Double Metaphone, Soundex)
-keep class org.apache.commons.codec.** { *; }
-keepclassmembers class org.apache.commons.codec.** { *; }
-dontwarn org.apache.commons.codec.**

# 6. Kotlinx Serialization
-keepattributes *Annotation*,InnerClasses
-dontnote kotlinx.serialization.SerializationKt
-keepclassmembers class * {
    *** Companion;
}
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}
-keepclassmembers class * {
    @kotlinx.serialization.Serializable <fields>;
}

# 7. Jetpack Compose & UI
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# 8. Media3 / ExoPlayer
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**
