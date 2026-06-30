-- =====================================================================
-- seed-demo 롤백 스크립트
-- user_id=9999 (이클립스) 연관 데이터 전체 삭제 + IPO 상태 원복
-- FK 순서 준수 (자식 → 부모)
-- =====================================================================

SET @demo_uid = 9999;

-- ─────────────────────────────────────────────
-- 1. holding_lots → holdings
-- ─────────────────────────────────────────────
DELETE hl FROM holding_lots hl
    JOIN holdings h ON hl.holding_id = h.id WHERE h.user_id = @demo_uid;
DELETE FROM holding_lots WHERE user_id = @demo_uid;
DELETE FROM holdings     WHERE user_id = @demo_uid;

-- ─────────────────────────────────────────────
-- 2. transfer_transactions → return_plan_allocations → return_plans
-- ─────────────────────────────────────────────
DELETE tt FROM transfer_transactions tt
    JOIN return_plan_allocations rpa ON tt.allocation_id = rpa.id
    JOIN return_plans rp ON rpa.return_plan_id = rp.id WHERE rp.user_id = @demo_uid;

DELETE rpa FROM return_plan_allocations rpa
    JOIN return_plans rp ON rpa.return_plan_id = rp.id WHERE rp.user_id = @demo_uid;

DELETE FROM return_plans WHERE user_id = @demo_uid;

-- ─────────────────────────────────────────────
-- 3. balance_holds → ipo_subscriptions
-- ─────────────────────────────────────────────
DELETE bh FROM balance_holds bh
    JOIN ipo_subscriptions s ON bh.subscription_id = s.id WHERE s.user_id = @demo_uid;

DELETE FROM ipo_subscriptions WHERE user_id = @demo_uid;

-- ─────────────────────────────────────────────
-- 4. rp_contracts, trade_orders, fx_exchange_transactions, transfer_transactions
-- ─────────────────────────────────────────────
DELETE FROM rp_contracts             WHERE user_id = @demo_uid;
DELETE FROM trade_orders             WHERE user_id = @demo_uid;
DELETE FROM fx_exchange_transactions WHERE user_id = @demo_uid;
DELETE FROM transfer_transactions    WHERE user_id = @demo_uid;

-- ─────────────────────────────────────────────
-- 5. card_transactions → cards
-- ─────────────────────────────────────────────
DELETE ct FROM card_transactions ct
    JOIN cards c ON ct.card_id = c.id WHERE c.user_id = @demo_uid;
DELETE FROM cards WHERE user_id = @demo_uid;

-- ─────────────────────────────────────────────
-- 6. 나머지 user_id 직접 참조 테이블
-- ─────────────────────────────────────────────
DELETE FROM favorite_ipos          WHERE user_id = @demo_uid;
DELETE FROM notifications          WHERE user_id = @demo_uid;
DELETE FROM idle_dollar_triggers   WHERE user_id = @demo_uid;
DELETE FROM notification_settings  WHERE user_id = @demo_uid;
DELETE FROM investment_profiles    WHERE user_id = @demo_uid;
DELETE FROM financial_accounts     WHERE user_id = @demo_uid;

-- ─────────────────────────────────────────────
-- 7. 유저 삭제
-- ─────────────────────────────────────────────
DELETE FROM users WHERE id = @demo_uid;

-- ─────────────────────────────────────────────
-- 8. IPO 상태 원복 (RDS 기준)
-- ─────────────────────────────────────────────
UPDATE ipos SET ipo_status = 'UPCOMING' WHERE id = 69;  -- BSP : OPEN → UPCOMING

UPDATE ipos SET ipo_status = 'LISTED',
                refund_date  = NULL,
                deposit_date = NULL
    WHERE id = 63;  -- DSC : CLOSED → LISTED

UPDATE ipos SET ipo_status = 'LISTED',
                refund_date  = NULL,
                deposit_date = NULL
    WHERE id = 91;  -- DPC : CLOSED → LISTED

UPDATE ipos SET ipo_status = 'LISTED' WHERE id = 77;  -- FCBM: CLOSED → LISTED

UPDATE ipos SET ipo_status = 'LISTED',
                product_id = NULL
    WHERE id = 89;  -- SPCX: CLOSED → LISTED, product_id 제거

-- ─────────────────────────────────────────────
-- 9. SPCX 종목 제거 (FK 순서: holding_lots → holdings → investment_products)
--    user_id 무관하게 SPCX product 참조하는 모든 holding 먼저 제거
-- ─────────────────────────────────────────────
DELETE hl FROM holding_lots hl
    JOIN holdings h ON hl.holding_id = h.id
    JOIN investment_products p ON h.product_id = p.id
    WHERE p.ticker = 'SPCX';

DELETE h FROM holdings h
    JOIN investment_products p ON h.product_id = p.id
    WHERE p.ticker = 'SPCX';

DELETE FROM investment_products WHERE ticker = 'SPCX';

-- ─────────────────────────────────────────────
-- 확인
-- ─────────────────────────────────────────────
SELECT '=== 잔여 유저 (9999 없어야 함) ===' AS '';
SELECT id, name, email FROM users ORDER BY id;

SELECT '=== IPO 원복 확인 ===' AS '';
SELECT id, ticker, ipo_status, refund_date, product_id
FROM ipos WHERE id IN (63, 69, 77, 89, 91);
