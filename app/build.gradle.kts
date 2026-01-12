plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.gms.google-services")
    id("kotlin-parcelize")
}

android {
    namespace = "com.TOTOMOFYP.VTOAPP"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.TOTOMOFYP.VTOAPP"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        
        // Load secrets from local.properties
        val localProperties = java.util.Properties()
        val localPropertiesFile = rootProject.file("local.properties")
        if (localPropertiesFile.exists()) {
            localProperties.load(java.io.FileInputStream(localPropertiesFile))
        }

        val banubaToken = localProperties.getProperty("BANUBA_TOKEN") ?: "\"\""
        val klingAccessKey = localProperties.getProperty("KLING_ACCESS_KEY") ?: "\"\""
        val klingSecretKey = localProperties.getProperty("KLING_SECRET_KEY") ?: "\"\""

        buildConfigField("String", "BANUBA_TOKEN", banubaToken)
        buildConfigField("String", "KLING_ACCESS_KEY", klingAccessKey)
        buildConfigField("String", "KLING_SECRET_KEY", klingSecretKey)
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
        isCoreLibraryDesugaringEnabled = true
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        viewBinding = true
    }
    
    lint {
        baseline = file("lint-baseline.xml")
        disable += listOf("FragmentAddMenuProvider", "FragmentBackPressedCallback", "FragmentLiveDataObserve")
        abortOnError = false
    }
}


dependencies {
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)



    // Jetpack Compose (UI Framework)
    implementation("androidx.compose.ui:ui:1.7.8")
    implementation("androidx.compose.material:material:1.7.8")
    implementation("androidx.compose.material3:material3:1.3.1")
    implementation("androidx.compose.ui:ui-tooling-preview:1.7.8")
    implementation("androidx.navigation:navigation-compose:2.8.9")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.compose.material:material-icons-extended:1.7.8")
    implementation("androidx.compose.foundation:foundation-layout:1.7.8")
    implementation("androidx.compose.runtime:runtime-livedata:1.7.8")

    // XML-based Navigation Component dependencies
    implementation("androidx.navigation:navigation-fragment-ktx:2.7.7")
    implementation("androidx.navigation:navigation-ui-ktx:2.7.7")
    
    // Support for XML layouts
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("com.google.android.material:material:1.11.0")
    
    // CameraX support for AR try-on
    implementation("androidx.camera:camera-camera2:1.3.1")
    implementation("androidx.camera:camera-lifecycle:1.3.1")
    implementation("androidx.camera:camera-view:1.3.1")
    // Guava for CameraX
    implementation("com.google.guava:guava:31.1-android")
    
    // Coil for image loading
    implementation("io.coil-kt:coil-compose:2.5.0")
    
    // OkHttp for API calls
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    implementation(platform("com.google.firebase:firebase-bom:33.11.0"))
    // Firebase Authentication
    implementation("com.google.firebase:firebase-auth-ktx")

    // Firestore (for storing user data)
    implementation("com.google.firebase:firebase-firestore-ktx")

    // Firebase UI (for pre-built authentication UI)
    implementation ("com.firebaseui:firebase-ui-auth:8.0.2")

    // Firebase Storage (if you need to store images)
    implementation("com.google.firebase:firebase-storage-ktx:21.0.1")

    implementation("com.google.android.gms:play-services-auth:21.3.0")

    implementation("com.facebook.android:facebook-login:18.0.2")
    //Banuba api
    api("androidx.recyclerview:recyclerview:1.3.2")
    api("androidx.constraintlayout:constraintlayout:2.1.4")
    api("androidx.appcompat:appcompat:1.6.1")
    implementation ("com.banuba.sdk:banuba_sdk:1.17.4")

    // WorkManager for scheduled notifications
    implementation("androidx.work:work-runtime-ktx:2.9.0")
}