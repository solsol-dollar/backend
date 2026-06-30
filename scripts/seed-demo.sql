-- =====================================================================
-- 시연용 풀 스테이트 유저 (이클립스)
-- 이름: 이클립스 / 이메일: demo@eclipse.dev / 간편비밀번호: 000000
-- user_id = 9999 (고정 — 환경 무관하게 충돌 없음)
--
-- 전제조건: seed-users.sql 실행 완료
--   financial_accounts 15~35 (7명×3개) 존재
--   → user_id=9999 계좌 AUTO_INCREMENT = 36~38
--       @acct_sec_usd = 36 (CMA USD)
--       [KRW]         = 37 (CMA KRW, 변수 불필요)
--       @acct_deposit = 38 (DEPOSIT USD)
--
-- 실행 방법 (멱등 — 기존 데이터 자동 초기화 후 재생성):
--   seed-demo.sql 단독  또는
--   seed-demo-rollback.sql → seed-demo.sql
--
-- 청약 시나리오 8종 (qa_demo_seed_spec.md 기준):
--   MANE(78)  50주 신청 → 10주(20%) 배정, DEPOSITED, 리턴플랜 201 EXECUTED
--   EIKN(76)  30주 신청 → 20주(67%) 배정, DEPOSITED, 리턴플랜 206 EXECUTED
--   CBRS(98)  10주 신청 →  2주(20%) 배정, DEPOSITED, 리턴플랜 202 EXECUTED
--   LFTO(65)  80주 신청 →  0주(낙첨),      DEPOSITED, 리턴플랜 203 EXECUTED
--   SPCX(89)  20주 신청 →  0주(낙첨),      DEPOSITED, 리턴플랜 204 EXECUTED
--   DSC(63)  100주 신청 → 30주(30%) 배정,  DEPOSITED, 리턴플랜 205 EXECUTED
--   BLZX(202) 60주 신청 → 15주(25%) 배정,  COMPLETED, 리턴플랜 없음 (환불 07-01 대기)
--   NXTN(203) 20주 신청 →  4주(20%) 배정,  COMPLETED, 리턴플랜 없음 (환불 07-01 대기)
--
-- 신규 IPO 3종 (id 201~203):
--   VRNT(201) UPCOMING, 청약 없음 (07-01 마감)
--   BLZX(202) LISTED 06-30, 수익 종목 ($22 → $28.50)
--   NXTN(203) LISTED 06-30, 수익 종목 ($45 → $52.30)
--
-- 계좌 최종 잔액:
--   CMA USD (id=36)  balance=$9,027.75  reserved=$2,242.20
--   DEPOSIT (id=38)  balance=$2,720.00
--
-- 금액 모델:
--   증거금 = subscription_amount × 1.01  → balance_holds.amount
--   fee    = allocated_amount × 0.005
--   refund = 증거금 − allocated_amount − fee
-- =====================================================================

SET @demo_uid = 9999;
SET @now      = NOW();

-- =====================================================================
-- 0. 기존 데모 데이터 초기화 (멱등 보장, FK 역순)
-- =====================================================================

DELETE hl FROM holding_lots hl
    JOIN holdings h ON hl.holding_id = h.id WHERE h.user_id = @demo_uid;
DELETE FROM holdings WHERE user_id = @demo_uid;

DELETE tt FROM transfer_transactions tt
    JOIN return_plan_allocations rpa ON tt.allocation_id = rpa.id
    JOIN return_plans rp ON rpa.return_plan_id = rp.id WHERE rp.user_id = @demo_uid;
DELETE rpa FROM return_plan_allocations rpa
    JOIN return_plans rp ON rpa.return_plan_id = rp.id WHERE rp.user_id = @demo_uid;
DELETE FROM return_plans WHERE user_id = @demo_uid;

DELETE bh FROM balance_holds bh
    JOIN ipo_subscriptions s ON bh.subscription_id = s.id WHERE s.user_id = @demo_uid;
DELETE FROM ipo_subscriptions WHERE user_id = @demo_uid;

DELETE FROM rp_contracts             WHERE user_id = @demo_uid;
DELETE FROM trade_orders             WHERE user_id = @demo_uid;
DELETE FROM fx_exchange_transactions WHERE user_id = @demo_uid;
DELETE FROM transfer_transactions    WHERE user_id = @demo_uid;

DELETE ct FROM card_transactions ct
    JOIN cards c ON ct.card_id = c.id WHERE c.user_id = @demo_uid;
DELETE FROM cards              WHERE user_id = @demo_uid;

DELETE FROM favorite_ipos      WHERE user_id = @demo_uid;
DELETE FROM notifications      WHERE user_id = @demo_uid;
DELETE FROM idle_dollar_triggers WHERE user_id = @demo_uid;
DELETE FROM notification_settings WHERE user_id = @demo_uid;
DELETE FROM investment_profiles  WHERE user_id = @demo_uid;
DELETE FROM financial_accounts   WHERE user_id = @demo_uid;
DELETE FROM users                WHERE id = @demo_uid;

-- 신규 IPO 삭제 (이전 실행 흔적 제거, 구독 먼저 삭제 완료)
DELETE FROM ipos                WHERE id IN (201, 202, 203);
DELETE FROM investment_products WHERE ticker IN ('VRNT', 'BLZX', 'NXTN');

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
-- 2. 금융 계좌 (AUTO_INCREMENT — seed-users.sql 이후 id=36~38)
--
--    CMA USD (id=36) → @acct_sec_usd
--      balance       = 초기 $5,000
--                    − Σ allocated_amount ($1,920)
--                    + Σ EXECUTED refund  ($8,157.75)
--                    − Σ DEP allocation   ($2,720.00)
--                    = $9,027.75 ✓
--      reserved      = BLZX $1,333.20 + NXTN $909.00 = $2,242.20
--
--    DEPOSIT USD (id=38) → @acct_deposit
--      balance = Σ DEP allocation = $2,720.00 ✓
-- =====================================================================
INSERT INTO financial_accounts (
    user_id, account_type, institution_type, institution_name,
    account_name, account_number, virtual_account_number,
    currency, balance, reserved_balance, interest_rate, maturity_date,
    linked, linked_at, created_at, updated_at, status
) VALUES (
    9999, 'SECURITIES', 'SECURITIES_FIRM', '신한투자증권',
    '신한투자증권 CMA 외화 계좌', '110-999-000001', '110-999-100001',
    'USD', 9027.7500, 2242.2000, NULL, NULL,
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
    'USD', 2720.0000, 0.0000, 2.5000, NULL,
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
-- 5. investment_products — LISTED IPO 종목 (BLZX, NXTN)
--    VRNT는 UPCOMING이므로 product 미생성
-- =====================================================================
INSERT INTO investment_products (
    product_type, ticker, product_name, exchange_name, currency, sector,
    created_at, updated_at, status
) VALUES ('STOCK', 'BLZX', 'Blazix Corporation', 'NASDAQ', 'USD', 'Technology', @now, @now, 'ACTIVE');
SET @prod_blzx = LAST_INSERT_ID();

INSERT INTO investment_products (
    product_type, ticker, product_name, exchange_name, currency, sector,
    created_at, updated_at, status
) VALUES ('STOCK', 'NXTN', 'Nextion Therapeutics Inc', 'NYSE', 'USD', 'Healthcare', @now, @now, 'ACTIVE');
SET @prod_nxtn = LAST_INSERT_ID();

-- =====================================================================
-- 6. 신규 IPO (id 201~203, 명세 §8 ID 범위)
--
--   VRNT(201): UPCOMING, offer=$31, 07-01 마감
--   BLZX(202): LISTED,   offer=$22, current=$28.50 (수익 종목)
--   NXTN(203): LISTED,   offer=$45, current=$52.30 (수익 종목)
-- =====================================================================
INSERT INTO ipos (
    id, product_id, ticker, company_name, exchange_name, sector,
    subscription_start_date, subscription_end_date,
    listing_date, refund_date, deposit_date,
    confirmed_offer_price, ipo_status, current_price,
    created_at, updated_at, status
) VALUES
    (201, NULL,       'VRNT', 'Verant Networks Corp',       'NASDAQ', 'Technology',
     '2026-06-25', '2026-07-01', '2026-07-02', '2026-07-03', '2026-07-03',
     31.0000, 'UPCOMING', NULL,    @now, @now, 'ACTIVE'),
    (202, @prod_blzx, 'BLZX', 'Blazix Corporation',         'NASDAQ', 'Technology',
     '2026-06-23', '2026-06-29', '2026-06-30', '2026-07-01', '2026-06-30',
     22.0000, 'LISTED',  28.5000, @now, @now, 'ACTIVE'),
    (203, @prod_nxtn, 'NXTN', 'Nextion Therapeutics Inc',   'NYSE',   'Healthcare',
     '2026-06-23', '2026-06-29', '2026-06-30', '2026-07-01', '2026-06-30',
     45.0000, 'LISTED',  52.3000, @now, @now, 'ACTIVE');

-- =====================================================================
-- 7. ipo_subscriptions (id 2001~2006, 2010~2011)
--
-- 금액 검증:
--   MANE  : $850×1.01=$858.50  / 10주×$17=$170   / fee=$0.85  / refund=$687.65   ✓
--   EIKN  : $540×1.01=$545.40  / 20주×$18=$360   / fee=$1.80  / refund=$183.60   ✓
--   CBRS  : $1,850×1.01=$1,868.50 / 2주×$185=$370 / fee=$1.85 / refund=$1,496.65 ✓
--   LFTO  : $1,840×1.01=$1,858.40 / 0주(offer=$23, 80주 신청) / fee=$0 / refund=$1,858.40 ✓
--   SPCX  : $2,700×1.01=$2,727.00 / 0주(offer=$135, 20주 신청) / fee=$0 / refund=$2,727.00 ✓
--   DSC   : $1,700×1.01=$1,717.00 / 30주×$17=$510 / fee=$2.55  / refund=$1,204.45 ✓
--   BLZX  : $1,320×1.01=$1,333.20 / 15주×$22=$330 / fee=$1.65  / refund=$1,001.55 ✓
--   NXTN  : $900×1.01=$909.00  / 4주×$45=$180    / fee=$0.90  / refund=$728.10   ✓
-- =====================================================================
INSERT INTO ipo_subscriptions (
    id, user_id, ipo_id, securities_account_id,
    requested_shares, offer_price, subscription_amount, currency,
    subscription_status, execution_mode, subscribed_at,
    allocated_shares, allocated_amount, refund_amount, allocation_rate,
    result_status, confirmed_at,
    created_at, updated_at, status
) VALUES
    -- [1] MANE(78) — 50주 × $17, 10주 20% 배정, DEPOSITED
    (2001, 9999, 78, @acct_sec_usd,
     50, 17.0000, 850.0000, 'USD',
     'CONFIRMED', 'MOCK', '2026-04-10 10:00:00',
     10, 170.0000, 687.6500, 0.2000,
     'DEPOSITED', '2026-04-11 10:00:00',
     @now, @now, 'ACTIVE'),

    -- [2] EIKN(76) — 30주 × $18, 20주 67% 배정, DEPOSITED
    (2002, 9999, 76, @acct_sec_usd,
     30, 18.0000, 540.0000, 'USD',
     'CONFIRMED', 'MOCK', '2026-04-18 10:00:00',
     20, 360.0000, 183.6000, 0.6667,
     'DEPOSITED', '2026-04-19 10:00:00',
     @now, @now, 'ACTIVE'),

    -- [3] CBRS(98) — 10주 × $185, 2주 20% 배정, DEPOSITED
    (2003, 9999, 98, @acct_sec_usd,
     10, 185.0000, 1850.0000, 'USD',
     'CONFIRMED', 'MOCK', '2026-05-08 10:00:00',
     2, 370.0000, 1496.6500, 0.2000,
     'DEPOSITED', '2026-05-09 10:00:00',
     @now, @now, 'ACTIVE'),

    -- [4] LFTO(65) — 80주 × $23, 0주 낙첨 전액환급, DEPOSITED
    (2004, 9999, 65, @acct_sec_usd,
     80, 23.0000, 1840.0000, 'USD',
     'CONFIRMED', 'MOCK', '2026-05-15 10:00:00',
     0, 0.0000, 1858.4000, 0.0000,
     'DEPOSITED', '2026-05-16 10:00:00',
     @now, @now, 'ACTIVE'),

    -- [5] SPCX(89) — 20주 × $135, 0주 낙첨 전액환급, DEPOSITED
    (2005, 9999, 89, @acct_sec_usd,
     20, 135.0000, 2700.0000, 'USD',
     'CONFIRMED', 'MOCK', '2026-05-22 10:00:00',
     0, 0.0000, 2727.0000, 0.0000,
     'DEPOSITED', '2026-05-23 10:00:00',
     @now, @now, 'ACTIVE'),

    -- [6] DSC(63) — 100주 × $17, 30주 30% 배정, DEPOSITED
    (2006, 9999, 63, @acct_sec_usd,
     100, 17.0000, 1700.0000, 'USD',
     'CONFIRMED', 'MOCK', '2026-06-01 10:00:00',
     30, 510.0000, 1204.4500, 0.3000,
     'DEPOSITED', '2026-06-02 10:00:00',
     @now, @now, 'ACTIVE'),

    -- [7] BLZX(202) — 60주 × $22, 15주 25% 배정, COMPLETED (환불 07-01 대기)
    (2010, 9999, 202, @acct_sec_usd,
     60, 22.0000, 1320.0000, 'USD',
     'CONFIRMED', 'MOCK', '2026-06-22 10:00:00',
     15, 330.0000, 1001.5500, 0.2500,
     'COMPLETED', '2026-06-22 11:00:00',
     @now, @now, 'ACTIVE'),

    -- [8] NXTN(203) — 20주 × $45, 4주 20% 배정, COMPLETED (환불 07-01 대기)
    (2011, 9999, 203, @acct_sec_usd,
     20, 45.0000, 900.0000, 'USD',
     'CONFIRMED', 'MOCK', '2026-06-22 10:00:00',
     4, 180.0000, 728.1000, 0.2000,
     'COMPLETED', '2026-06-22 11:00:00',
     @now, @now, 'ACTIVE');

-- =====================================================================
-- 8. balance_holds
--
--    amount = 증거금 = subscription_amount × 1.01
--    6건(MANE~DSC): RELEASED, released_at=listing_date 21:30, settled_at=NULL
--    BLZX·NXTN:     LOCKED (상장 06-30 당일, IpoAllocationJob 미실행)
-- =====================================================================
INSERT INTO balance_holds (
    account_id, subscription_id, amount,
    hold_status, released_at, settled_at,
    created_at, updated_at, status
) VALUES
    (@acct_sec_usd, 2001,  858.5000, 'RELEASED', '2026-04-20 21:30:00', NULL, '2026-04-10 10:00:00', '2026-04-20 21:30:00', 'ACTIVE'), -- MANE
    (@acct_sec_usd, 2002,  545.4000, 'RELEASED', '2026-04-28 21:30:00', NULL, '2026-04-18 10:00:00', '2026-04-28 21:30:00', 'ACTIVE'), -- EIKN
    (@acct_sec_usd, 2003, 1868.5000, 'RELEASED', '2026-05-18 21:30:00', NULL, '2026-05-08 10:00:00', '2026-05-18 21:30:00', 'ACTIVE'), -- CBRS
    (@acct_sec_usd, 2004, 1858.4000, 'RELEASED', '2026-05-25 21:30:00', NULL, '2026-05-15 10:00:00', '2026-05-25 21:30:00', 'ACTIVE'), -- LFTO
    (@acct_sec_usd, 2005, 2727.0000, 'RELEASED', '2026-06-01 21:30:00', NULL, '2026-05-22 10:00:00', '2026-06-01 21:30:00', 'ACTIVE'), -- SPCX
    (@acct_sec_usd, 2006, 1717.0000, 'RELEASED', '2026-06-10 21:30:00', NULL, '2026-06-01 10:00:00', '2026-06-10 21:30:00', 'ACTIVE'), -- DSC
    (@acct_sec_usd, 2010, 1333.2000, 'LOCKED',   NULL,                  NULL, '2026-06-22 10:00:00', @now,                  'ACTIVE'), -- BLZX
    (@acct_sec_usd, 2011,  909.0000, 'LOCKED',   NULL,                  NULL, '2026-06-22 10:00:00', @now,                  'ACTIVE'); -- NXTN

-- =====================================================================
-- 9. return_plans (id 201~206, 전부 EXECUTED)
--
--   plan_id → subscription → IPO
--     201 → 2001 → MANE  / 202 → 2003 → CBRS
--     203 → 2004 → LFTO  / 204 → 2005 → SPCX
--     205 → 2006 → DSC   / 206 → 2002 → EIKN
--
--   BLZX(2010) / NXTN(2011): 환불일 07-01 미도래, 리턴플랜 없음
-- =====================================================================
INSERT INTO return_plans (
    id, user_id, subscription_id, next_ipo_id,
    total_refund_amount, currency,
    current_securities_balance, savings_interest_rate,
    plan_status, executed_at,
    created_at, updated_at, status
) VALUES
    (201, 9999, 2001, NULL,  687.6500, 'USD', 5000.0000, 2.5000, 'EXECUTED', '2026-04-21 10:00:00', '2026-04-20 21:30:00', '2026-04-21 10:00:00', 'ACTIVE'), -- MANE
    (202, 9999, 2003, NULL, 1496.6500, 'USD', 5000.0000, 2.5000, 'EXECUTED', '2026-05-19 10:00:00', '2026-05-18 21:30:00', '2026-05-19 10:00:00', 'ACTIVE'), -- CBRS
    (203, 9999, 2004, NULL, 1858.4000, 'USD', 5000.0000, 2.5000, 'EXECUTED', '2026-05-26 10:00:00', '2026-05-25 21:30:00', '2026-05-26 10:00:00', 'ACTIVE'), -- LFTO
    (204, 9999, 2005, NULL, 2727.0000, 'USD', 5000.0000, 2.5000, 'EXECUTED', '2026-06-02 10:00:00', '2026-06-01 21:30:00', '2026-06-02 10:00:00', 'ACTIVE'), -- SPCX
    (205, 9999, 2006, NULL, 1204.4500, 'USD', 5000.0000, 2.5000, 'EXECUTED', '2026-06-11 10:00:00', '2026-06-10 21:30:00', '2026-06-11 10:00:00', 'ACTIVE'), -- DSC
    (206, 9999, 2002, NULL,  183.6000, 'USD', 5000.0000, 2.5000, 'EXECUTED', '2026-04-29 10:00:00', '2026-04-28 21:30:00', '2026-04-29 10:00:00', 'ACTIVE'); -- EIKN

-- =====================================================================
-- 10. return_plan_allocations (id 2001~2012, EXECUTED)
--
--   SECURITIES → @acct_sec_usd / DEPOSIT → @acct_deposit
--
--   DEP = floor(total_refund × dep_ratio / 100)  [정수]
--   SEC = total_refund − DEP                      [나머지]
--
--   plan 201(MANE  $687.65)  : DEP 10%→$68    SEC→$619.65  ✓
--   plan 206(EIKN  $183.60)  : DEP 40%→$73    SEC→$110.60  ✓
--   plan 202(CBRS  $1,496.65): DEP 30%→$448   SEC→$1,048.65 ✓
--   plan 203(LFTO  $1,858.40): DEP 40%→$743   SEC→$1,115.40 ✓
--   plan 204(SPCX  $2,727.00): DEP 20%→$545   SEC→$2,182.00 ✓
--   plan 205(DSC   $1,204.45): DEP 70%→$843   SEC→$361.45   ✓
--   Σ DEP = 68+73+448+743+545+843 = $2,720.00 = DEPOSIT 잔액 ✓
-- =====================================================================
INSERT INTO return_plan_allocations (
    id, return_plan_id, destination_type, destination_account_id,
    allocation_ratio, allocation_amount, allocation_status,
    created_at, updated_at, status
) VALUES
    -- plan 201 (MANE, $687.65): SEC 90% / DEP 10%
    (2001, 201, 'SECURITIES', @acct_sec_usd,  90.00,  619.6500, 'EXECUTED', '2026-04-21 10:00:00', '2026-04-21 10:00:00', 'ACTIVE'),
    (2002, 201, 'DEPOSIT',    @acct_deposit,  10.00,   68.0000, 'EXECUTED', '2026-04-21 10:00:00', '2026-04-21 10:00:00', 'ACTIVE'),
    -- plan 202 (CBRS, $1,496.65): SEC 70% / DEP 30%
    (2003, 202, 'SECURITIES', @acct_sec_usd,  70.00, 1048.6500, 'EXECUTED', '2026-05-19 10:00:00', '2026-05-19 10:00:00', 'ACTIVE'),
    (2004, 202, 'DEPOSIT',    @acct_deposit,  30.00,  448.0000, 'EXECUTED', '2026-05-19 10:00:00', '2026-05-19 10:00:00', 'ACTIVE'),
    -- plan 203 (LFTO, $1,858.40): SEC 60% / DEP 40%
    (2005, 203, 'SECURITIES', @acct_sec_usd,  60.00, 1115.4000, 'EXECUTED', '2026-05-26 10:00:00', '2026-05-26 10:00:00', 'ACTIVE'),
    (2006, 203, 'DEPOSIT',    @acct_deposit,  40.00,  743.0000, 'EXECUTED', '2026-05-26 10:00:00', '2026-05-26 10:00:00', 'ACTIVE'),
    -- plan 204 (SPCX, $2,727.00): SEC 80% / DEP 20%
    (2007, 204, 'SECURITIES', @acct_sec_usd,  80.00, 2182.0000, 'EXECUTED', '2026-06-02 10:00:00', '2026-06-02 10:00:00', 'ACTIVE'),
    (2008, 204, 'DEPOSIT',    @acct_deposit,  20.00,  545.0000, 'EXECUTED', '2026-06-02 10:00:00', '2026-06-02 10:00:00', 'ACTIVE'),
    -- plan 205 (DSC, $1,204.45): SEC 30% / DEP 70%
    (2009, 205, 'SECURITIES', @acct_sec_usd,  30.00,  361.4500, 'EXECUTED', '2026-06-11 10:00:00', '2026-06-11 10:00:00', 'ACTIVE'),
    (2010, 205, 'DEPOSIT',    @acct_deposit,  70.00,  843.0000, 'EXECUTED', '2026-06-11 10:00:00', '2026-06-11 10:00:00', 'ACTIVE'),
    -- plan 206 (EIKN, $183.60): SEC 60% / DEP 40%
    (2011, 206, 'SECURITIES', @acct_sec_usd,  60.00,  110.6000, 'EXECUTED', '2026-04-29 10:00:00', '2026-04-29 10:00:00', 'ACTIVE'),
    (2012, 206, 'DEPOSIT',    @acct_deposit,  40.00,   73.0000, 'EXECUTED', '2026-04-29 10:00:00', '2026-04-29 10:00:00', 'ACTIVE');

-- =====================================================================
-- 확인
-- =====================================================================
SELECT '=== 유저 ===' AS '';
SELECT id, name, email, onboarding_status FROM users WHERE id = 9999;

SELECT '=== 계좌 ===' AS '';
SELECT id, account_type, currency, balance, reserved_balance
FROM financial_accounts WHERE user_id = 9999 ORDER BY id;

SELECT '=== 신규 IPO ===' AS '';
SELECT id, ticker, ipo_status, subscription_end_date, listing_date,
       confirmed_offer_price, current_price
FROM ipos WHERE id IN (201, 202, 203) ORDER BY id;

SELECT '=== 청약 내역 (신청순) ===' AS '';
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
WHERE s.user_id = 9999 ORDER BY bh.subscription_id;

SELECT '=== 리턴플랜 ===' AS '';
SELECT rp.id AS plan_id,
       i.ticker AS source_ipo,
       rp.total_refund_amount,
       rp.plan_status,
       rpa.destination_type,
       rpa.allocation_ratio,
       rpa.allocation_amount,
       rpa.allocation_status
FROM return_plans rp
JOIN ipo_subscriptions sub ON sub.id = rp.subscription_id
JOIN ipos i ON i.id = sub.ipo_id
JOIN return_plan_allocations rpa ON rpa.return_plan_id = rp.id
WHERE rp.user_id = 9999
ORDER BY rp.executed_at, rpa.destination_type;

SELECT '=== DEP 합계 검증 ($2,720.00 이어야 함) ===' AS '';
SELECT SUM(rpa.allocation_amount) AS dep_total
FROM return_plan_allocations rpa
JOIN return_plans rp ON rpa.return_plan_id = rp.id
WHERE rp.user_id = 9999 AND rpa.destination_type = 'DEPOSIT';
