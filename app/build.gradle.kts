import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.google.services)
    alias(libs.plugins.kotlinx.serialization)
}

// ----------------------------------------------------------------------------
// Optional release-signing configuration.
//
// To produce a signed release APK, place a `keystore.properties` file at the
// repo root with the four properties listed in `keystore.properties.template`.
// When the file is absent (e.g. in CI builds, on developer machines without a
// keystore), the release variant falls back to the AGP-default unsigned APK so
// `./gradlew assembleRelease` still succeeds for sanity-check builds.
//
// `keystore.properties` is gitignored — never commit it.
// ----------------------------------------------------------------------------
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) {
        keystorePropertiesFile.inputStream().use { load(it) }
    }
}
val hasReleaseKeystore = keystoreProperties.getProperty("storeFile") != null

android {
    namespace = "com.sfm.scanner"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.sfm.scanner"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (hasReleaseKeystore) {
            create("release") {
                storeFile = rootProject.file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            if (hasReleaseKeystore) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
        debug {
            isMinifyEnabled = false
            // Side-by-side installable with release would normally use a
            // distinct applicationId here (applicationIdSuffix = ".debug").
            // That requires registering "com.sfm.scanner.debug" as a second
            // client in Firebase (google-services.json) — an owner-supplied
            // configuration step. Re-enable once that's done.
            versionNameSuffix = "-debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            // kotlinx.coroutines bundles a debug-agent metadata file used only by
            // IDE step-debugging. Strip it from the final APK; it has no runtime use.
            excludes += "DebugProbesKt.bin"
        }
    }
}

dependencies {
    // Feature modules
    implementation(project(":features:feature-splash"))
    implementation(project(":features:feature-selection"))
    implementation(project(":features:feature-form"))
    implementation(project(":features:feature-scan"))
    implementation(project(":features:feature-upload"))

    // Core modules
    implementation(project(":core:core-common"))
    implementation(project(":core:core-ui"))
    implementation(project(":core:core-storage"))

    // Data modules — required for Hilt component aggregation
    implementation(project(":data:data-camera"))
    implementation(project(":data:data-ar"))
    implementation(project(":data:data-firebase"))

    // ARCore: MainActivity invokes ArCoreApk.requestInstall directly when forwarding
    // ScanUiEffect.RequestArInstall. The class is also present transitively via
    // :data:data-ar, but Gradle compile-classpath isolation requires an explicit dep here.
    implementation(libs.arcore)

    // AndroidX Core
    implementation(libs.core.ktx)
    implementation(libs.activity.compose)
    implementation(libs.lifecycle.runtime.compose)

    // Compose BOM
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui.tooling.preview)

    // Navigation
    implementation(libs.navigation.compose)
    implementation(libs.hilt.navigation.compose)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)

    // WorkManager + Hilt integration
    implementation(libs.work.runtime.ktx)
    implementation(libs.hilt.work)
    ksp(libs.hilt.compiler.androidx)

    // Serialization (for nav arg encoding/decoding)
    implementation(libs.kotlinx.serialization.json)

    // Coroutines
    implementation(libs.coroutines.android)

    // Unit tests
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.turbine)

    // Instrumentation tests
    androidTestImplementation(libs.androidx.test.ext)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.ui.test.junit4)
    androidTestImplementation(libs.hilt.android.testing)
    kspAndroidTest(libs.hilt.android.compiler)

    // Debug
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)
}
