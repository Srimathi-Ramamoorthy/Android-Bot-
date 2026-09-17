plugins {
    alias(libs.plugins.android.application)
}


android {
    namespace = "com.example.productiondisplay"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.productiondisplay"
        minSdk = 24
        targetSdk = 35
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        viewBinding = true
    }
}



dependencies {
    //noinspection GradleDependency
    implementation (libs.okhttp)
    implementation (libs.gson)
    implementation (libs.volley)
    implementation (libs.retrofit)
    implementation (libs.converter.gson)
    implementation (libs.sqliteassethelper)
    implementation (libs.appcompat.v161)
    implementation (libs.material.v190)
    implementation (libs.constraintlayout.v214)
    implementation (libs.lifecycle.livedata.ktx.v262)
    implementation (libs.lifecycle.viewmodel.ktx.v262)
    implementation (libs.navigation.fragment.ktx)
    implementation (libs.navigation.ui.ktx)

    testImplementation (libs.junit)
    androidTestImplementation (libs.junit.v115)
    androidTestImplementation (libs.espresso.core.v351)
}
