# MindVault Code Style

This project follows **native conventions per platform**.  
**There is no global “prefer camelCase” rule.** Use what each ecosystem expects for maximum readability and tooling support.

---

## 1) Android (Kotlin) — follow official Kotlin/Android style
- **Packages:** `lowercase.without_underscores`
- **Classes / Interfaces / Enums / Objects:** `PascalCase` (e.g., `MainActivity`, `CryptoHelper`)
- **Functions & Variables:** `lowerCamelCase` (e.g., `syncEncryptedEntry`, `dataKey`)
- **Constants:** `UPPER_SNAKE_CASE` (e.g., `DEFAULT_TIMEOUT_MS`)
- **XML resource names:** `snake_case` (e.g., `activity_main.xml`, `ic_launcher_foreground.xml`)
- **IDs in XML:** `snake_case` (e.g., `@+id/btn_backup`)
- **File names (Kotlin):** match the primary type (e.g., `MainActivity.kt`, `SyncClient.kt`)
- **JSON keys to/from Android:** `snake_case` externally unless interoperating with a strict external API

Lint/format:
- Use **ktlint**/**Android Studio** formatter defaults.
- Line length guideline: 120 chars (wrap if readability suffers).
- No wildcard imports.

---

## 2) Server (Python/FastAPI)
- **Modules & files:** `snake_case.py`
- **Functions & variables:** `snake_case`
- **Classes:** `PascalCase`
- **Constants:** `UPPER_SNAKE_CASE`
- **Environment variables:** `UPPER_SNAKE_CASE`
- **Routes & JSON:** `snake_case` for payloads and responses

Lint/format:
- **black** (line length 100), **isort** (black profile), **ruff** for linting.

---

## 3) SQL / SQLite
- **Table & column names:** `snake_case` (e.g., `journal_entries`, `created_at`)
- **Primary keys:** `id` (INTEGER or TEXT depending on design)
- **Foreign keys:** `<referenced_table>_id`
- **Indexes:** `idx_<table>_<column>`

---

## 4) JSON / Network Contracts
- **Keys:** `snake_case`
- **Timestamps:** epoch ms (`created_at`, `updated_at`) or ISO 8601 (explicitly documented)
- **Booleans:** true/false (no "yes"/"no")
- **Error shape:** `{ "error": { "code": "string", "message": "human readable" } }`

---

## 5) Cryptography
- AES-GCM with 12-byte IV, 16-byte tag.
- **AAD** constant: `"journal-v1"` (string) — keep stable.
- Never log secrets, keys, or plaintext.

---

## 6) Testing & Logging
- **Android:** use instrumentation tests for JNI edges; unit tests for pure Kotlin.
- **Server:** pytest with factory fixtures.
- Log **actions**, not **secrets**. Prefer structured logs.

---

## 7) Comments & Docs
- Brief KDoc/Docstrings for public functions.
- Explain **why**, not what a line of code does when it’s obvious.

---

## 8) Dependency Policy
- Prefer **vanilla platform APIs**; add libs only when they buy real leverage (security, correctness, or big DX gains).
- Pin third-party versions; avoid transitive surprises.

---

## Quick naming recap
- Android Kotlin: follow Kotlin style (**camelCase for code**, **snake_case for resources**).
- Python & JSON: **snake_case**.
- SQL: **snake_case**.

> This explicitly **removes any prior repo preference for “camelCase everywhere.”**

