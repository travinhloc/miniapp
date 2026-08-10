# VanishX — liên kết tài liệu sản phẩm

Docs **không nằm trong repo này**. Nguồn sự thật:

| | Path |
|--|------|
| **Docs repo** | `../project-note-cursor/` |
| **VanishX docs** | `../project-note-cursor/vanishx-docs/` |
| Absolute | `/Users/loctra/Workspace/01_Projects/Active/project-note-cursor/vanishx-docs/` |

## File nên đọc (thứ tự)

1. `vanishx-engine-vi.md` — PRD / core MVP  
2. `vanishx-project-planning.md` — timeline  
3. `stories/README.md` + story đang làm (**Epic 6** / **Epic R** · E11 Done: `11.0.epic.md` · debt D11-*)  
4. `stories/completed/` — epic/story Done (1–5, 7–9)  
5. `vanishx-ui-ux-sprint-backlog.md` — Confirm PO / Chat backlog  
6. `vanishx-production-dod.md` + `stories/R.0.epic.md` — Epic R Release Prod / soft-launch  
7. `mockup/main.html` — UI M2 Night gallery  
8. `vanishx-idea-draft.md` — nhật ký quyết định  

BMAD lean (stack Android→KMP): `../project-note-cursor/bmad-agent/LEAN-MOBILE.md`

## Cách Cursor / agent “hiểu” docs

### Cách A — Multi-root (khuyên dùng)
Mở workspace gồm cả hai folder:
- `miniapp`
- `project-note-cursor` (hoặc chỉ `vanishx-docs`)

Rồi trong chat `@vanishx-docs` hoặc `@vanishx-engine-vi.md`.

### Cách B — Attach thủ công mỗi chat
Khi làm VanishX trong `miniapp`, attach:
```
@../project-note-cursor/vanishx-docs/vanishx-engine-vi.md
@../project-note-cursor/vanishx-docs/stories/README.md
```
(hoặc kéo folder `vanishx-docs` vào chat)

### Cách C — Rule luôn nhắc path
Xem `.cursor/rules/vanishx-docs.mdc` trong repo này.

## Scope code trong miniapp

- App product: `apps/vanishx/` · `applicationId` = `com.vault.vanishx` · `minSdk` 26
- Package: `presentation` / `domain` / `data` / `di` (app-local; reuse `core/*`)
- Crypto scheme: [`apps/vanishx/CRYPTO.md`](../apps/vanishx/CRYPTO.md)
- Mailbox (RTDB): [`apps/vanishx/MAILBOX.md`](../apps/vanishx/MAILBOX.md)
- Build staging: `./gradlew :apps:vanishx:assembleStagingDebug`
- Build production: `./gradlew :apps:vanishx:assembleProductionRelease` (Epic R.1–R.2)
- Reuse `core/*` · không nhét feature VanishX vào `core`
- Cập nhật docs: sửa bên `project-note-cursor/vanishx-docs`, rồi sync ghi chú ở đây nếu path đổi

## Core MVP (nhắc nhanh)

Crypto + SQLCipher + Firebase mailbox + create/join + gửi/nhận text + TTL/purge + sync-on-open.  
**Đã ship:** Panic · App lock · FCM · Block/Report · Pro stub · Epic 5–9 · **Epic 11** Media (debt D11-* parked).  
**Đang mở:** Epic 6 Optimize · Epic R Release Prod (`vanishx-production-dod.md`).
