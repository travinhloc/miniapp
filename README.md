# miniapp

Android **monorepo**: multiple product apps + shared MVVM / UI core.

## Layout

| Path | Gradle | Role |
|------|--------|------|
| `apps/sample` | `:apps:sample` | First host application |
| `core/common` | `:core:common` | Shared utilities / dispatchers |
| `core/mvvm` | `:core:mvvm` | Base ViewModel / screen / navigation types |
| `core/ui` | `:core:ui` | Shared Compose theme |
| `domain` | `:domain` | Shared domain layer |
| `data` | `:data` | Shared data layer |

## Setup

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew :apps:sample:assembleStagingDebug
```

## i18n

- Default (English): `**/res/values/strings.xml`
- Vietnamese: `**/res/values-vi/strings.xml`
- Never hardcode user-facing copy in Kotlin/Compose

## Agent skills

Project skills live under `.cursor/skills/`:

- `monorepo-role` — roles & ownership
- `monorepo-feature` — feature delivery
- `monorepo-code-review` — review checklist

## Commits

`feat|fix|refactor|test|docs|chore|ci(<scope>): summary`  
Scopes: `apps/sample`, `core/mvvm`, `i18n`, …
