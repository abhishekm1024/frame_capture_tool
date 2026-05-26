# ============================================================================
# R8 keep rules for release builds (used when isMinifyEnabled=true).
#
# Each block below corresponds to a library that uses reflection or annotation
# processing such that R8's static analysis cannot prove a class is reachable.
# Default AGP rules (proguard-android-optimize.txt) cover Android framework
# classes; everything below is specific to this app's third-party deps.
#
# Reviewed against the actual classpath in user-M14 (release prep).
# ============================================================================

# ----------------------------------------------------------------------------
# kotlinx.serialization
#   Generated $$serializer classes are referenced by reflection from the
#   Companion's serializer() factory. We keep:
#     • Annotation metadata (required for generated factories to resolve).
#     • Every $$serializer in the app's package.
#     • Every Companion holding a serializer(...) factory.
# ----------------------------------------------------------------------------
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

# ----------------------------------------------------------------------------
# Hilt
#   Hilt's reflection-driven ViewModel factory looks up class names; we keep
#   names of @HiltViewModel classes so the lookup table still resolves after
#   minification. @HiltWorker is covered by the WorkManager rule below
#   (Worker / CoroutineWorker keep-class).
# ----------------------------------------------------------------------------
-keepnames @dagger.hilt.android.lifecycle.HiltViewModel class * extends androidx.lifecycle.ViewModel

# ----------------------------------------------------------------------------
# Firebase + Google Play Services
#   Firebase libraries use unsafe reflection internally. Per Firebase's own
#   release guidance we keep the entire surface to avoid runtime NPEs.
# ----------------------------------------------------------------------------
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**

# ----------------------------------------------------------------------------
# ARCore
#   Native bridge calls into Kotlin/Java via JNI on class/method names that
#   R8 cannot see. Full keep is the documented stance from Google.
# ----------------------------------------------------------------------------
-keep class com.google.ar.core.** { *; }
-dontwarn com.google.ar.**

# ----------------------------------------------------------------------------
# Room
#   RoomDatabase subclasses are looked up by name at runtime (Room.databaseBuilder
#   calls Class.forName on the "{Database}_Impl" companion). @Entity classes
#   are reflected over by Room's column mapper.
# ----------------------------------------------------------------------------
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# ----------------------------------------------------------------------------
# WorkManager
#   WorkManager loads Worker/CoroutineWorker subclasses by full class name from
#   the input data of an enqueued work request. -keepnames preserves the
#   binary name so reflective instantiation still finds the class.
# ----------------------------------------------------------------------------
-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.CoroutineWorker
-keepnames class androidx.work.** { *; }

# ----------------------------------------------------------------------------
# kotlinx.coroutines
#   The coroutines-debug agent class is only on classpath in some configurations;
#   silence the missing-class warning in release where it is absent.
# ----------------------------------------------------------------------------
-dontwarn kotlinx.coroutines.debug.**

# ----------------------------------------------------------------------------
# Enum stability
#   data_contracts.md JSON shapes include enums (HitType, ArTrackingState).
#   Kotlinx serialization reads enum values by name; preserve member names so
#   round-trip JSON matches the spec after minification.
# ----------------------------------------------------------------------------
-keepclassmembers enum com.sfm.scanner.** { *; }
