/*
 * Dincharya — root Gradle build script.
 *
 * Declares the Android/Kotlin/KSP plugin versions once at the top level so
 * the app module (app/build.gradle.kts) applies them without repeating
 * version numbers. Keep these three versions in sync:
 *   - Kotlin            <-> Compose compiler extension (see app module)
 *   - KSP               <-> Kotlin
 *   - AGP               <-> Gradle wrapper version (see gradle-wrapper.properties)
 */
plugins {
    id("com.android.application") version "8.5.2" apply false
    id("org.jetbrains.kotlin.android") version "1.9.24" apply false
    id("com.google.devtools.ksp") version "1.9.24-1.0.20" apply false
}
