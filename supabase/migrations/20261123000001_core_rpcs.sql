-- Migration: 20261123000001_core_rpcs.sql
-- Description: Atomic Postgres Functions for Financial and Gamification Security

-- ============================================================================
-- 1. CLAIM DAILY CHECK-IN
-- ============================================================================
CREATE OR REPLACE FUNCTION public.claim_daily_checkin(p_user_id UUID)
RETURNS JSONB AS $$
DECLARE
    v_today DATE := CURRENT_DATE;
    v_last_checkin RECORD;
    v_new_streak INT := 1;
    v_config JSONB;
    v_base_bonus NUMERIC(10, 2);
    v_increment NUMERIC(10, 2);
    v_max_bonus NUMERIC(10, 2);
    v_calculated_bonus NUMERIC(10, 2);
    v_wallet_id UUID;
    v_new_balance NUMERIC(12, 2);
BEGIN
    -- Verify user matches or is security definer
    IF auth.uid() IS NOT NULL AND auth.uid() != p_user_id THEN
        RAISE EXCEPTION 'Unauthorized: Cannot claim check-in for another user';
    END IF;

    -- Check if already checked in today
    IF EXISTS (SELECT 1 FROM public.checkins WHERE user_id = p_user_id AND check_date = v_today) THEN
        RETURN jsonb_build_object(
            'success', FALSE,
            'message', 'Already checked in today'
        );
    END IF;

    -- Get last check-in to compute streak
    SELECT check_date, streak_count INTO v_last_checkin 
    FROM public.checkins 
    WHERE user_id = p_user_id 
    ORDER BY check_date DESC 
    LIMIT 1;

    IF FOUND THEN
        IF v_last_checkin.check_date = v_today - 1 THEN
            v_new_streak := v_last_checkin.streak_count + 1;
        ELSE
            v_new_streak := 1;
        END IF;
    END IF;

    -- Read check-in config from app_config
    SELECT value INTO v_config FROM public.app_config WHERE key = 'daily_checkin';
    v_base_bonus := COALESCE((v_config->>'bonus_base')::NUMERIC, 1.00);
    v_increment := COALESCE((v_config->>'bonus_streak_increment')::NUMERIC, 0.50);
    v_max_bonus := COALESCE((v_config->>'max_daily_bonus')::NUMERIC, 10.00);

    v_calculated_bonus := LEAST(v_base_bonus + ((v_new_streak - 1) * v_increment), v_max_bonus);

    -- Insert checkin record
    INSERT INTO public.checkins (user_id, check_date, streak_count, bonus_amount)
    VALUES (p_user_id, v_today, v_new_streak, v_calculated_bonus);

    -- Credit wallet atomically
    UPDATE public.wallets 
    SET balance = balance + v_calculated_bonus,
        total_earned = total_earned + v_calculated_bonus,
        updated_at = NOW()
    WHERE user_id = p_user_id
    RETURNING id, balance INTO v_wallet_id, v_new_balance;

    -- Write ledger entry
    INSERT INTO public.ledger_entries (wallet_id, user_id, type, amount, reason, reference_id)
    VALUES (v_wallet_id, p_user_id, 'credit', v_calculated_bonus, 'daily_checkin', 'streak_' || v_new_streak);

    RETURN jsonb_build_object(
        'success', TRUE,
        'streak_count', v_new_streak,
        'bonus_amount', v_calculated_bonus,
        'new_balance', v_new_balance
    );
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- ============================================================================
-- 2. REDEEM PROMO CODE
-- ============================================================================
CREATE OR REPLACE FUNCTION public.redeem_promo_code(p_user_id UUID, p_code TEXT)
RETURNS JSONB AS $$
DECLARE
    v_promo RECORD;
    v_user_today_count INT;
    v_wallet_id UUID;
    v_new_balance NUMERIC(12, 2);
    v_clean_code TEXT := UPPER(TRIM(p_code));
BEGIN
    IF auth.uid() IS NOT NULL AND auth.uid() != p_user_id THEN
        RAISE EXCEPTION 'Unauthorized: Cannot redeem promo for another user';
    END IF;

    -- Find promo code with row lock
    SELECT * INTO v_promo 
    FROM public.promo_codes 
    WHERE code = v_clean_code
    FOR UPDATE;

    IF NOT FOUND THEN
        RETURN jsonb_build_object('success', FALSE, 'message', 'Invalid promo code');
    END IF;

    IF NOT v_promo.is_active THEN
        RETURN jsonb_build_object('success', FALSE, 'message', 'Promo code is inactive');
    END IF;

    IF v_promo.expires_at IS NOT NULL AND v_promo.expires_at < NOW() THEN
        RETURN jsonb_build_object('success', FALSE, 'message', 'Promo code has expired');
    END IF;

    IF v_promo.used_count >= v_promo.total_limit THEN
        RETURN jsonb_build_object('success', FALSE, 'message', 'Promo code redemption limit reached');
    END IF;

    -- Check if user already redeemed this promo
    IF EXISTS (SELECT 1 FROM public.promo_redemptions WHERE promo_code_id = v_promo.id AND user_id = p_user_id) THEN
        RETURN jsonb_build_object('success', FALSE, 'message', 'You have already redeemed this promo code');
    END IF;

    -- Check user daily limit across all promos
    SELECT COUNT(*) INTO v_user_today_count 
    FROM public.promo_redemptions 
    WHERE user_id = p_user_id AND redeemed_at >= CURRENT_DATE::TIMESTAMPTZ;

    IF v_user_today_count >= v_promo.daily_limit_per_user THEN
        RETURN jsonb_build_object('success', FALSE, 'message', 'Daily promo redemption limit reached');
    END IF;

    -- Update promo code used count
    UPDATE public.promo_codes 
    SET used_count = used_count + 1 
    WHERE id = v_promo.id;

    -- Insert redemption
    INSERT INTO public.promo_redemptions (promo_code_id, user_id)
    VALUES (v_promo.id, p_user_id);

    -- Credit wallet
    UPDATE public.wallets 
    SET balance = balance + v_promo.amount,
        total_earned = total_earned + v_promo.amount,
        updated_at = NOW()
    WHERE user_id = p_user_id
    RETURNING id, balance INTO v_wallet_id, v_new_balance;

    -- Ledger
    INSERT INTO public.ledger_entries (wallet_id, user_id, type, amount, reason, reference_id)
    VALUES (v_wallet_id, p_user_id, 'credit', v_promo.amount, 'promo_code', v_clean_code);

    RETURN jsonb_build_object(
        'success', TRUE,
        'amount', v_promo.amount,
        'new_balance', v_new_balance
    );
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- ============================================================================
-- 3. REQUEST WITHDRAWAL (Atomic deduct & ledger in same transaction)
-- ============================================================================
CREATE OR REPLACE FUNCTION public.request_withdrawal(
    p_user_id UUID,
    p_amount NUMERIC(10, 2),
    p_method TEXT,
    p_account_snapshot JSONB
)
RETURNS JSONB AS $$
DECLARE
    v_config JSONB;
    v_min_amount NUMERIC(10, 2);
    v_max_amount NUMERIC(10, 2);
    v_daily_max NUMERIC(10, 2);
    v_daily_limit_count INT;
    v_hold_hours INT;
    v_today_total NUMERIC(10, 2);
    v_today_count INT;
    v_current_balance NUMERIC(12, 2);
    v_wallet_id UUID;
    v_withdrawal_id UUID;
    v_hold_until TIMESTAMPTZ;
BEGIN
    IF auth.uid() IS NOT NULL AND auth.uid() != p_user_id THEN
        RAISE EXCEPTION 'Unauthorized: Cannot request withdrawal for another user';
    END IF;

    IF p_amount <= 0 THEN
        RETURN jsonb_build_object('success', FALSE, 'message', 'Invalid withdrawal amount');
    END IF;

    IF p_method NOT IN ('upi', 'bank') THEN
        RETURN jsonb_build_object('success', FALSE, 'message', 'Unsupported withdrawal method');
    END IF;

    -- Read withdrawal config
    SELECT value INTO v_config FROM public.app_config WHERE key = 'withdrawal_config';
    v_min_amount := COALESCE((v_config->>'min_amount')::NUMERIC, 50.00);
    v_max_amount := COALESCE((v_config->>'max_amount')::NUMERIC, 1000.00);
    v_daily_max := COALESCE((v_config->>'daily_max_amount')::NUMERIC, 2000.00);
    v_daily_limit_count := COALESCE((v_config->>'daily_request_limit')::INT, 2);
    v_hold_hours := COALESCE((v_config->>'hold_hours')::INT, 24);

    -- Validate bounds
    IF p_amount < v_min_amount THEN
        RETURN jsonb_build_object('success', FALSE, 'message', 'Amount is less than minimum withdrawal of ₹' || v_min_amount);
    END IF;

    IF p_amount > v_max_amount THEN
        RETURN jsonb_build_object('success', FALSE, 'message', 'Amount exceeds maximum per request of ₹' || v_max_amount);
    END IF;

    -- Check daily count and amount
    SELECT COALESCE(SUM(amount), 0), COUNT(*) 
    INTO v_today_total, v_today_count 
    FROM public.withdrawals 
    WHERE user_id = p_user_id 
      AND created_at >= CURRENT_DATE::TIMESTAMPTZ 
      AND status != 'rejected';

    IF v_today_count >= v_daily_limit_count THEN
        RETURN jsonb_build_object('success', FALSE, 'message', 'Daily withdrawal count limit reached (' || v_daily_limit_count || '/day)');
    END IF;

    IF (v_today_total + p_amount) > v_daily_max THEN
        RETURN jsonb_build_object('success', FALSE, 'message', 'Daily withdrawal volume limit of ₹' || v_daily_max || ' exceeded');
    END IF;

    -- Lock wallet row
    SELECT id, balance INTO v_wallet_id, v_current_balance 
    FROM public.wallets 
    WHERE user_id = p_user_id 
    FOR UPDATE;

    IF v_current_balance < p_amount THEN
        RETURN jsonb_build_object('success', FALSE, 'message', 'Insufficient wallet balance');
    END IF;

    -- Immediate deduction
    UPDATE public.wallets 
    SET balance = balance - p_amount,
        updated_at = NOW()
    WHERE id = v_wallet_id;

    v_hold_until := NOW() + (v_hold_hours * INTERVAL '1 hour');

    -- Create withdrawal request
    INSERT INTO public.withdrawals (user_id, amount, method, account_snapshot, status, hold_until)
    VALUES (p_user_id, p_amount, p_method, p_account_snapshot, 'requested', v_hold_until)
    RETURNING id INTO v_withdrawal_id;

    -- Write debit ledger entry
    INSERT INTO public.ledger_entries (wallet_id, user_id, type, amount, reason, reference_id)
    VALUES (v_wallet_id, p_user_id, 'debit', p_amount, 'withdrawal_request', v_withdrawal_id::TEXT);

    RETURN jsonb_build_object(
        'success', TRUE,
        'withdrawal_id', v_withdrawal_id,
        'amount', p_amount,
        'status', 'requested',
        'hold_until', v_hold_until,
        'remaining_balance', v_current_balance - p_amount
    );
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- ============================================================================
-- 4. REWARD REFERRAL (When referee completes first task)
-- ============================================================================
CREATE OR REPLACE FUNCTION public.reward_referral(p_referee_id UUID)
RETURNS JSONB AS $$
DECLARE
    v_ref RECORD;
    v_config JSONB;
    v_referrer_bonus NUMERIC(10, 2);
    v_referee_bonus NUMERIC(10, 2);
    v_referrer_wallet UUID;
    v_referee_wallet UUID;
BEGIN
    SELECT * INTO v_ref 
    FROM public.referrals 
    WHERE referee_id = p_referee_id AND status = 'signed_up'
    FOR UPDATE;

    IF NOT FOUND THEN
        RETURN jsonb_build_object('success', FALSE, 'message', 'No pending referral found');
    END IF;

    SELECT value INTO v_config FROM public.app_config WHERE key = 'referral_config';
    v_referrer_bonus := COALESCE((v_config->>'referrer_bonus')::NUMERIC, 10.00);
    v_referee_bonus := COALESCE((v_config->>'referee_signup_bonus')::NUMERIC, 5.00);

    -- Credit referrer
    UPDATE public.wallets 
    SET balance = balance + v_referrer_bonus,
        total_earned = total_earned + v_referrer_bonus,
        updated_at = NOW()
    WHERE user_id = v_ref.referrer_id
    RETURNING id INTO v_referrer_wallet;

    INSERT INTO public.ledger_entries (wallet_id, user_id, type, amount, reason, reference_id)
    VALUES (v_referrer_wallet, v_ref.referrer_id, 'credit', v_referrer_bonus, 'referral_bonus', p_referee_id::TEXT);

    -- Credit referee (if signup bonus configured)
    IF v_referee_bonus > 0 THEN
        UPDATE public.wallets 
        SET balance = balance + v_referee_bonus,
            total_earned = total_earned + v_referee_bonus,
            updated_at = NOW()
        WHERE user_id = p_referee_id
        RETURNING id INTO v_referee_wallet;

        INSERT INTO public.ledger_entries (wallet_id, user_id, type, amount, reason, reference_id)
        VALUES (v_referee_wallet, p_referee_id, 'credit', v_referee_bonus, 'signup_referral_bonus', v_ref.referrer_id::TEXT);
    END IF;

    -- Update referral status
    UPDATE public.referrals 
    SET status = 'rewarded',
        bonus_amount = v_referrer_bonus,
        rewarded_at = NOW()
    WHERE id = v_ref.id;

    RETURN jsonb_build_object(
        'success', TRUE,
        'referrer_id', v_ref.referrer_id,
        'referrer_bonus', v_referrer_bonus,
        'referee_bonus', v_referee_bonus
    );
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- ============================================================================
-- 5. REWARD TASK (Approves claim and credits prize)
-- ============================================================================
CREATE OR REPLACE FUNCTION public.reward_task(p_claim_id UUID)
RETURNS JSONB AS $$
DECLARE
    v_claim RECORD;
    v_prize NUMERIC(10, 2);
    v_wallet_id UUID;
    v_new_balance NUMERIC(12, 2);
    v_completed_count INT;
BEGIN
    SELECT c.*, o.prize INTO v_claim 
    FROM public.claims c
    JOIN public.offers o ON o.id = c.offer_id
    WHERE c.id = p_claim_id
    FOR UPDATE;

    IF NOT FOUND THEN
        RETURN jsonb_build_object('success', FALSE, 'message', 'Claim not found');
    END IF;

    IF v_claim.status != 'pending' THEN
        RETURN jsonb_build_object('success', FALSE, 'message', 'Claim is not in pending status');
    END IF;

    v_prize := v_claim.prize;

    -- Mark completed
    UPDATE public.claims 
    SET status = 'completed' 
    WHERE id = p_claim_id;

    -- Credit wallet
    UPDATE public.wallets 
    SET balance = balance + v_prize,
        total_earned = total_earned + v_prize,
        updated_at = NOW()
    WHERE user_id = v_claim.user_id
    RETURNING id, balance INTO v_wallet_id, v_new_balance;

    -- Ledger
    INSERT INTO public.ledger_entries (wallet_id, user_id, type, amount, reason, reference_id)
    VALUES (v_wallet_id, v_claim.user_id, 'credit', v_prize, 'task_reward', p_claim_id::TEXT);

    -- Check if this is referee's first completed task for referral payout
    SELECT COUNT(*) INTO v_completed_count 
    FROM public.claims 
    WHERE user_id = v_claim.user_id AND status = 'completed';

    IF v_completed_count = 1 THEN
        PERFORM public.reward_referral(v_claim.user_id);
    END IF;

    RETURN jsonb_build_object(
        'success', TRUE,
        'prize', v_prize,
        'new_balance', v_new_balance
    );
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;
