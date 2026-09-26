plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinCompose)
}

android {
    namespace = "com.hoqit.beea"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.hoqit.beea"
        minSdk = 30
        targetSdk = 37
        versionCode = 1
        versionName = "0.0.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(platform(libs.androidxComposeBom))
    implementation(libs.androidxActivityCompose)
    implementation(libs.androidxAppcompat)
    implementation(libs.androidxComposeMaterial3)
    implementation(libs.androidxComposeMaterialIcons)
    implementation(libs.androidxComposeUi)
    implementation(libs.androidxComposeUiGraphics)
    implementation(libs.androidxComposeUiToolingPreview)
    implementation(libs.androidxCoreKtx)
    implementation(libs.androidxLifecycleRuntimeKtx)
    implementation(libs.androidxLifecycleLifecycleService)
    implementation(project(":openwakeword"))
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidxComposeBom))
    androidTestImplementation(libs.androidxComposeUiTestJunit4)
    androidTestImplementation(libs.androidxEspressoCore)
    androidTestImplementation(libs.androidxJunit)
    debugImplementation(libs.androidxComposeUiTestManifest)
    debugImplementation(libs.androidxComposeUiTooling)
}
