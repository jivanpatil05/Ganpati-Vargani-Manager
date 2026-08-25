# Keep default rules; Room / Hilt / Serialization keepers below.
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }

# Kotlin Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# App entities used by Room / backup
-keep class com.ganpati.vargani.data.local.room.entity.** { *; }
-keep class com.ganpati.vargani.domain.model.** { *; }

# ZXing
-keep class com.google.zxing.** { *; }
