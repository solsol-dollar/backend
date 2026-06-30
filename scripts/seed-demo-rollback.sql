-- =====================================================================
-- seed-demo 롤백 스크립트
-- user_id=9999 (이클립스) 연관 데이터 전체 삭제
-- 신규 IPO (VRNT/BLZX/NXTN, id=201~203) 및 investment_products 삭제
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
-- 4. 기타 user_id 직접 참조 테이블
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
-- 8. 신규 IPO 삭제 (구독 먼저 삭제 완료)
--    VRNT(201) / BLZX(202) / NXTN(203)
-- ─────────────────────────────────────────────
DELETE FROM ipos                WHERE id IN (201, 202, 203);
DELETE FROM investment_products WHERE ticker IN ('VRNT', 'BLZX', 'NXTN');

-- ─────────────────────────────────────────────
-- 확인
-- ─────────────────────────────────────────────
SELECT '=== user_id=9999 잔여 확인 (0건이어야 함) ===' AS '';
SELECT COUNT(*) AS remaining_users        FROM users              WHERE id = 9999;
SELECT COUNT(*) AS remaining_accounts     FROM financial_accounts WHERE user_id = 9999;
SELECT COUNT(*) AS remaining_subs         FROM ipo_subscriptions  WHERE user_id = 9999;
SELECT COUNT(*) AS remaining_plans        FROM return_plans       WHERE user_id = 9999;

SELECT '=== 신규 IPO 잔여 확인 (0건이어야 함) ===' AS '';
SELECT COUNT(*) AS remaining_ipos         FROM ipos               WHERE id IN (201, 202, 203);
SELECT COUNT(*) AS remaining_products     FROM investment_products WHERE ticker IN ('VRNT', 'BLZX', 'NXTN');
