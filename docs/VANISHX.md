# VanishX — liên kết tài liệu sản phẩm

Docs **không nằm trong repo này**. Nguồn sự thật:

| | Path |
|--|------|
| **Docs repo** | `../project-note-cursor/` |
| **VanishX docs** | `../project-note-cursor/vanishx-docs/` |
| Absolute | `/Users/loctra/Workspace/01_Projects/Active/project-note-cursor/vanishx-docs/` |

## File nên đọc (thứ tự)

1. `vanishx-engine-vi.md` — PRD / core MVP  
2. `vanishx-project-planning.md` — timeline core  
3. `stories/README.md` + story đang làm (bắt đầu `1.1.story.md`)  
4. `mockup/main.html` — UI hướng (polish sau MVP)  
5. `vanishx-idea-draft.md` — nhật ký quyết định  

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
@../project-note-cursor/vanishx-docs/stories/1.1.story.md
```
(hoặc kéo folder `vanishx-docs` vào chat)

### Cách C — Rule luôn nhắc path
Xem `.cursor/rules/vanishx-docs.mdc` trong repo này.

## Scope code trong miniapp

- App product: `apps/vanishx/` · `applicationId` = `com.vault.vanishx` · `minSdk` 26
- Package: `presentation` / `domain` / `data` / `di` (app-local; reuse `core/*`)
- Build: `./gradlew :apps:vanishx:assembleStagingDebug`
- Reuse `core/*` · không nhét feature VanishX vào `core`
- Cập nhật docs: sửa bên `project-note-cursor/vanishx-docs`, rồi sync ghi chú ở đây nếu path đổi

## Core MVP (nhắc nhanh)

Crypto + SQLCipher + Firebase mailbox + create/join + gửi/nhận text + TTL/purge + sync-on-open.  
**Sau MVP:** Panic, App lock, FCM, Block/Report, IAP, polish UI.
