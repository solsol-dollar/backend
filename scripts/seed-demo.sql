-- =====================================================================
-- 시연용 풀 스테이트 유저
-- 이름: 이클립스 / 이메일: demo@eclipse.dev / 간편비밀번호: 000000
-- user id=9999 (고정 — 환경 무관하게 충돌 없음)
--
-- 실행 방법 (어떤 순서든 가능):
--   A) seed-demo-rollback.sql → seed-demo.sql
--   B) purge-users.sql → seed-users.sql → seed-demo.sql
--   C) seed-demo.sql 단독 (멱등 — 기존 데이터 자동 초기화 후 재생성)
--
-- 청약 시나리오 5종 (RDS IPO ID 기준):
--   BSP  (id=69) : 청약 확정, 배정 대기 (오늘 마감, 내일 상장)
--   DSC  (id=63) : 배정 완료, 리턴플랜 DRAFT (환불 내일)
--   DPC  (id=91) : 배정 완료, 리턴플랜 없음 (오늘 환불 — Job 자동생성)
--   FCBM (id=77) : 낙첨 — 전액 환불 완료
--   SPCX (id=89) : 배정 완료, 주식 입고 (히스토리)
-- =====================================================================

SET @demo_uid = 9999;
SET @now = NOW();
SET @next_biz = CASE DAYOFWEEK(CURDATE())
    WHEN 6 THEN DATE_ADD(CURDATE(), INTERVAL 3 DAY)
    WHEN 7 THEN DATE_ADD(CURDATE(), INTERVAL 2 DAY)
    ELSE DATE_ADD(CURDATE(), INTERVAL 1 DAY)
END;

-- =====================================================================
-- 0. 기존 데모 데이터 초기화 (멱등 보장)
-- =====================================================================
DELETE hl FROM holding_lots hl
    JOIN holdings h ON hl.holding_id = h.id WHERE h.user_id = @demo_uid;
DELETE FROM holding_lots        WHERE user_id = @demo_uid;
DELETE FROM holdings            WHERE user_id = @demo_uid;

DELETE tt FROM transfer_transactions tt
    JOIN return_plan_allocations rpa ON tt.allocation_id = rpa.id
    JOIN return_plans rp ON rpa.return_plan_id = rp.id WHERE rp.user_id = @demo_uid;
DELETE rpa FROM return_plan_allocations rpa
    JOIN return_plans rp ON rpa.return_plan_id = rp.id WHERE rp.user_id = @demo_uid;
DELETE FROM return_plans        WHERE user_id = @demo_uid;

DELETE bh FROM balance_holds bh
    JOIN ipo_subscriptions s ON bh.subscription_id = s.id WHERE s.user_id = @demo_uid;
DELETE FROM ipo_subscriptions   WHERE user_id = @demo_uid;

DELETE FROM rp_contracts             WHERE user_id = @demo_uid;
DELETE FROM trade_orders             WHERE user_id = @demo_uid;
DELETE FROM fx_exchange_transactions WHERE user_id = @demo_uid;
DELETE FROM transfer_transactions    WHERE user_id = @demo_uid;

DELETE ct FROM card_transactions ct
    JOIN cards c ON ct.card_id = c.id WHERE c.user_id = @demo_uid;
DELETE FROM cards                WHERE user_id = @demo_uid;

DELETE FROM favorite_ipos            WHERE user_id = @demo_uid;
DELETE FROM notifications            WHERE user_id = @demo_uid;
DELETE FROM idle_dollar_triggers     WHERE user_id = @demo_uid;
DELETE FROM notification_settings    WHERE user_id = @demo_uid;
DELETE FROM investment_profiles      WHERE user_id = @demo_uid;
DELETE FROM financial_accounts       WHERE user_id = @demo_uid;
DELETE FROM users                    WHERE id = @demo_uid;

-- =====================================================================
-- 1. 유저
-- =====================================================================
INSERT INTO users (
    id, name, email, phone_number, onboarding_status, investment_status,
    simple_password, created_at, updated_at, status
) VALUES (
    9999, '이클립스', 'demo@eclipse.dev', '010-9999-0000', 'COMPLETED', 'COMPLETED',
    '$2a$10$Ob7dEdHyuwJiDtAg3TMtMe00XhbehVoFY/VVDPMRGcTmOm.w3IRFC',
    @now, @now, 'ACTIVE'
);

-- =====================================================================
-- 2. 금융 계좌 (AUTO_INCREMENT — ID 충돌 없음)
--    SECURITIES USD : 청약용 메인 (balance=$10,000 / reserved=$270)
--    SECURITIES KRW
--    DEPOSIT USD    : 리턴플랜 배분 대상 (balance=$2,000)
-- =====================================================================
INSERT INTO financial_accounts (
    user_id, account_type, institution_type, institution_name,
    account_name, account_number, virtual_account_number,
    currency, balance, reserved_balance, interest_rate, maturity_date,
    linked, linked_at, created_at, updated_at, status
) VALUES (
    9999, 'SECURITIES', 'SECURITIES_FIRM', '신한투자증권',
    '신한투자증권 CMA 외화 계좌', '110-999-000001', '110-999-100001',
    'USD', 10000.0000, 270.0000, NULL, NULL,
    TRUE, @now, @now, @now, 'ACTIVE'
);
SET @acct_sec_usd = LAST_INSERT_ID();

INSERT INTO financial_accounts (
    user_id, account_type, institution_type, institution_name,
    account_name, account_number, virtual_account_number,
    currency, balance, reserved_balance, interest_rate, maturity_date,
    linked, linked_at, created_at, updated_at, status
) VALUES (
    9999, 'SECURITIES', 'SECURITIES_FIRM', '신한투자증권',
    '신한투자증권 CMA 원화 계좌', '110-999-000001', NULL,
    'KRW', 0.0000, 0.0000, NULL, NULL,
    TRUE, @now, @now, @now, 'ACTIVE'
);

INSERT INTO financial_accounts (
    user_id, account_type, institution_type, institution_name,
    account_name, account_number, virtual_account_number,
    currency, balance, reserved_balance, interest_rate, maturity_date,
    linked, linked_at, created_at, updated_at, status
) VALUES (
    9999, 'DEPOSIT', 'BANK', '신한은행',
    '신한 외화 체인지업 예금', '110-999-000002', NULL,
    'USD', 2000.0000, 0.0000, 2.5000, NULL,
    TRUE, @now, @now, @now, 'ACTIVE'
);
SET @acct_deposit = LAST_INSERT_ID();

-- =====================================================================
-- 3. 카드 (DEPOSIT 계좌 연결)
-- =====================================================================
INSERT INTO cards (
    user_id, linked_account_id, card_type, card_status,
    card_name, card_number_masked, issuer_name, linked, linked_at,
    created_at, updated_at, status
) VALUES (
    9999, @acct_deposit, 'CHECK_CARD', 'LINKED',
    '신한 체인지업 체크카드', '9411-9999-0000-0001', '신한카드', TRUE, @now,
    @now, @now, 'ACTIVE'
);

-- =====================================================================
-- 4. 알림 설정
-- =====================================================================
INSERT INTO notification_settings (
    user_id, ipo_allocation_enabled, ipo_refund_enabled, idle_dollar_enabled,
    created_at, updated_at, status
) VALUES (9999, TRUE, TRUE, TRUE, @now, @now, 'ACTIVE');

-- =====================================================================
-- 5. IPO 상태 업데이트
-- =====================================================================
UPDATE ipos SET ipo_status = 'OPEN'
    WHERE id = 69;  -- BSP: UPCOMING → OPEN

UPDATE ipos SET ipo_status = 'CLOSED', refund_date = @next_biz, deposit_date = @next_biz
    WHERE id = 63;  -- DSC: LISTED → CLOSED, 환불 내일

UPDATE ipos SET ipo_status = 'CLOSED', refund_date = CURDATE(), deposit_date = CURDATE()
    WHERE id = 91;  -- DPC: LISTED → CLOSED, 환불 오늘

UPDATE ipos SET ipo_status = 'CLOSED'
    WHERE id IN (77, 89);  -- FCBM, SPCX: LISTED → CLOSED

-- =====================================================================
-- 6. SPCX 종목 등록 및 IPO product_id 연결
-- =====================================================================
INSERT INTO investment_products (
    product_type, ticker, product_name, exchange_name, currency, sector,
    created_at, updated_at, status
)
SELECT 'STOCK', 'SPCX', 'Space Exploration Technologies Corp', 'NASDAQ', 'USD',
       'Aerospace & Defense', @now, @now, 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM investment_products WHERE ticker = 'SPCX');

UPDATE ipos
    SET product_id = (SELECT id FROM investment_products WHERE ticker = 'SPCX' LIMIT 1)
    WHERE id = 89;

-- =====================================================================
-- 7. 청약 내역 5건
-- =====================================================================

-- [1] BSP — 청약 확정, 배정 대기
INSERT INTO ipo_subscriptions (
    user_id, ipo_id, securities_account_id,
    requested_shares, offer_price, subscription_amount, currency,
    subscription_status, execution_mode, subscribed_at, result_status, confirmed_at,
    created_at, updated_at, status
) VALUES (
    9999, 69, @acct_sec_usd,
    10, 27.0000, 270.0000, 'USD',
    'CONFIRMED', 'MOCK', DATE_SUB(@now, INTERVAL 2 HOUR), NULL, DATE_SUB(@now, INTERVAL 1 HOUR),
    @now, @now, 'ACTIVE'
);
SET @sub_bsp = LAST_INSERT_ID();

-- [2] DSC — 배정 완료, 리턴플랜 DRAFT (환불 내일)
--     10주 신청 → 8주 배정(80%), 환불 $34
INSERT INTO ipo_subscriptions (
    user_id, ipo_id, securities_account_id,
    requested_shares, offer_price, subscription_amount, currency,
    subscription_status, execution_mode, subscribed_at,
    allocated_shares, allocated_amount, refund_amount, allocation_rate,
    result_status, confirmed_at,
    created_at, updated_at, status
) VALUES (
    9999, 63, @acct_sec_usd,
    10, 17.0000, 170.0000, 'USD',
    'CONFIRMED', 'MOCK', DATE_SUB(CURDATE(), INTERVAL 9 DAY),
    8, 136.0000, 34.0000, 80.0000,
    'COMPLETED', DATE_SUB(CURDATE(), INTERVAL 9 DAY),
    @now, @now, 'ACTIVE'
);
SET @sub_dsc = LAST_INSERT_ID();

-- [3] DPC — 배정 완료, 리턴플랜 없음 (오늘 환불)
--     8주 신청 → 6주 배정(75%), 환불 $60
INSERT INTO ipo_subscriptions (
    user_id, ipo_id, securities_account_id,
    requested_shares, offer_price, subscription_amount, currency,
    subscription_status, execution_mode, subscribed_at,
    allocated_shares, allocated_amount, refund_amount, allocation_rate,
    result_status, confirmed_at,
    created_at, updated_at, status
) VALUES (
    9999, 91, @acct_sec_usd,
    8, 30.0000, 240.0000, 'USD',
    'CONFIRMED', 'MOCK', DATE_SUB(CURDATE(), INTERVAL 15 DAY),
    6, 180.0000, 60.0000, 75.0000,
    'COMPLETED', DATE_SUB(CURDATE(), INTERVAL 14 DAY),
    @now, @now, 'ACTIVE'
);
SET @sub_dpc = LAST_INSERT_ID();

-- [4] FCBM — 낙첨 (배정 0주, 전액 환불 완료)
--     10주 신청 → 0주 배정, 환불 $125
INSERT INTO ipo_subscriptions (
    user_id, ipo_id, securities_account_id,
    requested_shares, offer_price, subscription_amount, currency,
    subscription_status, execution_mode, subscribed_at,
    allocated_shares, allocated_amount, refund_amount, allocation_rate,
    result_status, confirmed_at,
    created_at, updated_at, status
) VALUES (
    9999, 77, @acct_sec_usd,
    10, 12.5000, 125.0000, 'USD',
    'CONFIRMED', 'MOCK', DATE_SUB(CURDATE(), INTERVAL 22 DAY),
    0, 0.0000, 125.0000, 0.0000,
    'COMPLETED', DATE_SUB(CURDATE(), INTERVAL 21 DAY),
    @now, @now, 'ACTIVE'
);
SET @sub_fcbm = LAST_INSERT_ID();

-- [5] SPCX — 배정 완료, 주식 입고 (히스토리)
--     5주 신청 → 5주 배정(100%), 환불 $0
INSERT INTO ipo_subscriptions (
    user_id, ipo_id, securities_account_id,
    requested_shares, offer_price, subscription_amount, currency,
    subscription_status, execution_mode, subscribed_at,
    allocated_shares, allocated_amount, refund_amount, allocation_rate,
    result_status, confirmed_at,
    created_at, updated_at, status
) VALUES (
    9999, 89, @acct_sec_usd,
    5, 135.0000, 675.0000, 'USD',
    'CONFIRMED', 'MOCK', DATE_SUB(CURDATE(), INTERVAL 28 DAY),
    5, 675.0000, 0.0000, 100.0000,
    'DEPOSITED', DATE_SUB(CURDATE(), INTERVAL 27 DAY),
    @now, @now, 'ACTIVE'
);
SET @sub_spcx = LAST_INSERT_ID();

-- =====================================================================
-- 8. balance_holds
-- =====================================================================
INSERT INTO balance_holds (
    account_id, subscription_id, amount, hold_status, settled_at,
    created_at, updated_at, status
) VALUES
    (@acct_sec_usd, @sub_bsp,  270.0000, 'LOCKED',  NULL,                                 @now, @now, 'ACTIVE'),
    (@acct_sec_usd, @sub_dsc,  170.0000, 'SETTLED', DATE_SUB(CURDATE(), INTERVAL 9 DAY),  @now, @now, 'ACTIVE'),
    (@acct_sec_usd, @sub_dpc,  240.0000, 'SETTLED', DATE_SUB(CURDATE(), INTERVAL 14 DAY), @now, @now, 'ACTIVE'),
    (@acct_sec_usd, @sub_fcbm, 125.0000, 'SETTLED', DATE_SUB(CURDATE(), INTERVAL 21 DAY), @now, @now, 'ACTIVE'),
    (@acct_sec_usd, @sub_spcx, 675.0000, 'SETTLED', DATE_SUB(CURDATE(), INTERVAL 27 DAY), @now, @now, 'ACTIVE');

-- =====================================================================
-- 9. return_plans (DSC만 DRAFT, DPC는 없음 — Job 자동생성 대상)
-- =====================================================================
INSERT INTO return_plans (
    user_id, subscription_id, next_ipo_id,
    total_refund_amount, currency, current_securities_balance,
    plan_status, created_at, updated_at, status
) VALUES (
    9999, @sub_dsc, 69,
    34.0000, 'USD', 10000.0000,
    'DRAFT', @now, @now, 'ACTIVE'
);
SET @rp_dsc = LAST_INSERT_ID();

INSERT INTO return_plan_allocations (
    return_plan_id, destination_type, destination_account_id,
    allocation_ratio, allocation_amount, allocation_status,
    created_at, updated_at, status
) VALUES
    (@rp_dsc, 'SECURITIES', @acct_sec_usd, 60.00, 20.4000, 'PENDING', @now, @now, 'ACTIVE'),
    (@rp_dsc, 'DEPOSIT',    @acct_deposit,  40.00, 13.6000, 'PENDING', @now, @now, 'ACTIVE');

-- =====================================================================
-- 10. holdings / holding_lots (SPCX 5주)
-- =====================================================================
INSERT INTO holdings (
    user_id, product_id, total_quantity, average_price, currency,
    created_at, updated_at, status
)
SELECT 9999, p.id, 5, 135.0000, 'USD', @now, @now, 'ACTIVE'
FROM investment_products p WHERE p.ticker = 'SPCX' LIMIT 1;
SET @holding_spcx = LAST_INSERT_ID();

INSERT INTO holding_lots (
    holding_id, user_id, product_id, source_type, source_id,
    quantity, remaining_quantity, acquisition_price, acquired_at,
    created_at, updated_at, status
)
SELECT @holding_spcx, 9999, p.id, 'IPO_ALLOCATION', @sub_spcx,
       5, 5, 135.0000, DATE_SUB(CURDATE(), INTERVAL 18 DAY),
       @now, @now, 'ACTIVE'
FROM investment_products p WHERE p.ticker = 'SPCX' LIMIT 1;

-- =====================================================================
-- 확인
-- =====================================================================
SELECT '=== 유저 ===' AS '';
SELECT id, name, email, onboarding_status FROM users WHERE id = 9999;

SELECT '=== 계좌 ===' AS '';
SELECT id, account_type, currency, balance, reserved_balance
FROM financial_accounts WHERE user_id = 9999 ORDER BY id;

SELECT '=== 청약 내역 ===' AS '';
SELECT s.id, i.ticker, s.subscription_status, s.result_status,
       s.requested_shares, s.allocated_shares, s.refund_amount
FROM ipo_subscriptions s JOIN ipos i ON s.ipo_id = i.id
WHERE s.user_id = 9999 ORDER BY s.id;

SELECT '=== 리턴플랜 ===' AS '';
SELECT rp.id, rp.total_refund_amount, rp.plan_status,
       rpa.destination_type, rpa.allocation_ratio, rpa.allocation_amount
FROM return_plans rp
JOIN return_plan_allocations rpa ON rpa.return_plan_id = rp.id
WHERE rp.user_id = 9999;

SELECT '=== 보유 주식 ===' AS '';
SELECT h.total_quantity, p.ticker, h.average_price
FROM holdings h JOIN investment_products p ON h.product_id = p.id
WHERE h.user_id = 9999;
