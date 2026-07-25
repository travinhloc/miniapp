import org.jetbrains.kotlin.konan.properties.loadProperties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.kover)
}

val signingProperties = loadProperties("$rootDir/signing.properties")
val getVersionCode: () -> Int = {
    if (project.hasProperty("versionCode")) {
        (project.property("versionCode") as String).toInt()
    } else {
        libs.versions.androidVersionCode.get().toInt()
    }
}

android {
    namespace = "com.vault.vanishx"
    compileSdk = libs.versions.androidCompileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.vault.vanishx"
        // Story 1.1: API 26+ (mailbox / crypto stack later)
        minSdk = 26
        targetSdk = libs.versions.androidTargetSdk.get().toInt()
        versionCode = getVersionCode()
        versionName = libs.versions.androidVersionName.get()

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        create(BuildTypes.RELEASE) {
            storeFile = file("${rootDir}/config/release.keystore")
            storePassword = signingProperties.getProperty("KEYSTORE_PASSWORD") as String
            keyPassword = signingProperties.getProperty("KEY_PASSWORD") as String
            keyAlias = signingProperties.getProperty("KEY_ALIAS") as String
        }

        getByName(BuildTypes.DEBUG) {
            storeFile = file("${rootDir}/config/debug.keystore")
            storePassword = "oQ4mL1jY2uX7wD8q"
            keyAlias = "debug-key-alias"
            keyPassword = "oQ4mL1jY2uX7wD8q"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isDebuggable = false
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs[BuildTypes.RELEASE]
        }

        create(BuildTypes.PRERELEASE) {
            initWith(getByName(BuildTypes.RELEASE))
        }

        debug {
            isMinifyEnabled = false
            signingConfig = signingConfigs[BuildTypes.DEBUG]
        }
    }

    flavorDimensions += Flavors.DIMENSION_VERSION
    productFlavors {
        create(Flavors.STAGING) {
            applicationIdSuffix = ".staging"
        }

        create(Flavors.PRODUCTION) {}
    }

    sourceSets["test"].resources {
        srcDir("src/test/resources")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_17.toString()
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    lint {
        checkDependencies = true
        xmlReport = true
        xmlOutput = file("build/reports/lint/lint-result.xml")
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

dependencies {
    implementation(projects.core.common)
    implementation(projects.core.mvvm)
    implementation(projects.core.ui)

    implementation(libs.bundles.androidx)

    implementation(platform(libs.compose.bom))
    implementation(libs.bundles.compose)
    debugImplementation(libs.compose.ui.tooling)

    implementation(libs.bundles.hilt)
    ksp(libs.hilt.compiler)

    implementation(libs.timber)

    testImplementation(libs.bundles.unitTest)
    testImplementation(libs.test.turbine)

    testImplementation(platform(libs.compose.bom))
    testImplementation(libs.bundles.uiTest)
}

kover {
    currentProject {
        createVariant("custom") {
            addWithDependencies("stagingDebug")
        }
    }
    reports {
        filters {
            excludes {
                androidGeneratedClasses()
                annotatedBy(
                    "androidx.compose.ui.tooling.preview.Preview",
                    "dagger.Module",
                )
                classes(
                    "*Kt\$Dsl*",
                    "*OuterClass*",
                    "*.*_ComponentTreeDeps*",
                    "*.*_HiltComponents*",
                    "*.*_HiltModules*",
                    "*.*_MembersInjector*",
                    "*.*_Factory*",
                    "*.Hilt_*",
                    "*.*\$Creator*",
                    "*ComposableSingletons*",
                )
                inheritedFrom(
                    "androidx.compose.ui.tooling.preview.PreviewParameterProvider",
                    "dagger.internal.Factory",
                )
                packages(
                    "dagger.hilt.internal",
                    "hilt_aggregated_deps",
                )
            }
        }
    }
}
