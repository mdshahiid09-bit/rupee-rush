-- Migration: 20261123000000_init_schema.sql
-- Description: Rupee Rush Core Schema, Tables, RLS, and Ledger Security

-- 1. Enable pgcrypto
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ============================================================================
-- 2. USERS & PROFILES
-- ============================================================================
CREATE TABLE IF NOT EXISTS public.users (
    id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    google_id TEXT,
    email TEXT,
    name TEXT,
    nickname TEXT UNIQUE,
    photo_url TEXT,
    referral_code TEXT UNIQUE,
    referred_by UUID REFERENCES public.users(id) ON DELETE SET NULL,
    status TEXT NOT NULL DEFAULT 'active' CHECK (status IN ('active', 'suspended', 'banned')),
    suspended_until TIMESTAMPTZ,
    delete_requested_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_users_referral_code ON public.users(referral_code);
CREATE INDEX IF NOT EXISTS idx_users_status ON public.users(status);

-- ============================================================================
-- 3. DEVICES (One Device, One Account rule)
-- ============================================================================
CREATE TABLE IF NOT EXISTS public.devices (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
    device_id TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_device_id UNIQUE (device_id),
    CONSTRAINT uq_user_device UNIQUE (user_id, device_id)
);

-- ============================================================================
-- 4. WALLETS & LEDGER (Strict double-entry ledger security)
-- ============================================================================
CREATE TABLE IF NOT EXISTS public.wallets (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL UNIQUE REFERENCES public.users(id) ON DELETE CASCADE,
    balance NUMERIC(12, 2) NOT NULL DEFAULT 0.00 CHECK (balance >= 0),
    total_earned NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS public.ledger_entries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    wallet_id UUID NOT NULL REFERENCES public.wallets(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
    type TEXT NOT NULL CHECK (type IN ('credit', 'debit')),
    amount NUMERIC(12, 2) NOT NULL CHECK (amount > 0),
    reason TEXT NOT NULL,
    reference_id TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_ledger_user ON public.ledger_entries(user_id);

-- ============================================================================
-- 5. OFFERS & MILESTONES
-- ============================================================================
CREATE TABLE IF NOT EXISTS public.offers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    app_name TEXT NOT NULL,
    logo_url TEXT,
    play_or_download_url TEXT NOT NULL,
    package_name TEXT NOT NULL,
    prize NUMERIC(10, 2) NOT NULL CHECK (prize >= 0),
    steps JSONB NOT NULL DEFAULT '[]'::JSONB,
    terms_text TEXT,
    is_published BOOLEAN NOT NULL DEFAULT FALSE,
    daily_cap INT NOT NULL DEFAULT 0,
    sort_order INT NOT NULL DEFAULT 0,
    starts_at TIMESTAMPTZ,
    ends_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS public.offer_milestones (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    offer_id UUID NOT NULL REFERENCES public.offers(id) ON DELETE CASCADE,
    type TEXT NOT NULL CHECK (type IN ('step', 'daily')),
    title TEXT NOT NULL,
    bonus_amount NUMERIC(10, 2) NOT NULL DEFAULT 0.00,
    days_required INT NOT NULL DEFAULT 0,
    is_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    is_visible BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ============================================================================
-- 6. CLAIMS & TRACKING
-- ============================================================================
CREATE TABLE IF NOT EXISTS public.claims (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
    offer_id UUID NOT NULL REFERENCES public.offers(id) ON DELETE CASCADE,
    status TEXT NOT NULL DEFAULT 'ongoing' CHECK (status IN ('ongoing', 'pending', 'completed', 'rejected', 'flagged')),
    started_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    installed_at TIMESTAMPTZ,
    usage_seconds INT NOT NULL DEFAULT 0,
    retry_count INT NOT NULL DEFAULT 0,
    rejection_reason TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_user_offer UNIQUE (user_id, offer_id)
);

CREATE TABLE IF NOT EXISTS public.milestone_progress (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    claim_id UUID NOT NULL REFERENCES public.claims(id) ON DELETE CASCADE,
    milestone_id UUID NOT NULL REFERENCES public.offer_milestones(id) ON DELETE CASCADE,
    day_number INT NOT NULL DEFAULT 0,
    marked_by_admin BOOLEAN NOT NULL DEFAULT FALSE,
    marked_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ============================================================================
-- 7. PAYMENT METHODS & WITHDRAWALS
-- ============================================================================
CREATE TABLE IF NOT EXISTS public.user_payment_methods (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
    type TEXT NOT NULL CHECK (type IN ('upi', 'bank')),
    details JSONB NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_user_payment_type UNIQUE (user_id, type)
);

CREATE TABLE IF NOT EXISTS public.withdrawals (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
    amount NUMERIC(10, 2) NOT NULL CHECK (amount > 0),
    method TEXT NOT NULL CHECK (method IN ('upi', 'bank')),
    account_snapshot JSONB NOT NULL,
    status TEXT NOT NULL DEFAULT 'requested' CHECK (status IN ('requested', 'processing', 'paid', 'rejected')),
    hold_until TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_withdrawals_user ON public.withdrawals(user_id);

-- ============================================================================
-- 8. PROMO CODES
-- ============================================================================
CREATE TABLE IF NOT EXISTS public.promo_codes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code TEXT NOT NULL UNIQUE,
    amount NUMERIC(10, 2) NOT NULL CHECK (amount > 0),
    total_limit INT NOT NULL DEFAULT 100,
    used_count INT NOT NULL DEFAULT 0,
    daily_limit_per_user INT NOT NULL DEFAULT 1,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    expires_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS public.promo_redemptions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    promo_code_id UUID NOT NULL REFERENCES public.promo_codes(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
    redeemed_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ============================================================================
-- 9. REFERRALS & CHECK-INS
-- ============================================================================
CREATE TABLE IF NOT EXISTS public.referrals (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    referrer_id UUID NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
    referee_id UUID NOT NULL UNIQUE REFERENCES public.users(id) ON DELETE CASCADE,
    status TEXT NOT NULL DEFAULT 'signed_up' CHECK (status IN ('signed_up', 'rewarded')),
    bonus_amount NUMERIC(10, 2) NOT NULL DEFAULT 0.00,
    rewarded_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS public.checkins (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
    check_date DATE NOT NULL,
    streak_count INT NOT NULL DEFAULT 1,
    bonus_amount NUMERIC(10, 2) NOT NULL DEFAULT 0.00,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_user_checkin_date UNIQUE (user_id, check_date)
);

-- ============================================================================
-- 10. NOTIFICATIONS & TICKETS
-- ============================================================================
CREATE TABLE IF NOT EXISTS public.notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES public.users(id) ON DELETE CASCADE, -- NULL means global broadcast
    type TEXT NOT NULL,
    title_en TEXT NOT NULL,
    title_hi TEXT,
    message_en TEXT NOT NULL,
    message_hi TEXT,
    image_url TEXT,
    link TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS public.tickets (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
    category TEXT NOT NULL,
    subject TEXT NOT NULL,
    status TEXT NOT NULL DEFAULT 'open' CHECK (status IN ('open', 'in_progress', 'resolved', 'closed')),
    related_task_id UUID REFERENCES public.claims(id) ON DELETE SET NULL,
    related_withdrawal_id UUID REFERENCES public.withdrawals(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS public.ticket_messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ticket_id UUID NOT NULL REFERENCES public.tickets(id) ON DELETE CASCADE,
    sender_id UUID NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
    is_admin BOOLEAN NOT NULL DEFAULT FALSE,
    message TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ============================================================================
-- 11. APP CONFIG & RELEASES
-- ============================================================================
CREATE TABLE IF NOT EXISTS public.app_config (
    key TEXT PRIMARY KEY,
    value JSONB NOT NULL,
    description TEXT,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS public.releases (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    version TEXT NOT NULL,
    notes TEXT,
    apk_url TEXT NOT NULL,
    sha256 TEXT NOT NULL,
    is_forced BOOLEAN NOT NULL DEFAULT FALSE,
    is_prerelease BOOLEAN NOT NULL DEFAULT FALSE,
    published_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ============================================================================
-- 12. ROW LEVEL SECURITY (RLS) POLICIES
-- ============================================================================
ALTER TABLE public.users ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.devices ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.wallets ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.ledger_entries ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.offers ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.offer_milestones ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.claims ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.milestone_progress ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.user_payment_methods ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.withdrawals ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.promo_codes ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.promo_redemptions ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.referrals ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.checkins ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.notifications ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.tickets ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.ticket_messages ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.app_config ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.releases ENABLE ROW LEVEL SECURITY;

-- Users policies
CREATE POLICY "Users can view own profile" ON public.users FOR SELECT USING (auth.uid() = id);
CREATE POLICY "Users can update own nickname/settings" ON public.users FOR UPDATE USING (auth.uid() = id);

-- Devices policies
CREATE POLICY "Users can view own device" ON public.devices FOR SELECT USING (auth.uid() = user_id);
CREATE POLICY "Users can insert own device" ON public.devices FOR INSERT WITH CHECK (auth.uid() = user_id);

-- Wallets & Ledger policies (READ ONLY for client)
CREATE POLICY "Users can view own wallet" ON public.wallets FOR SELECT USING (auth.uid() = user_id);
CREATE POLICY "Users can view own ledger" ON public.ledger_entries FOR SELECT USING (auth.uid() = user_id);

-- Offers & Milestones (Public read for active/published)
CREATE POLICY "Users can view published offers" ON public.offers FOR SELECT USING (is_published = TRUE);
CREATE POLICY "Users can view visible milestones" ON public.offer_milestones FOR SELECT USING (is_enabled = TRUE AND is_visible = TRUE);

-- Claims & Milestone Progress
CREATE POLICY "Users can view own claims" ON public.claims FOR SELECT USING (auth.uid() = user_id);
CREATE POLICY "Users can create own claim" ON public.claims FOR INSERT WITH CHECK (auth.uid() = user_id);
CREATE POLICY "Users can view own milestone progress" ON public.milestone_progress FOR SELECT 
    USING (EXISTS (SELECT 1 FROM public.claims WHERE claims.id = milestone_progress.claim_id AND claims.user_id = auth.uid()));

-- Payment methods policies
CREATE POLICY "Users can manage own payment methods" ON public.user_payment_methods FOR ALL USING (auth.uid() = user_id);

-- Withdrawals policies (View own, create via function)
CREATE POLICY "Users can view own withdrawals" ON public.withdrawals FOR SELECT USING (auth.uid() = user_id);

-- Promo redemptions
CREATE POLICY "Users can view own redemptions" ON public.promo_redemptions FOR SELECT USING (auth.uid() = user_id);

-- Referrals policies
CREATE POLICY "Users can view own referrals" ON public.referrals FOR SELECT USING (auth.uid() = referrer_id OR auth.uid() = referee_id);

-- Check-ins policies
CREATE POLICY "Users can view own checkins" ON public.checkins FOR SELECT USING (auth.uid() = user_id);

-- Notifications policies
CREATE POLICY "Users can view own or broadcast notifications" ON public.notifications FOR SELECT 
    USING ((user_id = auth.uid() OR user_id IS NULL) AND deleted_at IS NULL);
CREATE POLICY "Users can soft-delete own notifications" ON public.notifications FOR UPDATE 
    USING (user_id = auth.uid() OR user_id IS NULL);

-- Tickets policies
CREATE POLICY "Users can manage own tickets" ON public.tickets FOR ALL USING (auth.uid() = user_id);
CREATE POLICY "Users can manage own ticket messages" ON public.ticket_messages FOR ALL 
    USING (EXISTS (SELECT 1 FROM public.tickets WHERE tickets.id = ticket_messages.ticket_id AND tickets.user_id = auth.uid()));

-- App config & Releases (Read only for authenticated users)
CREATE POLICY "Public read app config" ON public.app_config FOR SELECT TO authenticated USING (TRUE);
CREATE POLICY "Public read releases" ON public.releases FOR SELECT TO authenticated USING (is_prerelease = FALSE);

-- ============================================================================
-- 13. AUTH HOOK / USER ONBOARDING TRIGGER
-- ============================================================================
CREATE OR REPLACE FUNCTION public.handle_new_user()
RETURNS TRIGGER AS $$
DECLARE
    new_referral_code TEXT;
BEGIN
    new_referral_code := UPPER(SUBSTRING(COALESCE(NEW.raw_user_meta_data->>'name', 'RR') FROM 1 FOR 3) || SUBSTRING(MD5(RANDOM()::TEXT) FROM 1 FOR 4));

    INSERT INTO public.users (
        id,
        google_id,
        email,
        name,
        nickname,
        photo_url,
        referral_code,
        status,
        created_at
    ) VALUES (
        NEW.id,
        NEW.raw_user_meta_data->>'sub',
        NEW.email,
        NEW.raw_user_meta_data->>'full_name',
        NULL,
        NEW.raw_user_meta_data->>'avatar_url',
        new_referral_code,
        'active',
        NOW()
    );

    INSERT INTO public.wallets (
        user_id,
        balance,
        total_earned
    ) VALUES (
        NEW.id,
        0.00,
        0.00
    );

    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

DROP TRIGGER IF EXISTS on_auth_user_created ON auth.users;
CREATE TRIGGER on_auth_user_created
    AFTER INSERT ON auth.users
    FOR EACH ROW EXECUTE FUNCTION public.handle_new_user();

-- ============================================================================
-- 14. DEFAULT APP CONFIG
-- ============================================================================
INSERT INTO public.app_config (key, value, description)
VALUES 
    ('maintenance_mode', '{"enabled": false, "message": "Rupee Rush is undergoing scheduled maintenance.", "expected_back_time": null, "support_link": "https://t.me/rupeerush"}'::jsonb, 'Maintenance lock state and details'),
    ('daily_checkin', '{"bonus_base": 1.00, "bonus_streak_increment": 0.50, "max_daily_bonus": 10.00, "reminder_hour": 20}'::jsonb, 'Check-in rewards and reminder timing'),
    ('referral_config', '{"referrer_bonus": 10.00, "referee_signup_bonus": 5.00, "reward_on_first_task": true}'::jsonb, 'Refer & earn reward configuration'),
    ('withdrawal_config', '{"min_amount": 50.00, "max_amount": 1000.00, "daily_max_amount": 2000.00, "daily_request_limit": 2, "hold_hours": 24}'::jsonb, 'Withdrawal thresholds and limits'),
    ('telegram_channel', '{"url": "https://t.me/rupeerush", "channel_name": "Rupee Rush Official"}'::jsonb, 'Official community channel link')
ON CONFLICT (key) DO NOTHING;

