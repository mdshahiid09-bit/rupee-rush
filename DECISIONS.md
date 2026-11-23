# Rupee Rush Engineering Decisions Log

Every technical and architectural decision made autonomously is recorded here with reasons.

| Timestamp | Area | Decision | Reason |
|---|---|---|---|
| 2026-11-23 | Architecture | Kotlin + Jetpack Compose with Material 3 | Modern Android standard, matches hard constraints in spec. |
| 2026-11-23 | Database | Double-entry ledger with `wallets` and `ledger_entries` | Strict audit trail, balance cannot be manipulated directly by client. |
| 2026-11-23 | Backend | Atomic Postgres RPCs for checkin, promo, withdrawal, referral, task rewards | Prevents race conditions and guarantees transactional consistency on free plan. |
| 2026-11-23 | Updates | GitHub Releases via Supabase Edge Function | Keeps GitHub PAT secure, enables in-app updates without Google Play Store. |
| 2026-11-23 | Network | Ktor HTTP Client / Supabase-kt | Lightweight Kotlin-native SDK compatible with Android. |
| 2026-11-23 | Android Icons | Vector Rupee Rush badge in `ic_launcher_foreground.xml` | Self-contained vector asset with no external dependencies; prevents AAPT resource missing errors. |
| 2026-11-23 | CI/CD | `rootProject.file(...)` resolution for release keystore | Ensures multi-module Gradle subprojects correctly locate the keystore in CI and local builds. |

## MISSING ITEMS (To be provided by project owner)
- `GITHUB_RELEASE_TOKEN`: For Supabase Edge Function `app-update-check` to read private repo releases.
- `GOOGLE_WEB_CLIENT_ID`: OAuth 2.0 Web Client ID in Google Cloud Console for Supabase Google Sign-In.
- `DEFAULT_TEXT_TBD`: Default server notification template text.
