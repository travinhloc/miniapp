---
name: monorepo-role
description: >-
  Defines and updates agent roles, ownership boundaries, and Cursor rules/skills
  for the miniapp monorepo. Use when the user asks about agent roles, who owns
  apps vs core, AGENTS.md, .cursor/rules, or project skills setup.
---

# Monorepo role

## Goal

Keep clear **roles** for humans/agents working in this monorepo so `apps/*` and `core/*` stay separated.

## Roles

| Role | Owns | Must not |
|------|------|----------|
| **App engineer** | `apps/<name>` features, navigation, app DI | Put shared primitives only in one app |
| **Core engineer** | `core/mvvm`, `core/ui`, `core/common` | Product-specific screens/copy |
| **Platform** | Gradle, CI, Detekt, flavors, signing | Feature UI |
| **i18n** | `values*` string catalogs | Hardcoded UI text |

## When changing roles / agent config

1. Update `AGENTS.md` (source of truth)
2. Update or add `.cursor/rules/*.mdc` (alwaysApply for global; globs for focused)
3. Update skills under `.cursor/skills/` if workflows change
4. Commit as `docs(agents): …` or `chore(agents): …`

## Output

Summarize: role name · paths owned · rules/skills touched · commit message suggestion.
