# Kotlin Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.sfm.scanner.**$$serializer { *; }
-keepclassmembers class com.sfm.scanner.** {
    *** Companion;
}
-keepclasseswithmembers class com.sfm.scanner.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Hilt
-keepnames @dagger.hilt.android.lifecycle.HiltViewModel class * extends androidx.lifecycle.ViewModel

# Firebase
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**

# ARCore
-keep class com.google.ar.core.** { *; }
-dontwarn com.google.ar.**

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# WorkManager
-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.CoroutineWorker
-keepnames class androidx.work.** { *; }

# Prevent stripping of enum values used in JSON serialization
-keepclassmembers enum com.sfm.scanner.** { *; }
