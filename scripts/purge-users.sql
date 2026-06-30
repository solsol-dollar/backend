-- =====================================================================
-- 전체 유저 데이터 삭제 스크립트
-- users 및 연관 테이블 전체 초기화
-- ipos, investment_products, price_candles 등 마스터 데이터는 유지
-- =====================================================================

SET FOREIGN_KEY_CHECKS = 0;

TRUNCATE TABLE holding_lots;
TRUNCATE TABLE holdings;
TRUNCATE TABLE transfer_transactions;
TRUNCATE TABLE return_plan_allocations;
TRUNCATE TABLE return_plans;
TRUNCATE TABLE balance_holds;
TRUNCATE TABLE ipo_subscriptions;
TRUNCATE TABLE rp_contracts;
TRUNCATE TABLE trade_orders;
TRUNCATE TABLE fx_exchange_transactions;
TRUNCATE TABLE card_transactions;
TRUNCATE TABLE cards;
TRUNCATE TABLE favorite_ipos;
TRUNCATE TABLE notifications;
TRUNCATE TABLE idle_dollar_triggers;
TRUNCATE TABLE notification_settings;
TRUNCATE TABLE investment_profiles;
TRUNCATE TABLE financial_accounts;
TRUNCATE TABLE users;

SET FOREIGN_KEY_CHECKS = 1;

-- ─────────────────────────────────────────────
-- 확인
-- ─────────────────────────────────────────────
SELECT 'users',                COUNT(*) FROM users
UNION ALL SELECT 'financial_accounts',  COUNT(*) FROM financial_accounts
UNION ALL SELECT 'ipo_subscriptions',   COUNT(*) FROM ipo_subscriptions
UNION ALL SELECT 'balance_holds',       COUNT(*) FROM balance_holds
UNION ALL SELECT 'return_plans',        COUNT(*) FROM return_plans
UNION ALL SELECT 'holdings',            COUNT(*) FROM holdings;
