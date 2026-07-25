---
name: monorepo-code-review
description: >-
  Reviews pull requests and diffs for the miniapp Android monorepo against
  architecture, MVVM, i18n, security, and test standards. Use when reviewing
  code, PRs, or when the user asks for a code review.
---

# Monorepo code review

## Severity

- Critical — must fix before merge
- Suggestion — should fix
- Nice to have — optional

## Checklist

1. **Architecture:** no `core` → `apps` dependency; no Android in `domain`; features not dumped into `core`
2. **MVVM:** StateFlow; `BaseViewModel`; no ViewModel passed to children
3. **Compose:** PascalCase screens; `modifier` first optional param; previews at bottom
4. **i18n:** no hardcoded user-facing strings; locales updated together
5. **Security:** no secrets; debug-only logging; careful `exported`
6. **Tests:** new logic covered; naming `` `When …, it …` ``
7. **Commits:** conventional type(scope); scope matches touched module

## Output format

```
### Findings
- Critical: …
- Suggestion: …
- Nice to have: …

### Summary
(1–2 sentences)
```
