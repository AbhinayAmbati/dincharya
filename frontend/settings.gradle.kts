/*
 * Dincharya — frontend (Android app) Gradle settings.
 *
 * Declares the plugin repositories and the single app module.
 * The backend lives in ../backend (Node.js/Express, planned Phase 5) and is
 * built independently by its own toolchain.
 */
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Dincharya"
include(":app")
