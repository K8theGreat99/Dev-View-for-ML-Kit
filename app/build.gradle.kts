plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
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
        versionCode = 1
        versionName = "Ada"

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

    debugImplementation(libs.androidx.ui.tooling)
}
