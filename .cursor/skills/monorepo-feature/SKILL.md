---
name: monorepo-feature
description: >-
  Implements features in the miniapp monorepo across apps and shared core MVVM
  modules. Use when adding screens, ViewModels, use cases, repositories, or
  shared core APIs for a product app.
---

# Monorepo feature

## Checklist

```
Feature progress:
- [ ] Confirm target app (`apps/<name>`) vs shared `core/*`
- [ ] Domain model + repository (+ use case) if needed
- [ ] Data impl + DI bindings
- [ ] ViewModel extends BaseViewModel (core:mvvm)
- [ ] Screen + Content composables + Preview
- [ ] Strings in values + values-vi (and other locales)
- [ ] Unit tests
- [ ] ./gradlew detekt + relevant unit tests
```

## Placement rules

- **Product-only** → `apps/<name>/…`
- **Reusable MVVM/UI** → `core/mvvm` or `core/ui` (ask if unsure)
- Do not duplicate BaseViewModel / theme per app

## Screen pattern

Public `FeatureScreen(viewModel, navigator)` + private `FeatureScreenContent(state, onAction, modifier)`.

## Commit

`feat(apps/<name>): …` or `feat(core/mvvm): …`
