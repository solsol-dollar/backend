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
-- 청약 시나리오 5종 (RDS IPO ID / 실제 날짜 기준):
--   SPCX (id=89)  sub 05-29~06-11 / listing 06-12 / deposit 06-15
--                 → 5주 100% 배정, 주식 입고 완료 (히스토리)
--   FCBM (id=77)  sub 06-01~06-17 / listing 06-18 / refund  06-19
--                 → 낙첨, 전액 환불 완료, 리턴플랜 EXECUTED (→ DSC)
--   DPC  (id=91)  sub 06-11~06-24 / listing 06-25 / refund  06-30(오늘)
--                 → 6주 75% 배정, 오늘 환불 예정, 리턴플랜 없음 (Job 자동생성)
--   DSC  (id=63)  sub 06-12~06-24 / listing 06-25 / refund  07-01(내일)
--                 → 8주 80% 배정, 내일 환불 예정, 리턴플랜 DRAFT (→ BSP)
--   BSP  (id=69)  sub 06-17~06-30(오늘 마감) / listing 07-01
--                 → 10주 청약 확정, 배정 대기
--
-- 체인: FCBM(환불 06-19) --[EXECUTED]--> DSC(환불 07-01) --[DRAFT]--> BSP
-- =====================================================================

SET @demo_uid = 9999;
SET @now      = NOW();

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
--    Securities USD : balance $10,000
--                    reserved $704 = DPC $264 + DSC $170 + BSP $270 (LOCKED 합계)
--    Securities KRW : (원화 계좌)
--    DEPOSIT USD    : $2,000 (리턴플랜 배분 대상)
-- =====================================================================
INSERT INTO financial_accounts (
    user_id, account_type, institution_type, institution_name,
    account_name, account_number, virtual_account_number,
    currency, balance, reserved_balance, interest_rate, maturity_date,
    linked, linked_at, created_at, updated_at, status
) VALUES (
    9999, 'SECURITIES', 'SECURITIES_FIRM', '신한투자증권',
    '신한투자증권 CMA 외화 계좌', '110-999-000001', '110-999-100001',
    'USD', 10000.0000, 704.0000, NULL, NULL,
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
-- 5. IPO 상태 업데이트 (실제 DB 날짜 고정값 기준)
-- =====================================================================
-- BSP(69): 오늘(06-30) 청약 마감, 내일 상장
UPDATE ipos SET ipo_status = 'OPEN'
    WHERE id = 69;

-- DSC(63): 청약 마감됨, 내일(07-01) 환불
UPDATE ipos SET ipo_status = 'CLOSED',
    refund_date = '2026-07-01', deposit_date = '2026-07-01'
    WHERE id = 63;

-- DPC(91): 청약 마감됨, 오늘(06-30) 환불
UPDATE ipos SET ipo_status = 'CLOSED',
    refund_date = '2026-06-30', deposit_date = '2026-06-30'
    WHERE id = 91;

-- FCBM(77): 청약 마감됨 (환불 06-19 이미 완료)
UPDATE ipos SET ipo_status = 'CLOSED'
    WHERE id = 77;

-- SPCX(89): 청약 마감됨 (입고 06-15 이미 완료)
UPDATE ipos SET ipo_status = 'CLOSED'
    WHERE id = 89;

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
-- 7. 청약 내역 5건 (청약 시간 순)
-- =====================================================================

-- [1] SPCX(89) ── 2026-06-05 청약, 06-12 배정, 06-15 입고
--     5주 × $135 = $675 / 5주 100% 배정 / refund=$0 / DEPOSITED
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
    'CONFIRMED', 'MOCK', '2026-06-05 10:00:00',
    5, 675.0000, 0.0000, 100.0000,
    'DEPOSITED', '2026-06-05 11:00:00',
    @now, @now, 'ACTIVE'
);
SET @sub_spcx = LAST_INSERT_ID();

-- [2] FCBM(77) ── 2026-06-10 청약, 06-18 배정(낙첨), 06-19 전액 환불
--     10주 × $12.5 = $125 / 0주 배정 / refund=$125 / DEPOSITED
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
    'CONFIRMED', 'MOCK', '2026-06-10 10:00:00',
    0, 0.0000, 125.0000, 0.0000,
    'DEPOSITED', '2026-06-10 11:00:00',
    @now, @now, 'ACTIVE'
);
SET @sub_fcbm = LAST_INSERT_ID();

-- [3] DPC(91) ── 2026-06-18 청약, 06-25 배정, 06-30(오늘) 환불 예정
--     8주 × $33 = $264 / 6주 75% 배정 / refund=$66 / COMPLETED
INSERT INTO ipo_subscriptions (
    user_id, ipo_id, securities_account_id,
    requested_shares, offer_price, subscription_amount, currency,
    subscription_status, execution_mode, subscribed_at,
    allocated_shares, allocated_amount, refund_amount, allocation_rate,
    result_status, confirmed_at,
    created_at, updated_at, status
) VALUES (
    9999, 91, @acct_sec_usd,
    8, 33.0000, 264.0000, 'USD',
    'CONFIRMED', 'MOCK', '2026-06-18 10:00:00',
    6, 198.0000, 66.0000, 75.0000,
    'COMPLETED', '2026-06-18 11:00:00',
    @now, @now, 'ACTIVE'
);
SET @sub_dpc = LAST_INSERT_ID();

-- [4] DSC(63) ── 2026-06-20 청약, 06-25 배정, 07-01(내일) 환불 예정
--     10주 × $17 = $170 / 8주 80% 배정 / refund=$34 / COMPLETED
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
    'CONFIRMED', 'MOCK', '2026-06-20 10:00:00',
    8, 136.0000, 34.0000, 80.0000,
    'COMPLETED', '2026-06-20 11:00:00',
    @now, @now, 'ACTIVE'
);
SET @sub_dsc = LAST_INSERT_ID();

-- [5] BSP(69) ── 2026-06-28 청약, 오늘(06-30) 마감, 내일(07-01) 상장
--     10주 × $27 = $270 / 배정 미결정 / result_status=NULL
INSERT INTO ipo_subscriptions (
    user_id, ipo_id, securities_account_id,
    requested_shares, offer_price, subscription_amount, currency,
    subscription_status, execution_mode, subscribed_at,
    result_status, confirmed_at,
    created_at, updated_at, status
) VALUES (
    9999, 69, @acct_sec_usd,
    10, 27.0000, 270.0000, 'USD',
    'CONFIRMED', 'MOCK', '2026-06-28 10:00:00',
    NULL, '2026-06-28 11:00:00',
    @now, @now, 'ACTIVE'
);
SET @sub_bsp = LAST_INSERT_ID();

-- =====================================================================
-- 8. balance_holds
--    LOCKED  : 배정 대기 / 환불일 미도래 (청약금 여전히 잠금)
--    RELEASED: 환불 또는 입고 완료 (잠금 해제)
--
--    SPCX  : RELEASED 06-15 (입고 완료)
--    FCBM  : RELEASED 06-19 (환불 완료)
--    DPC   : LOCKED    (오늘 환불 예정, 미처리)
--    DSC   : LOCKED    (내일 환불 예정)
--    BSP   : LOCKED    (배정 대기)
-- =====================================================================
INSERT INTO balance_holds (
    account_id, subscription_id, amount,
    hold_status, released_at, settled_at,
    created_at, updated_at, status
) VALUES
    (@acct_sec_usd, @sub_spcx,  675.0000, 'RELEASED', '2026-06-15 09:00:00', '2026-06-14 09:00:00', '2026-06-05 10:00:00', '2026-06-15 09:00:00', 'ACTIVE'),
    (@acct_sec_usd, @sub_fcbm,  125.0000, 'RELEASED', '2026-06-19 09:00:00', '2026-06-18 09:00:00', '2026-06-10 10:00:00', '2026-06-19 09:00:00', 'ACTIVE'),
    (@acct_sec_usd, @sub_dpc,   264.0000, 'LOCKED',   NULL,                  NULL,                  '2026-06-18 10:00:00', @now,                  'ACTIVE'),
    (@acct_sec_usd, @sub_dsc,   170.0000, 'LOCKED',   NULL,                  NULL,                  '2026-06-20 10:00:00', @now,                  'ACTIVE'),
    (@acct_sec_usd, @sub_bsp,   270.0000, 'LOCKED',   NULL,                  NULL,                  '2026-06-28 10:00:00', @now,                  'ACTIVE');

-- =====================================================================
-- 9. return_plans + return_plan_allocations
--
--    체인: FCBM(환불 06-19) -EXECUTED-> DSC(환불 07-01) -DRAFT-> BSP
--
--    FCBM 리턴플랜: EXECUTED 06-19, $125 환불
--                   → Securities 70% = $87.50
--                   → Deposit    30% = $37.50
--
--    DSC  리턴플랜: DRAFT (환불일 07-01 미도래), $34 예정
--                   → Securities 60% = $20.40
--                   → Deposit    40% = $13.60
--
--    DPC  리턴플랜: 없음 (Job이 환불 후 자동생성)
--    SPCX 리턴플랜: 없음 (refund=$0, 배분 대상 금액 없음)
-- =====================================================================

-- [FCBM] EXECUTED — 06-19 환불금 $125 분배 완료
INSERT INTO return_plans (
    user_id, subscription_id, next_ipo_id,
    total_refund_amount, currency, current_securities_balance,
    savings_interest_rate, plan_status, executed_at,
    created_at, updated_at, status
) VALUES (
    9999, @sub_fcbm, 63,
    125.0000, 'USD', 10000.0000,
    2.5000, 'EXECUTED', '2026-06-19 10:00:00',
    '2026-06-19 09:30:00', '2026-06-19 10:00:00', 'ACTIVE'
);
SET @rp_fcbm = LAST_INSERT_ID();

INSERT INTO return_plan_allocations (
    return_plan_id, destination_type, destination_account_id,
    allocation_ratio, allocation_amount, allocation_status,
    created_at, updated_at, status
) VALUES
    (@rp_fcbm, 'SECURITIES', @acct_sec_usd, 70.00,  87.5000, 'EXECUTED', '2026-06-19 10:00:00', '2026-06-19 10:00:00', 'ACTIVE'),
    (@rp_fcbm, 'DEPOSIT',    @acct_deposit,  30.00,  37.5000, 'EXECUTED', '2026-06-19 10:00:00', '2026-06-19 10:00:00', 'ACTIVE');

-- [DSC] DRAFT — 07-01 환불 예정, 아직 미실행
INSERT INTO return_plans (
    user_id, subscription_id, next_ipo_id,
    total_refund_amount, currency, current_securities_balance,
    savings_interest_rate, plan_status, executed_at,
    created_at, updated_at, status
) VALUES (
    9999, @sub_dsc, 69,
    34.0000, 'USD', 10000.0000,
    2.5000, 'DRAFT', NULL,
    @now, @now, 'ACTIVE'
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
-- 10. holdings / holding_lots (SPCX 5주, 입고 2026-06-15)
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
       5, 5, 135.0000, '2026-06-15 09:00:00',
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

SELECT '=== 청약 내역 (시간순) ===' AS '';
SELECT s.id, i.ticker, s.subscribed_at, s.subscription_status, s.result_status,
       s.requested_shares, s.allocated_shares, s.refund_amount
FROM ipo_subscriptions s JOIN ipos i ON s.ipo_id = i.id
WHERE s.user_id = 9999 ORDER BY s.subscribed_at;

SELECT '=== balance_holds ===' AS '';
SELECT bh.subscription_id, i.ticker, bh.amount, bh.hold_status,
       bh.released_at, bh.settled_at
FROM balance_holds bh
JOIN ipo_subscriptions s ON bh.subscription_id = s.id
JOIN ipos i ON s.ipo_id = i.id
WHERE s.user_id = 9999 ORDER BY bh.created_at;

SELECT '=== 리턴플랜 체인 ===' AS '';
SELECT rp.id,
       i_src.ticker AS source_ipo,
       i_nxt.ticker AS next_ipo,
       rp.total_refund_amount,
       rp.plan_status,
       rp.executed_at,
       rpa.destination_type,
       rpa.allocation_ratio,
       rpa.allocation_amount,
       rpa.allocation_status
FROM return_plans rp
JOIN ipo_subscriptions sub ON sub.id = rp.subscription_id
JOIN ipos i_src ON i_src.id = sub.ipo_id
LEFT JOIN ipos i_nxt ON i_nxt.id = rp.next_ipo_id
JOIN return_plan_allocations rpa ON rpa.return_plan_id = rp.id
WHERE rp.user_id = 9999
ORDER BY rp.executed_at, rpa.destination_type;

SELECT '=== 보유 주식 ===' AS '';
SELECT h.total_quantity, p.ticker, h.average_price, hl.acquired_at
FROM holdings h
JOIN investment_products p ON h.product_id = p.id
JOIN holding_lots hl ON hl.holding_id = h.id
WHERE h.user_id = 9999;
