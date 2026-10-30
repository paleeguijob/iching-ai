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

// Dev builds may ship a default Gemini key from local.properties (never committed).
val geminiApiKey: String = run {
    val file = rootProject.file("local.properties")
    val props = Properties()
    if (file.exists()) file.inputStream().use(props::load)
    props.getProperty("GEMINI_API_KEY", "")
}
val generatedSecretsDir = layout.buildDirectory.dir("generated/secrets/commonMain/kotlin")
val generateBuildSecrets by tasks.registering {
    val key = geminiApiKey
    val outDir = generatedSecretsDir
    inputs.property("key", key)
    outputs.dir(outDir)
    doLast {
        val file = outDir.get().file("com/iching/ai/data/BuildSecrets.kt").asFile
        file.parentFile.mkdirs()
        file.writeText(
            """
            |package com.iching.ai.data
            |
            |internal object BuildSecrets {
            |    const val DEFAULT_GEMINI_API_KEY: String = "${key.replace("\"", "")}"
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
            kotlin.srcDir(generateBuildSecrets)
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
