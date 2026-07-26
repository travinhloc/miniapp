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
 * Staging `google-services.json` is gitignored (real Firebase config stays local).
 * CI / clean checkouts need a placeholder so `process*GoogleServices` can run.
 * Never overwrites an existing file.
 */
fun ensureGoogleServicesPlaceholder(
    relativePath: String,
    packageName: String,
    projectId: String,
) {
    val target = file(relativePath)
    if (target.exists()) return
    target.parentFile.mkdirs()
    target.writeText(
        """
        {
          "project_info": {
            "project_number": "0",
            "project_id": "$projectId",
            "storage_bucket": "$projectId.appspot.com"
          },
          "client": [
            {
              "client_info": {
                "mobilesdk_app_id": "1:0:android:0000000000000000000000",
                "android_client_info": {
                  "package_name": "$packageName"
                }
              },
              "oauth_client": [],
              "api_key": [
                {
                  "current_key": "AIzaSyCiPlaceholderDoNotUse"
                }
              ],
              "services": {
                "appinvite_service": {
                  "other_platform_oauth_client": []
                }
              }
            }
          ],
          "configuration_version": "1"
        }
        """.trimIndent() + "\n",
    )
}

ensureGoogleServicesPlaceholder(
    relativePath = "src/staging/google-services.json",
    packageName = "com.vault.vanishx.staging",
    projectId = "vanishx-staging-ci-placeholder",
)
ensureGoogleServicesPlaceholder(
    relativePath = "src/production/google-services.json",
    packageName = "com.vault.vanishx",
    projectId = "vanishx-production-placeholder",
)

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

    implementation(libs.tink.android)
    implementation(libs.androidx.security.crypto)
    implementation(libs.sqlcipher.android)
    implementation(libs.androidx.sqlite)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.database)
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
