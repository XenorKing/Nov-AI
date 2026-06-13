---
name: Nov-AI Android project path
description: Real project is nested inside nov-ai-repo/nov-ai-repo/, not the partial copy at nov-ai-repo/app/
---

# Nov-AI Android project

**Why:** The repo was cloned into `nov-ai-repo/` but the actual Gradle project root is one level deeper at `nov-ai-repo/nov-ai-repo/`. There is also a `nov-ai-repo/app/` directory which is a partial/stale copy — any edits there have no effect on the build.

**How to apply:** Always target `nov-ai-repo/nov-ai-repo/app/src/main/java/com/novaproject/novai/` for source changes.

## Key paths
- Gradle root: `nov-ai-repo/nov-ai-repo/`
- Sources: `nov-ai-repo/nov-ai-repo/app/src/main/java/com/novaproject/novai/`
- Dependency catalog: `nov-ai-repo/nov-ai-repo/gradle/libs.versions.toml`
- App dependencies: `nov-ai-repo/nov-ai-repo/app/build.gradle.kts`
- Firestore rules: `nov-ai-repo/nov-ai-repo/firestore.rules`

## Security & Bug fixes applied (Jun 2026)

### Vuln 2 — OpenRouter token in Firestore plaintext
- Created `util/TokenSecureStore.kt` (EncryptedSharedPreferences, AES-256-GCM)
- Added `@get:com.google.firebase.firestore.Exclude` to `AISettings.openRouterToken`
- `ChatRepository.settingsFlow/getSettings/saveSettings` now read/write token via TokenSecureStore
- One-time migration: if local token empty, reads legacy value from Firestore raw data, saves locally, deletes from Firestore
- Added `androidx.security:security-crypto:1.0.0` dependency

### Vuln 3 — Response cache not cleared on logout
- `ChatViewModel.init` auth-state collector calls `repo.clearCache()` in the signed-out branch

### Vuln 5 — No SSL pinning
- `AppModule.provideOkHttpClient()` attaches `CertificatePinner` for `novai-proxy.xenortvin.workers.dev` in RELEASE builds only
- Pins Cloudflare E5/E6 intermediate CAs — **must verify with `openssl s_client` before shipping**

### Bug 3 — Token stats skipped for streaming responses
- In `ChatRepository.sendMessage` streaming branch: estimates prompt/completion tokens (chars/4) and calls `saveTokenUsage`

### Bug 4 — Search fires on every keystroke (no debounce)
- `ChatViewModel.searchMessages` now does `delay(300)` before querying Firestore

### Bug 5 — No indicator when AI context is truncated
- `ChatScreen` lazy list shows an info banner when `messages.size > 20` (AI only sees last 20)
