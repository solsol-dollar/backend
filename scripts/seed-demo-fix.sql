-- =====================================================================
-- seed-demo-fix.sql
-- seed-demo.sql 실행 후 RDS ID 불일치 수정본
--
-- RDS 실제 계좌:
--   id=10 : SECURITIES USD  ← 청약용 메인 계좌
--   id=11 : SECURITIES KRW  (기존)
--   id=12 : SECURITIES KRW  (seed-demo.sql이 실수로 추가한 중복 → 제거)
--   id=13 : DEPOSIT USD     (seed-demo.sql이 추가한 것, 유지)
--
-- RDS 실제 IPO ID:
--   BSP=69  DSC=63  DPC=91  FCBM=77  SPCX=89
-- =====================================================================

SET @now = NOW();

SET @next_biz = CASE DAYOFWEEK(CURDATE())
    WHEN 6 THEN DATE_ADD(CURDATE(), INTERVAL 3 DAY)
    WHEN 7 THEN DATE_ADD(CURDATE(), INTERVAL 2 DAY)
    ELSE DATE_ADD(CURDATE(), INTERVAL 1 DAY)
END;

-- ─────────────────────────────────────────────
-- 1. 중복 KRW 계좌 제거 (seed-demo.sql이 잘못 추가한 것)
-- ─────────────────────────────────────────────
DELETE FROM financial_accounts WHERE id = 12;

-- ─────────────────────────────────────────────
-- 2. SECURITIES USD 계좌 reserved_balance 반영
--    (BSP 청약 LOCKED 금액 $270)
-- ─────────────────────────────────────────────
UPDATE financial_accounts
    SET reserved_balance = 270.0000
    WHERE id = 10;

-- ─────────────────────────────────────────────
-- 3. 카드 (AUTO_INCREMENT, DEPOSIT 계좌 id=13 연결)
-- ─────────────────────────────────────────────
INSERT INTO cards (
    user_id, linked_account_id, card_type, card_status,
    card_name, card_number_masked, issuer_name, linked, linked_at,
    created_at, updated_at, status
) VALUES (
    5, 13, 'CHECK_CARD', 'LINKED',
    '신한 체인지업 체크카드', '9411-9999-0000-0001', '신한카드', TRUE, @now,
    @now, @now, 'ACTIVE'
);

-- ─────────────────────────────────────────────
-- 4. 청약 내역 5건 (RDS IPO ID 기준)
-- ─────────────────────────────────────────────

-- [1] BSP (id=69) — 청약 확정, 배정 대기
INSERT INTO ipo_subscriptions (
    user_id, ipo_id, securities_account_id,
    requested_shares, offer_price, subscription_amount, currency,
    subscription_status, execution_mode, subscribed_at, result_status, confirmed_at,
    created_at, updated_at, status
) VALUES (
    5, 69, 10,
    10, 27.0000, 270.0000, 'USD',
    'CONFIRMED', 'MOCK', DATE_SUB(@now, INTERVAL 2 HOUR), NULL, DATE_SUB(@now, INTERVAL 1 HOUR),
    @now, @now, 'ACTIVE'
);
SET @sub_bsp = LAST_INSERT_ID();

-- [2] DSC (id=63) — 배정 완료, 리턴플랜 DRAFT (환불 내일)
--     10주 신청 → 8주 배정(80%), 환불 $34
INSERT INTO ipo_subscriptions (
    user_id, ipo_id, securities_account_id,
    requested_shares, offer_price, subscription_amount, currency,
    subscription_status, execution_mode, subscribed_at,
    allocated_shares, allocated_amount, refund_amount, allocation_rate,
    result_status, confirmed_at,
    created_at, updated_at, status
) VALUES (
    5, 63, 10,
    10, 17.0000, 170.0000, 'USD',
    'CONFIRMED', 'MOCK', DATE_SUB(CURDATE(), INTERVAL 9 DAY),
    8, 136.0000, 34.0000, 80.0000,
    'COMPLETED', DATE_SUB(CURDATE(), INTERVAL 9 DAY),
    @now, @now, 'ACTIVE'
);
SET @sub_dsc = LAST_INSERT_ID();

-- [3] DPC (id=91) — 배정 완료, 리턴플랜 없음 (오늘 환불)
--     8주 신청 → 6주 배정(75%), 환불 $60
INSERT INTO ipo_subscriptions (
    user_id, ipo_id, securities_account_id,
    requested_shares, offer_price, subscription_amount, currency,
    subscription_status, execution_mode, subscribed_at,
    allocated_shares, allocated_amount, refund_amount, allocation_rate,
    result_status, confirmed_at,
    created_at, updated_at, status
) VALUES (
    5, 91, 10,
    8, 30.0000, 240.0000, 'USD',
    'CONFIRMED', 'MOCK', DATE_SUB(CURDATE(), INTERVAL 15 DAY),
    6, 180.0000, 60.0000, 75.0000,
    'COMPLETED', DATE_SUB(CURDATE(), INTERVAL 14 DAY),
    @now, @now, 'ACTIVE'
);
SET @sub_dpc = LAST_INSERT_ID();

-- [4] FCBM (id=77) — 낙첨 (배정 0주, 전액 환불 완료)
--     10주 신청 → 0주 배정, 환불 $125
INSERT INTO ipo_subscriptions (
    user_id, ipo_id, securities_account_id,
    requested_shares, offer_price, subscription_amount, currency,
    subscription_status, execution_mode, subscribed_at,
    allocated_shares, allocated_amount, refund_amount, allocation_rate,
    result_status, confirmed_at,
    created_at, updated_at, status
) VALUES (
    5, 77, 10,
    10, 12.5000, 125.0000, 'USD',
    'CONFIRMED', 'MOCK', DATE_SUB(CURDATE(), INTERVAL 22 DAY),
    0, 0.0000, 125.0000, 0.0000,
    'COMPLETED', DATE_SUB(CURDATE(), INTERVAL 21 DAY),
    @now, @now, 'ACTIVE'
);
SET @sub_fcbm = LAST_INSERT_ID();

-- [5] SPCX (id=89) — 배정 완료, 주식 입고 (히스토리)
--     5주 신청 → 5주 배정(100%), 환불 $0
INSERT INTO ipo_subscriptions (
    user_id, ipo_id, securities_account_id,
    requested_shares, offer_price, subscription_amount, currency,
    subscription_status, execution_mode, subscribed_at,
    allocated_shares, allocated_amount, refund_amount, allocation_rate,
    result_status, confirmed_at,
    created_at, updated_at, status
) VALUES (
    5, 89, 10,
    5, 135.0000, 675.0000, 'USD',
    'CONFIRMED', 'MOCK', DATE_SUB(CURDATE(), INTERVAL 28 DAY),
    5, 675.0000, 0.0000, 100.0000,
    'DEPOSITED', DATE_SUB(CURDATE(), INTERVAL 27 DAY),
    @now, @now, 'ACTIVE'
);
SET @sub_spcx = LAST_INSERT_ID();

-- ─────────────────────────────────────────────
-- 5. balance_holds
-- ─────────────────────────────────────────────
INSERT INTO balance_holds (
    account_id, subscription_id, amount, hold_status, settled_at,
    created_at, updated_at, status
) VALUES
    (10, @sub_bsp,  270.0000, 'LOCKED',  NULL,                                    @now, @now, 'ACTIVE'),
    (10, @sub_dsc,  170.0000, 'SETTLED', DATE_SUB(CURDATE(), INTERVAL 9 DAY),     @now, @now, 'ACTIVE'),
    (10, @sub_dpc,  240.0000, 'SETTLED', DATE_SUB(CURDATE(), INTERVAL 14 DAY),    @now, @now, 'ACTIVE'),
    (10, @sub_fcbm, 125.0000, 'SETTLED', DATE_SUB(CURDATE(), INTERVAL 21 DAY),    @now, @now, 'ACTIVE'),
    (10, @sub_spcx, 675.0000, 'SETTLED', DATE_SUB(CURDATE(), INTERVAL 27 DAY),    @now, @now, 'ACTIVE');

-- ─────────────────────────────────────────────
-- 6. return_plans (DSC만 DRAFT)
-- ─────────────────────────────────────────────
INSERT INTO return_plans (
    user_id, subscription_id, next_ipo_id,
    total_refund_amount, currency, current_securities_balance,
    plan_status, created_at, updated_at, status
) VALUES (
    5, @sub_dsc, 69,
    34.0000, 'USD', 10000.0000,
    'DRAFT', @now, @now, 'ACTIVE'
);
SET @rp_dsc = LAST_INSERT_ID();

INSERT INTO return_plan_allocations (
    return_plan_id, destination_type, destination_account_id,
    allocation_ratio, allocation_amount, allocation_status,
    created_at, updated_at, status
) VALUES
    (@rp_dsc, 'SECURITIES', 10, 60.00, 20.4000, 'PENDING', @now, @now, 'ACTIVE'),
    (@rp_dsc, 'DEPOSIT',    13, 40.00, 13.6000, 'PENDING', @now, @now, 'ACTIVE');

-- ─────────────────────────────────────────────
-- 7. holdings / holding_lots (SPCX 5주)
-- ─────────────────────────────────────────────
INSERT INTO holdings (
    user_id, product_id, total_quantity, average_price, currency,
    created_at, updated_at, status
)
SELECT 5, p.id, 5, 135.0000, 'USD', @now, @now, 'ACTIVE'
FROM investment_products p WHERE p.ticker = 'SPCX' LIMIT 1;

SET @holding_spcx = LAST_INSERT_ID();

INSERT INTO holding_lots (
    holding_id, user_id, product_id, source_type, source_id,
    quantity, remaining_quantity, acquisition_price, acquired_at,
    created_at, updated_at, status
)
SELECT @holding_spcx, 5, p.id, 'IPO_ALLOCATION', @sub_spcx,
       5, 5, 135.0000, DATE_SUB(CURDATE(), INTERVAL 18 DAY),
       @now, @now, 'ACTIVE'
FROM investment_products p WHERE p.ticker = 'SPCX' LIMIT 1;

-- ─────────────────────────────────────────────
-- 확인
-- ─────────────────────────────────────────────
SELECT '=== 계좌 ===' AS '';
SELECT id, account_type, currency, balance, reserved_balance
FROM financial_accounts WHERE user_id = 5 ORDER BY id;

SELECT '=== 청약 내역 ===' AS '';
SELECT s.id, i.ticker, s.subscription_status, s.result_status,
       s.requested_shares, s.allocated_shares, s.refund_amount
FROM ipo_subscriptions s JOIN ipos i ON s.ipo_id = i.id
WHERE s.user_id = 5 ORDER BY s.id;

SELECT '=== 리턴플랜 ===' AS '';
SELECT rp.id, rp.total_refund_amount, rp.plan_status,
       rpa.destination_type, rpa.allocation_ratio, rpa.allocation_amount
FROM return_plans rp
JOIN return_plan_allocations rpa ON rpa.return_plan_id = rp.id
WHERE rp.user_id = 5;

SELECT '=== 보유 주식 ===' AS '';
SELECT h.total_quantity, p.ticker, h.average_price
FROM holdings h JOIN investment_products p ON h.product_id = p.id
WHERE h.user_id = 5;
