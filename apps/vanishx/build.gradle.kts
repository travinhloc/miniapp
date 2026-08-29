import org.jetbrains.kotlin.konan.properties.loadProperties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.kover)
    alias(libs.plugins.google.services)
}

/**
 * Real `google-services.json` files stay gitignored (or local-only).
 * If missing (e.g. CI), copy the committed placeholder next to it.
 * Never overwrites an existing json.
 */
fun ensureGoogleServicesFromPlaceholder(flavorDir: String) {
    val target = file("src/$flavorDir/google-services.json")
    if (target.exists()) return
    val placeholder = file("src/$flavorDir/google-services.placeholder.json")
    check(placeholder.exists()) {
        "Missing ${placeholder.path} — required when google-services.json is absent"
    }
    target.parentFile.mkdirs()
    placeholder.copyTo(target, overwrite = false)
}

ensureGoogleServicesFromPlaceholder("staging")
ensureGoogleServicesFromPlaceholder("production")

val signingPropertiesFile = rootProject.file("signing.properties")
val releaseKeystoreFile = rootProject.file("config/release.keystore")
val canSignRelease = signingPropertiesFile.exists() && releaseKeystoreFile.exists()
val signingProperties = if (canSignRelease) {
    loadProperties(signingPropertiesFile.absolutePath)
} else {
    null
}
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
        if (canSignRelease) {
            val props = checkNotNull(signingProperties)
            create(BuildTypes.RELEASE) {
                storeFile = releaseKeystoreFile
                storePassword = props.getProperty("KEYSTORE_PASSWORD")
                keyPassword = props.getProperty("KEY_PASSWORD")
                keyAlias = props.getProperty("KEY_ALIAS")
            }
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
            if (canSignRelease) {
                signingConfig = signingConfigs[BuildTypes.RELEASE]
            }
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
            buildConfigField("String", "INVITE_HTTPS_HOST", "\"vanihx-staging.web.app\"")
            manifestPlaceholders["appLinkHost"] = "vanihx-staging.web.app"
            manifestPlaceholders["appLinkHostAlt"] = "vanihx-staging.firebaseapp.com"
        }

        create(Flavors.PRODUCTION) {
            buildConfigField("String", "INVITE_HTTPS_HOST", "\"vanishx.app\"")
            manifestPlaceholders["appLinkHost"] = "vanishx.app"
            manifestPlaceholders["appLinkHostAlt"] = "www.vanishx.app"
        }
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

    implementation(libs.tink.android)
    implementation(libs.androidx.security.crypto)
    implementation(libs.androidx.biometric)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.sqlcipher.android)
    implementation(libs.androidx.sqlite)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.database)
    implementation(libs.firebase.messaging)
    implementation(libs.firebase.storage)
    implementation(libs.coil.compose)
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.ui)
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.androidx.camera.video)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.zxing.core)
    implementation(libs.zxing.android.embedded)
    implementation(libs.accompanist.permissions)
    implementation(libs.timber)

    testImplementation(libs.bundles.unitTest)
    testImplementation(libs.test.turbine)
    testImplementation(libs.test.core.ktx)
    testImplementation(libs.androidx.room.runtime)
    testImplementation(libs.androidx.room.ktx)

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
