/*
 * Dincharya — app module build script.
 *
 * Version compatibility map (do not bump one without checking the others):
 *   Kotlin 1.9.24  <->  Compose compiler extension 1.5.14  <->  KSP 1.9.24-1.0.20
 *   AGP 8.5.2      <->  Gradle wrapper 8.7
 */
// Imported explicitly because inside the Gradle Kotlin DSL the bare name
// "java" resolves to the java plugin extension, which would break the
// fully-qualified java.net.URI reference used by the font download below.
import java.net.URI
import java.util.Base64

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.dincharya.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.dincharya.app"
        minSdk = 26          // Adaptive icons without legacy PNGs; java.time available
        targetSdk = 34
        versionCode = 5
        versionName = "0.2.3"
        vectorDrawables { useSupportLibrary = true }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Release signing key, provided by CI as environment variables
            // (decoded from GitHub Actions secrets). Locally — where no key
            // is around — we fall back to the debug key so the build still
            // produces an installable APK; it just can't upgrade over a
            // properly signed release install.
            val keystoreBase64 = System.getenv("SIGNING_KEYSTORE_BASE64")
            signingConfig = if (!keystoreBase64.isNullOrBlank()) {
                val keystoreFile = File(
                    layout.buildDirectory.asFile.get(),
                    "release-signing/dincharya-release.p12",
                )
                keystoreFile.parentFile.mkdirs()
                keystoreFile.writeBytes(
                    Base64.getDecoder().decode(keystoreBase64)
                )
                signingConfigs.create("releaseUpload") {
                    storeFile = keystoreFile
                    storeType = "PKCS12"
                    storePassword = System.getenv("SIGNING_STORE_PASSWORD")
                    keyAlias = System.getenv("SIGNING_KEY_ALIAS")
                    keyPassword = System.getenv("SIGNING_KEY_PASSWORD")
                }
            } else {
                signingConfigs.getByName("debug")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }

    buildFeatures { compose = true }
    composeOptions { kotlinCompilerExtensionVersion = "1.5.14" }

    packaging {
        resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" }
    }
}

// Export Room schemas to /app/schemas so DB migrations can be reviewed in PRs.
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

// ---------------------------------------------------------------------------
// Noto Serif fonts (SIL OFL — see THIRD_PARTY_NOTICES.md).
//
// The four TTFs are fetched from the official Noto fonts repository on the
// FIRST build and cached in src/main/res/font/ afterwards. This keeps the
// git repo text-only while the app still bundles the real fonts.
//
// The URLs are pinned to an immutable commit SHA of notofonts.github.io so
// an upstream reorganisation can never silently break the build.
// ---------------------------------------------------------------------------
val fetchNotoSerif by tasks.registering {
    val fontDir = file("src/main/res/font")
    val notoCommit = "d9b11dadb3d9d5cb562d753b0cb59b19ce805afb"
    val fonts = mapOf(
        "noto_serif_regular.ttf" to "NotoSerif-Regular.ttf",
        "noto_serif_medium.ttf" to "NotoSerif-Medium.ttf",
        "noto_serif_semibold.ttf" to "NotoSerif-SemiBold.ttf",
        "noto_serif_bold.ttf" to "NotoSerif-Bold.ttf",
    )
    outputs.upToDateWhen { fonts.keys.all { File(fontDir, it).exists() } }
    doLast {
        fontDir.mkdirs()
        fonts.forEach { (fileName, upstreamName) ->
            val target = File(fontDir, fileName)
            if (!target.exists()) {
                val url = "https://raw.githubusercontent.com/notofonts/notofonts.github.io/" +
                    "$notoCommit/fonts/NotoSerif/hinted/ttf/$upstreamName"
                logger.lifecycle("Downloading $fileName from the Noto fonts repo...")
                URI(url).toURL().openStream().use { input ->
                    target.outputStream().use { output -> input.copyTo(output) }
                }
            }
        }
    }
}

tasks.named("preBuild") { dependsOn(fetchNotoSerif) }

dependencies {
    // --- Android core ---
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.1")

    // --- Compose (BOM pins every Compose artifact version) ---
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.4")

    // --- Persistence: Room (local-first, source of truth on device) ---
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // --- Background work: reminders + nightly retrain ---
    implementation("androidx.work:work-runtime-ktx:2.9.1")

    // --- Testing ---
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
