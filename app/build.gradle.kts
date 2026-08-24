plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

// Supplied by CI as -PbuildNumber=${{ github.run_number }}; 0 for local builds.
val buildNumber: String = (project.findProperty("buildNumber") as String?) ?: "0"

android {
    namespace = "com.k8thegreat.devview"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.k8thegreat.devview"
        minSdk = 26
        targetSdk = 34

        // Keep these in sync with the Linear session-tracking issue.
        versionCode = 2
        versionName = "Ada 2"

        // ML Kit ships its OCR pipeline as a ~11 MB native library per architecture.
        // Shipping all four puts ~39 MB in the APK of which a given phone uses one.
        // Every 64-bit Android device runs arm64-v8a; x86 and x86_64 are emulators.
        ndk { abiFilters += listOf("arm64-v8a") }

        buildConfigField("String", "BUILD_NUMBER", "\"$buildNumber\"")
        buildConfigField(
            "String",
            "VERSION_BLURB",
            "\"Ada Lovelace wrote the first published algorithm intended for a machine, " +
                "and saw that such a machine could manipulate symbols rather than only numbers.\"",
        )
    }

    buildTypes {
        // Debug is the build we actually install. It is auto-signed with the debug
        // keystore, so no suffixes here: the package name and version string on the
        // phone match the Linear session record exactly.
        debug {
            isDebuggable = true
        }
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
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
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.mlkit.text.recognition)
    implementation(libs.coil.compose)
    implementation(libs.androidx.exifinterface)
    implementation(libs.kotlinx.serialization.json)

    debugImplementation(libs.androidx.ui.tooling)
}
