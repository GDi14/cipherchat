# Add project specific ProGuard rules here.

# Keep Room Database classes
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Keep Data DTOs and Serialized models
-keepclassmembers class * {
    @com.squareup.moshi.Json *;
    @kotlinx.serialization.SerialName *;
    @kotlinx.serialization.Serializable *;
}
-keep @kotlinx.serialization.Serializable class * { *; }

# Keep Signal Protocol classes
-keep class org.whispersystems.libsignal.IdentityKeyPair { *; }
-keep class org.whispersystems.libsignal.IdentityKey { *; }
-keep class org.whispersystems.libsignal.state.** { *; }
-keep class org.whispersystems.libsignal.protocol.** { *; }

# Keep Supabase & Ktor DTOs
-keep class com.example.data.** { *; }
-keep class com.example.crypto.** { *; }

