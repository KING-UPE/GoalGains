plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.myapplication"
    compileSdk = 36

    buildFeatures {
        viewBinding = true
    }

    defaultConfig {
        applicationId = "com.example.myapplication"
        minSdk = 23             // lower for max compatibility
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Ensure ABI splits include all common devices
        ndk {
            abiFilters += listOf("armeabi-v7a", "arm64-v8a")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            // Optional: universal debug APK for testing on any device
            ndk {
                abiFilters += listOf("armeabi-v7a", "arm64-v8a")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    // Fix 16 KB alignment for native libraries
    packagingOptions {
        jniLibs {
            useLegacyPackaging = false
        }
    }

}


dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)

    // These are the correct Java versions for navigation
    implementation("androidx.navigation:navigation-fragment:2.7.7")
    implementation("androidx.navigation:navigation-ui:2.7.7")
    implementation("androidx.coordinatorlayout:coordinatorlayout:1.2.0")
    implementation("com.google.android.material:material:1.13.0")

    // For Charts
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")

    // Firebase Bill of Materials (BoM) - Recommended
    // The invalid [3, 5] has been removed from this line
    implementation(platform("com.google.firebase:firebase-bom:34.4.0"))

    // Firebase Dependencies (no versions needed due to BoM)
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-analytics")

    // === NEW: Add these two lines for Glide ===
    implementation ("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor ("com.github.bumptech.glide:compiler:4.16.0")

    implementation("androidx.core:core-splashscreen:1.0.1")
// ML Kit barcode scanning
    implementation("com.google.mlkit:barcode-scanning:17.2.0")

// CameraX (updated)
    implementation("androidx.camera:camera-core:1.3.3")
    implementation("androidx.camera:camera-camera2:1.3.3")
    implementation("androidx.camera:camera-lifecycle:1.3.3")
    implementation("androidx.camera:camera-view:1.3.3")

// Glide
    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")


    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
