import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.googleServices)
    alias(libs.plugins.firebaseCrashlytics)
}

// Keys live in local.properties (not committed). Release falls back to Google's public AdMob *test* ids.
val localProps = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use(::load)
}
fun prop(name: String, default: String) = (findProperty(name) as String?) ?: localProps.getProperty(name) ?: default

val admobTestAppId = "ca-app-pub-3940256099942544~3347511713"
val admobTestRewarded = "ca-app-pub-3940256099942544/5224354917"

// RevenueCat crashes a release build on purpose if it finds a Test Store key, so that key is debug-only.
val revenueCatKey = prop("REVENUECAT_ANDROID_KEY", "")
val revenueCatTestKey = prop("REVENUECAT_ANDROID_TEST_KEY", "")
require(!revenueCatKey.startsWith("test_")) {
    "REVENUECAT_ANDROID_KEY must be the goog_ key; put the test_ key in REVENUECAT_ANDROID_TEST_KEY"
}

// Release signing: create keystore.properties at the root (storeFile, storePassword, keyAlias, keyPassword).
val keystoreProps = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) file.inputStream().use(::load)
}

dependencies {
    implementation(projects.composeApp)
    implementation(libs.androidx.activity.compose)
    implementation(libs.compose.foundation)
    implementation(libs.koin.android)

    implementation(project.dependencies.platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.appcheck.playintegrity)
    debugImplementation(libs.firebase.appcheck.debug)
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.messaging)
    implementation(libs.revenuecat.purchases)
    implementation(libs.play.services.ads)
    implementation(libs.ump)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.playServices)
    implementation(libs.googleid)
    implementation(libs.kotlinx.coroutines.core)
}

android {
    namespace = "com.iching.ai"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.iching.ai"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 120
        versionName = "1.2.0"
    }
    buildFeatures {
        buildConfig = true
    }
    signingConfigs {
        if (keystoreProps.isNotEmpty()) {
            create("release") {
                storeFile = rootProject.file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("debug") {
            buildConfigField("String", "REVENUECAT_ANDROID_KEY", "\"${revenueCatTestKey.ifBlank { revenueCatKey }}\"")
            // Debug always serves Google's test ads, so clicking our own ads can't get the AdMob account flagged.
            buildConfigField("String", "ADMOB_REWARDED_ANDROID", "\"$admobTestRewarded\"")
            manifestPlaceholders["admobAppId"] = admobTestAppId
        }
        getByName("release") {
            buildConfigField("String", "REVENUECAT_ANDROID_KEY", "\"$revenueCatKey\"")
            buildConfigField("String", "ADMOB_REWARDED_ANDROID", "\"${prop("ADMOB_REWARDED_ANDROID", admobTestRewarded)}\"")
            manifestPlaceholders["admobAppId"] = prop("ADMOB_APP_ID_ANDROID", admobTestAppId)
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (keystoreProps.isNotEmpty()) signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_11
    }
}

// Release ships the key inside the app, so it must be RevenueCat's public goog_ key, never a secret sk_ key.
tasks.matching { it.name == "preReleaseBuild" }.configureEach {
    // Locals only: the configuration cache can't serialize references to the build script.
    val isPublicKey = revenueCatKey.startsWith("goog_")
    val keyPrefix = revenueCatKey.take(3).ifBlank { "nothing" }
    doFirst {
        require(isPublicKey) { "REVENUECAT_ANDROID_KEY must be the public goog_ key for release builds (found $keyPrefix...)" }
    }
}
