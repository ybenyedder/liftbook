import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

// Signing secrets live in local.properties (gitignored) — never in this file.
val keystoreProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

android {
    namespace = "com.hevyclone.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.hevyclone.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 40
        versionName = "1.40"
    }

    signingConfigs {
        create("release") {
            storeFile = file("../hevy-release.keystore")
            storePassword = keystoreProps.getProperty("hevyclone.storePassword")
                ?: throw GradleException("hevyclone.storePassword manquant dans local.properties")
            keyAlias = keystoreProps.getProperty("hevyclone.keyAlias") ?: "hevy"
            keyPassword = keystoreProps.getProperty("hevyclone.keyPassword")
                ?: throw GradleException("hevyclone.keyPassword manquant dans local.properties")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release")
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
    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.browser:browser:1.8.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.1")
    // native Google sign-in (Credential Manager) — no browser
    implementation("androidx.credentials:credentials:1.3.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")
    debugImplementation("androidx.compose.ui:ui-tooling")
    testImplementation("junit:junit:4.13.2")
}
