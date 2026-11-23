# MASTER PROMPT: Rupee Rush, USER APP (Android APK)

App name (display): Rupee Rush  
Package name (applicationId): com.rupeerush.user  

> **Note:** Build ONLY the user-facing Android app. Do not build any admin screen, admin login, admin route, or admin feature. The admin panel is a separate app that will be built later. Do not add any admin logic, admin key, or admin UI to this app.

---

## 1. Product Summary

Rupee Rush is an earning app. Users log in with Google, complete app-install tasks offered by the admin, earn INR rewards in a wallet, refer friends, do a daily check-in, and withdraw via UPI or Bank Transfer or redeem promo codes. Users can see a leaderboard. The app is distributed as an APK only (not Play Store) through Telegram or a direct link.

---

## 2. Hard Constraints (Must Follow)

- **Platform:** Android only. Kotlin + Jetpack Compose. Minimum SDK and target SDK chosen by the build; keep compatible with current Android versions.
- **Backend:** Supabase, FREE plan only. Keep storage, bandwidth, database size and Edge Function invocations within free limits. Do NOT store APK files in Supabase Storage. APK files come from GitHub Releases.
- **Language:** UI in English only. Notification text supports Hindi and English (see Notifications).
- **Secrets:** No secret may be inside the APK. This includes the GitHub token and the Supabase service_role key. Secrets stay only in Supabase Edge Function environment variables.
- **Build:** GitHub Actions builds the APK. The signing keystore comes from GitHub Secrets. Never commit the keystore or passwords to the repo.
- **Security model:** Row Level Security on every table. A user can read and write only their own rows. Wallet balances change only through server-side functions that write ledger entries. The app never writes a balance directly.
- **Distribution:** APK only. No Play Store specific code paths are required.

---

## 3. Screens and Navigation

Screens (in order of first launch):
1. Splash
2. Login (Google)
3. Onboarding (3 screens)
4. Permission requests (after onboarding, and when a feature needs them)
5. Home (main tab)
6. Offers (main tab)
7. Wallet (main tab)
8. Referral (from Home or Profile)
9. Leaderboard (from Home)
10. Profile (main tab)
11. Notifications list
12. Support and Tickets
13. Task detail, with Terms and Conditions on a full-screen page
14. Withdrawal screen and confirm dialog
15. Maintenance lock (full screen, overlays everything)
16. Blocked, suspended or banned screen (full screen, shown on login)
17. Offline screen
18. Error screen

Bottom navigation: Home, Offers, Wallet, Profile.  
Maintenance lock and blocked screens replace the whole navigation.

---

## 4. First Launch and Auth

- App opens on Splash, then Login.
- Login uses Google sign-in only.
- Login screen has a Terms and Privacy checkbox. The user must tick it before pressing login.
- After login, the app checks account status first:
  - If blocked, suspended or banned: show the Blocked screen and stop.
  - If maintenance lock is on: show the Maintenance lock screen and stop.
  - Otherwise continue to onboarding (first time) or Home (returning user).
- Onboarding has 3 screens, each with Skip and Next:
  1. How tasks work
  2. INR earning and UPI or bank withdrawal
  3. Why each permission is needed (4 topics across these screens: install detection, usage access, notifications, storage and install)
  - The last screen goes to Home.
- User session stays logged in permanently until the user logs out.
- Referral code is asked only at signup, not later. Allow entering a referral code during first signup only (or skip).

### Blocked, Suspended, Banned Screen
Shows:
- Status name (Banned, Suspended, or Permanently banned)
- Admin reason text
- Suspension end date (only for suspended)
- Support ticket link  
Balance is safe and visible only as "balance on hold" if the admin config allows it. Withdrawals are blocked.

### Maintenance Lock Screen
- Full-screen lock. No navigation, no other screen is reachable.
- Shows: app logo, simple animation, admin custom message, expected back time, support link.
- User can only view. No buttons except the support link.
- The lock shows even while the app is running. The app checks the lock state on open and periodically while open.
- When the admin lifts the lock, the app unlocks automatically without a restart.

---

## 5. Permissions

Required permissions (ask after login in onboarding, and again at update time if still missing):
- Package install detection (to detect when a task app is installed)
- Usage access (to show app usage time to the admin and to track task progress)
- Notifications (to show reminders and progress)
- Storage / files (to save downloaded APK)
- Unknown apps install (to install a downloaded APK)
- Foreground service (so a download continues in the background)
- Internet

Rules:
- If usage access or install detection is denied, a task cannot start. Show a clear reason and a button to grant it.
- Each permission screen explains in plain English why it is needed.
- Never silently request or use a permission the user did not allow.

---

## 6. Home

- Header: app name, and a profile icon that shows the first letter of the user's name in a circle.
- Balance card (total wallet balance, shown in INR). Glass-style card.
- Daily check-in button: one tap per day. Shows current streak. Bonus amount comes from server config.
- Quick links: Offers, Referral, Leaderboard.
- Leaderboard preview: top 3 users (name and total earning).
- Telegram channel link.
- Pull to refresh.
- Confetti runs on a successful daily check-in.

---

## 7. Offers and Tasks

### Offers Screen
- Top bar with a 3-dot menu. The menu opens the task tabs.
- Default list shows only Available offers (not yet started), as full-width cards, one below another.
- Card shows: app logo, title, bold highlighted prize. No status badge. No instruction line.
- Sort order is set by the admin in Config (the app follows the order the server sends).
- Shimmer loading on first load only, sized like the card, then fades into content.
- Card press uses a ripple effect.
- If an offer's daily cap is reached: show it greyed with "Kal aayega" (available tomorrow).
- Offers past their date window: tasks already started keep running to completion. The offer stops showing as available.
- Each user can take each offer only once.
- No limit on the number of ongoing tasks.
- Empty list: friendly message.

### Task Detail Screen
Shows:
- App name, logo, prize (bold).
- Steps: one step per card, in order.
- Milestones (if any): listed with progress. Daily earning milestone shows progress as a number (for example 3/7 days). Hidden milestones are not shown.
- A "Terms and Conditions" button opens a full-screen T&C page.
- A "Get Started" button.

### Get Started Flow
1. User taps Get Started.
2. The task moves to Ongoing.
3. The app records startedAt on the server.
4. User is redirected to the store or download link set by the admin. The package name is never shown to the user.
5. Install detection runs in real time.

### Install Detection
- Install detection is automatic and real time.
- An install counts only if it happened AFTER startedAt.
- A pre-installed app does not count.
- When install is detected, the task moves to Pending automatically. No "Task completed" button is needed.
- If the app is uninstalled during the task, flag the task for the admin. Do not auto-reject.
- Usage time is captured with usage access and sent to the server so the admin can see it live.

### Task Tabs (from the 3-dot menu)
Tab order: Ongoing (default), Pending, Completed, Rejected.
- **Ongoing:** started tasks not yet installed.
- **Pending:** installed tasks waiting for admin approval. There is NO countdown and NO auto-approval. The task stays Pending until the admin approves or rejects it. Show the label "installation is not payment". Include a link to Task Tracking.
- **Completed:** approved tasks, with the prize amount.
- **Rejected:** shows the rejection reason and a Retry button. Retry is allowed up to 3 times per offer. Each retry starts a new startedAt.
- Empty tab: friendly message with a button that goes to Offers.

### Milestones (optional per offer)
- Milestone types:
  - Step milestones (for example: create an account in the app, add a bank account in the app). These are verified by the admin.
  - Daily earning milestone (for example: earn a set amount per day for N days). The admin marks each day manually.
- Each milestone has its own bonus amount.
- Milestone reward is paid only when all milestones of the task are complete.
- Milestone progress is shown as a number (for example 3/7 days) and a progress bar.
- Disabled milestones: no bonus, not shown.
- Hidden milestones: not shown to the user, do not count, do not pay.

---

## 8. Wallet and Withdrawal

### Wallet Screen
- Total balance (INR) on a glass card.
- Balance history list (credits and debits, with date and reason).
- Withdraw button.
- Promo code input box.
- Withdrawal status list (simple label per request: Requested, Processing, Paid, Rejected).

### Rewards
- A task reward is added to balance only after admin approval.
- Pending rewards are shown separately as pending, not as balance.

### Withdrawal
- Methods: UPI and Bank Transfer. More methods can be added from the admin config later.
- UPI: user enters UPI ID. Verify the UPI ID before saving (show an error for invalid format or failed check).
- Bank Transfer: account holder name, account number, IFSC code. A Fetch button verifies the details and shows an error for wrong details.
- User can save only one UPI and one bank account at a time.
- Amount entry checks minimum and maximum (from server config). Maximum is tied to the daily limit.
- Daily limit applies to total amount per day and number of requests per day (both from server config).
- Confirm dialog shows the amount and the method. User presses Confirm.
- On confirm: the amount is deducted from the wallet immediately and a withdrawal request is created with the hold period set by the admin.
- Status shows as a simple label only.
- If the admin rejects, the amount is returned to the wallet immediately.
- User can delete the account (see Profile) even with a pending withdrawal. Pending withdrawal is handled manually by the admin.

### Promo Code
- Input box in Wallet. User types the code that the admin shared on Telegram.
- A valid code credits the wallet balance.
- Codes are never shown inside the app.
- One code can be used by many users, up to the total limit set by the admin. A user can use each code once.
- Daily redeem limit per user comes from server config.
- Show clear error messages: invalid code, expired code, limit reached, already used.

---

## 9. Referral (Refer & Earn)

- Each user has a referral code (shown in Profile and Referral screen).
- A referral code can be entered only at signup.
- Referrer gets a fixed rupee bonus (value from server config). The bonus is credited when the referee's first task is approved by the admin.
- Referee also gets a signup bonus (value from server config).
- No referral chain. Only direct referrals earn.
- No cap on the number of referral bonuses per user.
- Referral screen shows: total referrals, total referral earnings, and the list of referred friends with their FULL names.
- Share options: copy code, WhatsApp share, system share menu.

---

## 10. Daily Check-in and Streak

- Check-in is allowed once per calendar day (user's local time).
- Bonus amount comes from server config. Admin can change it, and the new amount applies to all users immediately.
- Streak: consecutive days of check-in. If a day is missed, the streak resets to zero.
- Confetti on a successful check-in.

---

## 11. Leaderboard

- Top 50 users ranked by total earning (all-time). Ties: the user who reached that total first ranks higher.
- Shows rank, name, total earning.
- Current user's rank is highlighted if they are in the top 50.
- Admin can remove a user from the leaderboard (this happens on the server; the app just does not show removed users).

---

## 12. Profile

Shows:
- Google photo and name
- Nickname with an edit button. Nickname can be changed any time (no change limit). Rules: 3 to 15 characters, must be unique, bad words are blocked (server check).
- Joined date
- Referral code (copy)
- Telegram channel link

Settings:
- Notifications: a separate toggle for each type (task, payment, reminder).
- Theme: Light, Dark, or System (saved on the device).
- Terms and Conditions
- Privacy Policy (shown inside the app as a page)
- Support
- Logout (with confirmation)
- Delete account (with confirmation). After delete request, user data is removed after 30 days. Withdrawal and payment records stay.

---

## 13. Support and Tickets

- Raise a Ticket: user picks a category, writes a message, and submits.
- Ticket list shows status and admin replies.
- A ticket can link to a task or a withdrawal (optional).
- Telegram channel link is also shown on the Support screen.
- No time limit is promised for replies.

---

## 14. Notifications

### In-App Notifications List
- Each notification shows: title, message, optional image, optional link.
- Notifications stay in the list until the user deletes them.
- User can delete one by one, or use "Delete all" (with confirmation).
- Money-related notifications (payments, withdrawals, rewards) remain in Transactions even if deleted from the list.

### Local Notifications (made by the app, no server)
- Daily check-in reminder: time set by the server config (admin sets one time for all users).
- Streak warning: sent at night if the user has not checked in that day.
- Milestone progress: shown when the admin marks a milestone day.
- Install and download progress: a progress bar notification with status (Downloading, Installing, Done).
- Tapping any notification opens the correct screen.
- Default sound.
- Notification permission is asked after onboarding.

### Server Notifications (in-app list only, no push)
- Withdrawal status changes and admin broadcasts appear in the in-app list only.
- Default text for these is fixed in the app. Use the placeholder `[DEFAULT_TEXT_TBD]` and keep it in one config file so it is easy to change.
- Notifications support Hindi and English text. The app shows the text in the user's selected language (Hindi or English), with English as fallback.
- Emoji allowed in text.

### Notification Settings (Profile)
- One toggle per type: task updates, payments, reminders.

---

## 15. App Update (GitHub Release)

- Every time the app opens, check for a new version.
- The check calls a Supabase Edge Function. The function holds the GitHub token (repo is private) and returns: latest version, release notes, APK download URL, and whether the update is optional or forced.
- The GitHub token must never be inside the APK.
- Only normal (non-pre-release) GitHub releases are shown.
- Optional update: user can close and continue.
- Forced update: user must update before using the app. Show a full-screen message.
- Download inside the app:
  - Circular progress indicator with percent.
  - Pause and resume.
  - If the network changes, wait and resume.
  - Runs as a foreground service so it continues when the app is in the background.
  - Progress shows in the notification.
  - Checksum verification after download (SHA-256 provided by the Edge Function). If it fails, delete the file and retry.
  - Automatic retry on failure (with a short backoff).
- After download, open the Android install dialog. The user must confirm once.
- After install, the new version opens automatically.

---

## 16. Error, Offline and Empty States

- Error: simple message with a Retry button. No technical details shown to the user.
- No internet: offline screen with a Retry button. Show last cached data if available (balance, offers list) but mark it as cached.
- Empty states: friendly message with a call to action (for example "No offers right now" with a button to refresh).

---

## 17. Visual Design and Animations

- Glass-style cards on all screens (Home, Offers, Wallet, Profile, Referral, Leaderboard).
- Shimmer loading on the first list load only.
- Confetti on: install complete, task approved, withdrawal paid, daily check-in.
- Ripple effect on card press.
- Animations are slow and smooth (around 800ms or more).
- Same effects in light and dark mode. Dark mode uses the same glass and glow style.
- No font size or font style setting.
- No ads anywhere in the app.

---

## 18. Data Model & Architecture (Supabase PostgreSQL)

1. `users`: id, google_id, email, name, nickname, photo_url, referral_code, referred_by, status (active, suspended, banned), suspended_until, created_at, delete_requested_at
2. `devices`: id, user_id, device_id (unique per user; one device, one account rule)
3. `wallets`: id, user_id, balance, total_earned, updated_at
4. `ledger_entries`: id, wallet_id, user_id, type (credit, debit), amount, reason, reference_id, created_at
5. `offers`: id, app_name, logo_url, play_or_download_url, package_name, prize, steps (jsonb), terms_text, is_published, daily_cap, starts_at, ends_at, created_at, updated_at
6. `offer_milestones`: id, offer_id, type (step, daily), title, bonus_amount, days_required, is_enabled, is_visible
7. `claims`: id, user_id, offer_id, status (ongoing, pending, completed, rejected, flagged), started_at, installed_at, usage_seconds, retry_count, rejection_reason
8. `milestone_progress`: id, claim_id, milestone_id, day_number, marked_by_admin, marked_at
9. `withdrawals`: id, user_id, amount, method (upi, bank), account_snapshot (jsonb), status (requested, processing, paid, rejected), hold_until, created_at
10. `user_payment_methods`: id, user_id, type (upi, bank), details (jsonb), updated_at
11. `promo_codes`: id, code, amount, total_limit, used_count, daily_limit_per_user, is_active, expires_at
12. `promo_redemptions`: id, promo_code_id, user_id, redeemed_at
13. `referrals`: id, referrer_id, referee_id, status (signed_up, rewarded), bonus_amount, rewarded_at
14. `checkins`: id, user_id, check_date, streak_count, bonus_amount, created_at
15. `notifications`: id, user_id (nullable for broadcast), type, title_en, title_hi, message_en, message_hi, image_url, link, created_at, deleted_at
16. `tickets`: id, user_id, category, subject, status (open, in_progress, resolved, closed), related_task_id, related_withdrawal_id, created_at
17. `ticket_messages`: id, ticket_id, sender_id, is_admin, message, created_at
18. `app_config`: key (text primary key), value (jsonb), description (text), updated_at
19. `releases`: version, notes, apk_url, sha256, is_forced, is_prerelease, published_at

---

## 19. Security and Fraud Rules

- One device, one account: check device ID at login. A second account on the same device is blocked.
- Install must be after `startedAt`. Pre-installed apps do not count.
- Suspicious activity is flagged for the admin. Do not auto-decide.
- Sensitive writes (withdrawal request, promo redeem, check-in bonus, referral reward, task reward) run through server functions with server-side validation. The client cannot send a final amount.
- Rate limits on sensitive functions to stop abuse.
- No secret inside the app. Keep only the public Supabase URL and anon key in the app.

---

## 20. Open Items & Placeholders

- Default notification text: `[DEFAULT_TEXT_TBD]`
- Reward Hub: not in this build. Leave the menu entry out.
- Any feature not listed above: do not add it. Ask the owner first.
