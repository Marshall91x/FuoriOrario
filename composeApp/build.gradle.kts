import com.codingfeline.buildkonfig.compiler.FieldSpec.Type.STRING
import java.util.Properties
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSetTree

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.buildkonfig)
    alias(libs.plugins.ktlint)
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
        // commonTest holds Compose UI tests: on Android they run on a device (connectedAndroidTest), not as JVM unit tests.
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        instrumentedTestVariant.sourceSetTree.set(KotlinSourceSetTree.test)
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        unitTestVariant.sourceSetTree.set(KotlinSourceSetTree.unitTest)
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        outputModuleName.set("composeApp")
        browser {
            commonWebpackConfig {
                outputFileName = "composeApp.js"
            }
            testTask {
                useKarma { useChromeHeadless() }
            }
        }
        binaries.executable()
    }

    sourceSets {
        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
            implementation(libs.ktor.client.okhttp)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
        wasmJsMain.dependencies {
            implementation(libs.ktor.client.js)
            // 0.9.0 (from Ktor) reads `import.meta` directly, which breaks the Karma test bundle. Drop when Ktor ships >= 0.9.1.
            implementation(libs.kotlinx.io.core)
        }
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.ui.backhandler)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.navigation.compose)
            implementation(project.dependencies.platform(libs.supabase.bom))
            implementation(libs.supabase.auth)
            implementation(libs.supabase.postgrest)
            implementation(libs.kotlinx.datetime)
            implementation(libs.multiplatform.settings)
            implementation(project.dependencies.platform(libs.koin.bom))
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
            implementation(libs.lifecycle.viewmodel)
            implementation(libs.lifecycle.runtime.compose)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            // A real Supabase 401 for the error handlers.
            implementation(libs.ktor.client.mock)
            @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
            implementation(libs.compose.uiTest)
            implementation(libs.multiplatform.settings.test)
            implementation(libs.navigationevent)
        }
        // Architecture rules (Konsist reads commonMain) and Koin verify(): JVM only.
        androidUnitTest.dependencies {
            implementation(libs.junit)
            implementation(libs.konsist)
            implementation(libs.koin.test)
        }
    }
}

// Supabase, version and signing config: local.properties in dev, environment variables in CI. Never commit real values.
val localProps =
    Properties().apply {
        rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use(::load)
    }

fun config(key: String): String = localProps.getProperty(key) ?: System.getenv(key) ?: ""

// APP_VERSION=X.Y.Z comes from the release tag (vX.Y.Z); versionCode must grow for in-place APK updates.
val appVersion = config("APP_VERSION").ifEmpty { "0.0.0" }
val appVersionCode =
    requireNotNull(Regex("""(\d+)\.(\d{1,2})\.(\d{1,2})""").matchEntire(appVersion)) {
        "APP_VERSION must be X.Y.Z with Y, Z < 100, got '$appVersion'"
    }.destructured.let { (major, minor, patch) ->
        maxOf(1, major.toInt() * 10_000 + minor.toInt() * 100 + patch.toInt())
    }

android {
    namespace = "it.manu.fuoriorario"
    compileSdk = libs.versions.android.compileSdk
        .get()
        .toInt()

    defaultConfig {
        applicationId = "it.manu.fuoriorario"
        minSdk = libs.versions.android.minSdk
            .get()
            .toInt()
        targetSdk = libs.versions.android.targetSdk
            .get()
            .toInt()
        versionCode = appVersionCode
        versionName = appVersion
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    signingConfigs {
        create("release") {
            storeFile = config("ANDROID_KEYSTORE_PATH").takeIf { it.isNotEmpty() }?.let(::file)
            storePassword = config("ANDROID_KEYSTORE_PASSWORD")
            keyAlias = config("ANDROID_KEY_ALIAS")
            keyPassword = config("ANDROID_KEY_PASSWORD")
        }
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        // Debug talks to local Supabase over plain http://127.0.0.1.
        getByName("debug") { manifestPlaceholders["usesCleartextTraffic"] = true }
        getByName("release") {
            manifestPlaceholders["usesCleartextTraffic"] = false
            isMinifyEnabled = false
            // Unsigned when no keystore is configured (local builds); CI always provides one.
            signingConfigs.getByName("release").takeIf { it.storeFile != null }?.let { signingConfig = it }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    debugImplementation(libs.compose.uiTooling)
    debugImplementation(libs.androidx.compose.uiTestManifest)
    androidTestImplementation(libs.androidx.compose.uiTestJunit4)
}

buildkonfig {
    packageName = "it.manu.fuoriorario"
    defaultConfigs {
        buildConfigField(STRING, "SUPABASE_URL", config("SUPABASE_URL"))
        buildConfigField(STRING, "SUPABASE_ANON_KEY", config("SUPABASE_ANON_KEY"))
    }
}

ktlint {
    version.set("1.5.0")
    android.set(true)
    outputToConsole.set(true)
    filter {
        exclude { element -> element.file.path.contains("/build/") }
        exclude { element -> element.file.path.contains("/generated/") }
    }
}
