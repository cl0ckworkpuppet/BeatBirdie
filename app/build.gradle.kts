plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.it391_project_beatbirdie_for_android"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.it391_project_beatbirdie_for_android"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.2"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.recyclerview)
    implementation(libs.fastscroll)
    implementation(libs.glide)
    annotationProcessor(libs.glide.compiler)
    implementation(libs.preference)
    implementation(libs.room.runtime)
    annotationProcessor(libs.room.compiler)
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.common)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
