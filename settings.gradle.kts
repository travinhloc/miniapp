pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.10.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://www.jitpack.io") }
    }
}

rootProject.name = "miniapp"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

include(":apps:sample")
include(":apps:vanishx")
include(":core:common")
include(":core:mvvm")
include(":core:ui")
include(":data")
include(":domain")
