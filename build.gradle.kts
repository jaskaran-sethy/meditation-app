// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.android.application") version "8.13.2" apply false
    id("org.jetbrains.kotlin.android") version "2.2.21" apply false
    // Since Kotlin 2.0 the Compose compiler ships with Kotlin and always matches its version.
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.21" apply false
}
