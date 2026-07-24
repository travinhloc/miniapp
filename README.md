# miniapp

Android monorepo (`app` / `domain` / `data`) with Jetpack Compose.

## Setup

- Open in Android Studio
- JDK 17+ (Android Studio JBR works)

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew assembleStagingDebug
```

## Lint / static analysis

```bash
./gradlew lint
./gradlew detekt
```

## Tests

```bash
./gradlew app:testStagingDebugUnitTest
./gradlew data:testDebugUnitTest
./gradlew domain:test
./gradlew koverHtmlReportCustom
```

## Release signing

- Put `release.keystore` in `config/`
- Fill `signing.properties` (do not commit secrets)
