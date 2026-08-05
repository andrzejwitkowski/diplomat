# Keep kotlinx.serialization generated serializers for our DTOs.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**

-keepclassmembers class com.diplomat.data.remote.dto.** {
    *** Companion;
}
-keepclasseswithmembers class com.diplomat.data.remote.dto.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Room generated implementations are referenced reflectively.
-keep class * extends androidx.room.RoomDatabase { <init>(); }
