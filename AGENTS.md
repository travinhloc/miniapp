# AGENTS.md

You are an experienced Android engineer working in the **miniapp monorepo**.

## Mission

Build multiple Android product apps that share a common **MVVM / UDF** base, Compose UI kit, and networking primitives. Prefer reuse through `core/*` over copying code between apps.

## Monorepo layout

```
miniapp/
├── apps/                 # Product applications (one module per app)
│   └── sample/           # First host app (:apps:sample)
├── core/                 # Shared libraries (no product-specific features)
│   ├── common/           # Dispatchers, tiny utilities
│   ├── mvvm/             # BaseViewModel, BaseScreen, BaseDestination
│   └── ui/               # Design system / theme
├── domain/               # Shared domain contracts (evolve per-app if needed)
├── data/                 # Shared data implementations
├── .cursor/
│   ├── rules/            # Always-on / file-scoped agent rules
│   └── skills/           # Workflows: role · feature · code-review
└── AGENTS.md
```

**Dependency direction (never invert):**

`apps/*` → `core/*` + (`domain` ← `data`)

- `core/*` must not depend on any `apps/*`
- `domain` stays pure Kotlin (no Android)
- New product apps: `apps/<name>/` + `include(":apps:<name>")` in `settings.gradle.kts`

## Stack

| Area | Choice |
|------|--------|
| UI | Jetpack Compose + Material 3 |
| Architecture | MVVM + UDF, StateFlow |
| DI | Hilt |
| Async | Coroutines + Flow |
| Network | Retrofit + OkHttp + Moshi |
| Languages | Android resources (`values`, `values-vi`, …) — no hardcoded user-facing copy |
| Min / compile SDK | See `gradle/libs.versions.toml` |

## Agent workflows (skills)

| Skill | When to use |
|-------|-------------|
| `monorepo-role` | Define/adjust agent roles, rules, or ownership boundaries |
| `monorepo-feature` | Implement a feature across apps/core |
| `monorepo-code-review` | Review a PR/diff against monorepo standards |

## Commit convention

```
<type>(<scope>): <summary>
```

**Types:** `feat` · `fix` · `refactor` · `test` · `docs` · `chore` · `ci`

**Scopes (examples):** `apps/sample` · `core/mvvm` · `core/ui` · `core/common` · `domain` · `data` · `i18n` · `agents`

Examples:

```
feat(apps/sample): add home empty state
fix(core/mvvm): prevent loading counter underflow
chore(i18n): add Vietnamese strings for errors
docs(agents): clarify feature skill checklist
```

One logical change per commit. Present tense. If the message needs “and”, split the commit.

## Defaults

- Dev build: `./gradlew :apps:sample:assembleStagingDebug`
- Static analysis: `./gradlew detekt lint`
- Unit tests: `./gradlew :apps:sample:testStagingDebugUnitTest :data:testDebugUnitTest :domain:test`

## Boundaries

✅ Required: Compose-only UI · StateFlow · Hilt · extend shared `BaseViewModel` · localized strings · tests for new logic  

⚠️ Ask before: new Gradle module · new third-party dependency · changing flavors/Detekt/CI  

🚫 Don't: put product features in `core/*` · Android deps in `domain` · LiveData · hardcode user-facing text · commit secrets (`signing.properties`, keystores)

## VanishX product context

Specs and stories are **outside** this repo. Before VanishX work, read:

- `docs/VANISHX.md` (pointer)
- `../project-note-cursor/vanishx-docs/vanishx-engine-vi.md`
- Active file under `../project-note-cursor/vanishx-docs/stories/`

Prefer attaching those paths in chat (`@vanishx-docs`) or open a multi-root workspace with `project-note-cursor`.
