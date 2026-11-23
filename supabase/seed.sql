-- Seed data for app_config and sample offers
INSERT INTO public.app_config (key, value, description)
VALUES 
    ('maintenance_mode', '{"enabled": false, "message": "Rupee Rush is undergoing scheduled maintenance.", "expected_back_time": null, "support_link": "https://t.me/rupeerush"}'::jsonb, 'Maintenance lock state and details'),
    ('daily_checkin', '{"bonus_base": 1.00, "bonus_streak_increment": 0.50, "max_daily_bonus": 10.00, "reminder_hour": 20}'::jsonb, 'Check-in rewards and reminder timing'),
    ('referral_config', '{"referrer_bonus": 10.00, "referee_signup_bonus": 5.00, "reward_on_first_task": true}'::jsonb, 'Refer & earn reward configuration'),
    ('withdrawal_config', '{"min_amount": 50.00, "max_amount": 1000.00, "daily_max_amount": 2000.00, "daily_request_limit": 2, "hold_hours": 24}'::jsonb, 'Withdrawal thresholds and limits'),
    ('telegram_channel', '{"url": "https://t.me/rupeerush", "channel_name": "Rupee Rush Official"}'::jsonb, 'Official community channel link')
ON CONFLICT (key) DO UPDATE 
SET value = EXCLUDED.value, updated_at = NOW();

-- Insert Sample Welcome Promo Code
INSERT INTO public.promo_codes (code, amount, total_limit, daily_limit_per_user, is_active)
VALUES ('WELCOME50', 50.00, 1000, 1, TRUE)
ON CONFLICT (code) DO NOTHING;
