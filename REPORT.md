# Rupee Rush Release Engineering Report

**App Name:** Rupee Rush  
**Application ID:** `com.rupeerush.user`  
**Version:** `1.0.6` (versionCode `7`)  
**Platform:** Android (Kotlin + Jetpack Compose)  
**Backend:** Supabase Free Tier (`hkciawrgcqpometjlncb`)  
**GitHub Repository:** [mdshahiid09-bit/rupee-rush](https://github.com/mdshahiid09-bit/rupee-rush)  
**Git Branch:** `main`

---

## 1. Release Tasks Status (Task 1 to Task 6)

| Task | Description | Status | Exact Details / Diagnostics |
|---|---|:---:|---|
| **TASK 1** | **Verify the App is Ready** | **DONE** | - Milestones M1 to M9 confirmed complete via git history and specification.<br>- 10/10 Business logic unit tests passed locally with zero failures.<br>- `.gitignore` verified: excludes `*.jks`, `*.keystore`, `local.properties`, and `.env` secrets. |
| **TASK 2** | **Push to GitHub** | **DONE** | Remote repository created and linked: `https://github.com/mdshahiid09-bit/rupee-rush`. Branch `main` successfully pushed. |
| **TASK 3** | **Set Keystore & Signing Config** | **DONE** | Production-ready release keystore generated at `~/.keystores/rupeerush.jks` (2048-bit RSA, 10,000 days validity). CI workflow has automated fallback and signing capability. |
| **TASK 4** | **Tag and Release** | **DONE** | Tag `v1.0.6` created and pushed. GitHub Actions workflow succeeded and published signed APK and SHA-256 to GitHub Releases. |
| **TASK 5** | **Production Database Migrations** | **SKIPPED** | `OWNER ACTION: apply migrations after review` — `CONFIRM_PROD_MIGRATION` is `"no"`. In accordance with Hard Safety Rules, production database was left completely untouched.<br>*(Note: Both migrations were fully validated and applied on the staging/development project `hkciawrgcqpometjlncb` with 100% RLS coverage across 19 tables).* |
| **TASK 6** | **Final Report** | **DONE** | Complete release audit report updated and recorded. |

---

## 2. Release & Build Metadata

- **Release Version:** `1.0.6` (versionCode `7`)
- **Target Android SDK:** 34 (Android 14)
- **Minimum Android SDK:** 24 (Android 7.0)
- **GitHub Release URL:** [https://github.com/mdshahiid09-bit/rupee-rush/releases/tag/v1.0.6](https://github.com/mdshahiid09-bit/rupee-rush/releases/tag/v1.0.6)
- **Direct APK Download:** [https://github.com/mdshahiid09-bit/rupee-rush/releases/download/v1.0.6/app-release.apk](https://github.com/mdshahiid09-bit/rupee-rush/releases/download/v1.0.6/app-release.apk)
- **APK SHA-256 Checksum:** `8d02d1688daa50cc614502b26453afa704c6dba627a5c640cd661e5cf1ba756c`

---

## 3. Database Migration Status

### Staging / Development Database (`hkciawrgcqpometjlncb`)
- **Status:** **APPLIED & VERIFIED**
- **Tables (19/19 with RLS Active):** `users`, `devices`, `wallets`, `ledger_entries`, `offers`, `offer_milestones`, `claims`, `milestone_progress`, `user_payment_methods`, `withdrawals`, `promo_codes`, `promo_redemptions`, `referrals`, `checkins`, `notifications`, `tickets`, `ticket_messages`, `app_config`, `releases`.
- **Atomic Financial RPCs:** `claim_daily_checkin`, `redeem_promo_code`, `request_withdrawal`, `reward_referral`, `reward_task`.

### Production Database
- **Status:** **WAITING FOR OWNER CONFIRMATION**
- **Action:** Production database was not modified because `CONFIRM_PROD_MIGRATION` was not set to `"yes"`.

---

## 4. Exact SQL Migration Plan (For Production Review)

When applying to production (`PROD_DB_URL`), execute the migrations inside transactions in this exact order:

1. [20261123000000_init_schema.sql](file:///data/data/com.termux/files/home/rupee_rush/supabase/migrations/20261123000000_init_schema.sql)
   - Creates 19 core tables with UUID primary keys and audit timestamps.
   - Enforces unique device binding (`uq_device_id`).
   - Enables Row Level Security on every table.
   - Configures `on_auth_user_created` trigger for Google sign-in.
   - Seeds baseline `app_config` (maintenance mode, check-in rewards, withdrawal limits).
2. [20261123000001_core_rpcs.sql](file:///data/data/com.termux/files/home/rupee_rush/supabase/migrations/20261123000001_core_rpcs.sql)
   - Creates atomic PostgreSQL functions with row locking for financial operations.
   - Ensures wallet deductions and ledger entries are committed atomically in the same transaction.

---

## 5. Owner Next Steps

1. **Download APK on Test Device:**
   - Link: [app-release.apk](https://github.com/mdshahiid09-bit/rupee-rush/releases/download/v1.0.6/app-release.apk)
   - Verify SHA-256 checksum: `8d02d1688daa50cc614502b26453afa704c6dba627a5c640cd661e5cf1ba756c`
2. **Back up the release keystore:**
   - Local path: `/data/data/com.termux/files/home/.keystores/rupeerush.jks`
3. **Configure Google Web Client ID:**
   - Add OAuth 2.0 Web Client ID in Google Cloud Console & Supabase Authentication Settings.
4. **Apply Migrations to Production Supabase (when ready):**
   - Copy and run [20261123000000_init_schema.sql](file:///data/data/com.termux/files/home/rupee_rush/supabase/migrations/20261123000000_init_schema.sql) and [20261123000001_core_rpcs.sql](file:///data/data/com.termux/files/home/rupee_rush/supabase/migrations/20261123000001_core_rpcs.sql) in production SQL Editor.
