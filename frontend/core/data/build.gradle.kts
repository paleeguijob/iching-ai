import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinxSerialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
}

// Base URL of the `api` Cloud Function, from local.properties or -PICHING_API_BASE_URL=...
// e.g. https://asia-southeast1-<project-id>.cloudfunctions.net/api
val apiBaseUrl: String = run {
    val file = rootProject.file("local.properties")
    val props = Properties()
    if (file.exists()) file.inputStream().use(props::load)
    (findProperty("ICHING_API_BASE_URL") as String?)
        ?: props.getProperty("ICHING_API_BASE_URL")
        ?: "https://asia-southeast1-YOUR_FIREBASE_PROJECT_ID.cloudfunctions.net/api"
}
// Firebase Hosting site with the privacy policy / terms / delete-account pages.
val webBaseUrl: String = run {
    val file = rootProject.file("local.properties")
    val props = Properties()
    if (file.exists()) file.inputStream().use(props::load)
    (findProperty("ICHING_WEB_BASE_URL") as String?)
        ?: props.getProperty("ICHING_WEB_BASE_URL")
        ?: "https://YOUR_FIREBASE_PROJECT_ID.web.app"
}
val generatedConfigDir = layout.buildDirectory.dir("generated/buildconfig/commonMain/kotlin")
val generateBuildConfig by tasks.registering {
    val url = apiBaseUrl.trimEnd('/')
    val web = webBaseUrl.trimEnd('/')
    val outDir = generatedConfigDir
    inputs.property("url", url)
    inputs.property("web", web)
    outputs.dir(outDir)
    doLast {
        val file = outDir.get().file("com/iching/ai/data/BuildConfig.kt").asFile
        file.parentFile.mkdirs()
        file.writeText(
            """
            |package com.iching.ai.data
            |
            |object BuildConfig {
            |    const val API_BASE_URL: String = "${url.replace("\"", "")}"
            |    const val WEB_BASE_URL: String = "${web.replace("\"", "")}"
            |}
            |""".trimMargin()
        )
    }
}

kotlin {
    iosArm64()
    iosSimulatorArm64()

    androidLibrary {
        namespace = "com.iching.ai.data"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
        androidResources {
            enable = true
        }
        withHostTestBuilder {}
    }

    sourceSets {
        commonMain {
            kotlin.srcDir(generateBuildConfig)
            dependencies {
                api(projects.core.domain)
                implementation(libs.compose.runtime)
                implementation(libs.compose.components.resources)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.ktor.client.core)
                implementation(libs.room.runtime)
                implementation(libs.sqlite.bundled)
                implementation(libs.datastore.preferences)
                implementation(libs.koin.core)
            }
        }
        androidMain.dependencies {
            implementation(libs.ktor.client.okhttp)
            implementation(libs.koin.android)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.ktor.client.mock)
        }
    }
}

compose.resources {
    packageOfResClass = "com.iching.ai.data.resources"
}

room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    add("kspAndroid", libs.room.compiler)
    add("kspIosArm64", libs.room.compiler)
    add("kspIosSimulatorArm64", libs.room.compiler)
}
