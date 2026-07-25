# miniapp

Android **monorepo**: multiple product apps + shared MVVM / UI core.

## Layout

| Path | Gradle | Role |
|------|--------|------|
| `apps/sample` | `:apps:sample` | Sample host application |
| `apps/vanishx` | `:apps:vanishx` | VanishX burner chat (`com.vault.vanishx`) |
| `core/common` | `:core:common` | Shared utilities / dispatchers |
| `core/mvvm` | `:core:mvvm` | Base ViewModel / screen / navigation types |
| `core/ui` | `:core:ui` | Shared Compose theme |
| `domain` | `:domain` | Shared domain layer |
| `data` | `:data` | Shared data layer |

## Setup

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew :apps:vanishx:assembleStagingDebug
# or sample:
./gradlew :apps:sample:assembleStagingDebug
```

### VanishX docs (PRD v0.1)

Product specs live outside this repo:

→ [`docs/VANISHX.md`](./docs/VANISHX.md) · [`../project-note-cursor/vanishx-docs/vanishx-engine-vi.md`](../project-note-cursor/vanishx-docs/vanishx-engine-vi.md)

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
Scopes: `apps/sample`, `apps/vanishx`, `core/mvvm`, `i18n`, …

## VanishX

- App module: `apps/vanishx` · `applicationId` `com.vault.vanishx`
- Product docs (PRD, stories, mockup): [`docs/VANISHX.md`](./docs/VANISHX.md) · `../project-note-cursor/vanishx-docs/`
