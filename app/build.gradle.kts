plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.resqlink"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.resqlink"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.lifecycle.livedata.ktx)
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)
    implementation(libs.inputmapping)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation("com.airbnb.android:lottie:6.4.0")
    implementation("com.google.android.material:material:1.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    //live location dependency :
    implementation("com.google.android.gms:play-services-location:21.0.1")




    // OSM Android SDK
    implementation("org.osmdroid:osmdroid-android:6.1.16")

    // Retrofit for API calls
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")

    // Location services
    implementation("com.google.android.gms:play-services-location:21.0.1")

    // RecyclerView & CardView
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.cardview:cardview:1.0.0")

    // Dexter for permissions
    implementation("com.karumi:dexter:6.2.3")

    // For phone call intent
    implementation("androidx.core:core-ktx:1.12.0")

    //updated dependencies for opensteet map :
    implementation("org.osmdroid:osmdroid-android:6.1.16")
    implementation("org.osmdroid:osmdroid-mapsforge:6.1.16") // Optional for offline maps

    implementation("org.jsoup:jsoup:1.17.2")

    implementation("com.google.android.libraries.places:places:3.3.0")








}