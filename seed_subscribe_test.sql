-- =============================================================================
-- 전체 시나리오 시드 데이터 (user_id = 1 기준)
--
-- 커버하는 케이스:
--  1.  청약 가능 (OPEN) × 2종목 — 시간 09:00~17:00 검증
--  2.  청약예정 (UPCOMING) — subscriptionStartDate 미래, confirmed_offer_price=NULL
--  3.  배정완료 + 리턴플랜 수정 가능 (refund=내일 영업일) — RDDT
--  4.  오늘 상장 + IpoAllocationJob 배정 대상 — resultStatus=PENDING
--  5.  오늘 상장 + IpoListingJob 입고 대상 (미입고) — COMPLETED, no holding_lot
--  6.  오늘 상장 + IpoListingJob 중복 방지 — COMPLETED, holding_lot 존재
--  7.  오늘 환불일 + DRAFT 리턴플랜 있음 (정산 대상)
--  8.  오늘 환불일 + 리턴플랜 없음 (자동 생성 대상)
--  9.  취소된 청약 (CANCELLED) — balance_hold RELEASED
--  10. 배정완료 + 상장완료 + 주식 입고됨 (ARM, 과거) — 히스토리 확인용
--
-- 영업일 계산 (MySQL DAYOFWEEK: 1=일, 2=월, ..., 6=금, 7=토):
--   @next_biz : CURDATE() 다음 영업일
--   @prev_biz : CURDATE() 직전 영업일
-- =============================================================================

SET @next_biz = CASE DAYOFWEEK(CURDATE())
    WHEN 6 THEN DATE_ADD(CURDATE(), INTERVAL 3 DAY)
    WHEN 7 THEN DATE_ADD(CURDATE(), INTERVAL 2 DAY)
    ELSE DATE_ADD(CURDATE(), INTERVAL 1 DAY)
END;

SET @prev_biz = CASE DAYOFWEEK(CURDATE())
    WHEN 2 THEN DATE_SUB(CURDATE(), INTERVAL 3 DAY)
    WHEN 1 THEN DATE_SUB(CURDATE(), INTERVAL 2 DAY)
    ELSE DATE_SUB(CURDATE(), INTERVAL 1 DAY)
END;

-- =============================================================================
-- 0. 사용자
-- =============================================================================
INSERT INTO `users` (`id`, `name`, `email`, `phone_number`, `onboarding_status`, `created_at`, `updated_at`, `status`)
VALUES (1, '김희선', 'test@example.com', '010-1234-5678', 'COMPLETED', NOW(), NOW(), 'ACTIVE')
ON DUPLICATE KEY UPDATE `name` = `name`;

-- =============================================================================
-- 1. 금융 계좌
--    account 1 : SECURITIES USD  — 청약/보유 메인 계좌 (reservedBalance=1000: sub 300 LOCKED)
--    account 2 : SECURITIES KRW
--    account 3 : SAVINGS USD     — 리턴플랜 SAVINGS 분배 대상
--    account 4 : DEPOSIT USD     — 리턴플랜 DEPOSIT 분배 대상
-- =============================================================================
INSERT INTO `financial_accounts`
    (`id`, `user_id`, `account_type`, `institution_type`, `institution_name`, `account_name`,
     `account_number`, `currency`, `balance`, `reserved_balance`,
     `linked`, `linked_at`, `created_at`, `updated_at`, `status`)
VALUES
    (1, 1, 'SECURITIES', 'SECURITIES', '신한투자증권', 'CMA USD',
     '270-91-175039[01]', 'USD', 10000.0000, 1000.0000,
     TRUE, NOW(), NOW(), NOW(), 'ACTIVE')
ON DUPLICATE KEY UPDATE `balance` = 10000.0000, `reserved_balance` = 1000.0000;

INSERT INTO `financial_accounts`
    (`id`, `user_id`, `account_type`, `institution_type`, `institution_name`, `account_name`,
     `account_number`, `currency`, `balance`, `reserved_balance`,
     `linked`, `linked_at`, `created_at`, `updated_at`, `status`)
VALUES
    (2, 1, 'SECURITIES', 'SECURITIES', '신한투자증권', 'CMA KRW',
     '270-91-175039[02]', 'KRW', 10000000.0000, 0.0000,
     TRUE, NOW(), NOW(), NOW(), 'ACTIVE')
ON DUPLICATE KEY UPDATE `balance` = 10000000.0000;

INSERT INTO `financial_accounts`
    (`id`, `user_id`, `account_type`, `institution_type`, `institution_name`, `account_name`,
     `account_number`, `currency`, `balance`, `interest_rate`, `maturity_date`,
     `linked`, `linked_at`, `created_at`, `updated_at`, `status`)
VALUES
    (3, 1, 'SAVINGS', 'BANK', '신한은행', '신한 Value-up 외화적립예금',
     '****-****-3391', 'USD', 200.0000, 4.0000, DATE_ADD(CURDATE(), INTERVAL 1 YEAR),
     TRUE, NOW(), NOW(), NOW(), 'ACTIVE')
ON DUPLICATE KEY UPDATE `linked` = TRUE;

INSERT INTO `financial_accounts`
    (`id`, `user_id`, `account_type`, `institution_type`, `institution_name`, `account_name`,
     `account_number`, `currency`, `balance`, `interest_rate`, `maturity_date`,
     `linked`, `linked_at`, `created_at`, `updated_at`, `status`)
VALUES
    (4, 1, 'DEPOSIT', 'BANK', '신한은행', '외화 체인지업 예금',
     '****-****-7742', 'USD', 500.0000, 3.0000, DATE_ADD(CURDATE(), INTERVAL 1 YEAR),
     TRUE, NOW(), NOW(), NOW(), 'ACTIVE')
ON DUPLICATE KEY UPDATE `linked` = TRUE;

-- =============================================================================
-- 2. investment_products — 상장된 종목 (IpoListingJob 에서 product_id 필요)
--    id=101 : LSTK (오늘 상장 테스트용)
--    id=102 : ARM  (과거 상장 완료)
-- =============================================================================
INSERT INTO `investment_products`
    (`id`, `product_type`, `ticker`, `product_name`, `exchange_name`, `currency`, `sector`, `created_at`, `updated_at`, `status`)
VALUES
    (101, 'STOCK', 'LSTK', 'ListingTest Corp.', 'NASDAQ', 'USD', 'Technology', NOW(), NOW(), 'ACTIVE')
ON DUPLICATE KEY UPDATE `ticker` = 'LSTK';

INSERT INTO `investment_products`
    (`id`, `product_type`, `ticker`, `product_name`, `exchange_name`, `currency`, `sector`, `created_at`, `updated_at`, `status`)
VALUES
    (102, 'STOCK', 'ARM', 'Arm Holdings plc', 'NASDAQ', 'USD', 'Semiconductors', NOW(), NOW(), 'ACTIVE')
ON DUPLICATE KEY UPDATE `ticker` = 'ARM';

-- =============================================================================
-- 3. IPO 목록
-- =============================================================================

-- [케이스 1-A] OPEN — 청약 가능 (오늘 청약기간 내, 09:00~17:00 시간 검증 대상)
INSERT INTO `ipos`
    (`id`, `ticker`, `company_name`, `exchange_name`, `sector`,
     `subscription_start_date`, `subscription_end_date`, `listing_date`, `refund_date`, `deposit_date`,
     `offer_price_min`, `offer_price_max`, `confirmed_offer_price`, `minimum_subscription_amount`,
     `ipo_status`, `number_of_shares`, `created_at`, `updated_at`, `status`)
VALUES
    (1, 'CRWV', 'CoreWeave, Inc.', 'NASDAQ', 'Technology',
     DATE_SUB(CURDATE(), INTERVAL 2 DAY), DATE_ADD(CURDATE(), INTERVAL 2 DAY),
     DATE_ADD(CURDATE(), INTERVAL 5 DAY),
     CASE DAYOFWEEK(DATE_ADD(CURDATE(), INTERVAL 5 DAY)) WHEN 6 THEN DATE_ADD(CURDATE(), INTERVAL 8 DAY) WHEN 7 THEN DATE_ADD(CURDATE(), INTERVAL 7 DAY) ELSE DATE_ADD(CURDATE(), INTERVAL 6 DAY) END,
     CASE DAYOFWEEK(DATE_ADD(CURDATE(), INTERVAL 5 DAY)) WHEN 6 THEN DATE_ADD(CURDATE(), INTERVAL 8 DAY) WHEN 7 THEN DATE_ADD(CURDATE(), INTERVAL 7 DAY) ELSE DATE_ADD(CURDATE(), INTERVAL 6 DAY) END,
     38.0000, 40.0000, 40.0000, 100.0000,
     'OPEN', 49000000, NOW(), NOW(), 'ACTIVE')
ON DUPLICATE KEY UPDATE
    `subscription_start_date` = DATE_SUB(CURDATE(), INTERVAL 2 DAY),
    `subscription_end_date`   = DATE_ADD(CURDATE(), INTERVAL 2 DAY),
    `ipo_status` = 'OPEN';

-- [케이스 1-B] OPEN — 두 번째 청약 가능 종목
INSERT INTO `ipos`
    (`id`, `ticker`, `company_name`, `exchange_name`, `sector`,
     `subscription_start_date`, `subscription_end_date`, `listing_date`, `refund_date`, `deposit_date`,
     `offer_price_min`, `offer_price_max`, `confirmed_offer_price`, `minimum_subscription_amount`,
     `ipo_status`, `number_of_shares`, `created_at`, `updated_at`, `status`)
VALUES
    (2, 'CRCL', 'Circle Internet Group, Inc.', 'NYSE', 'Financial Technology',
     DATE_SUB(CURDATE(), INTERVAL 1 DAY), DATE_ADD(CURDATE(), INTERVAL 3 DAY),
     DATE_ADD(CURDATE(), INTERVAL 6 DAY),
     CASE DAYOFWEEK(DATE_ADD(CURDATE(), INTERVAL 6 DAY)) WHEN 6 THEN DATE_ADD(CURDATE(), INTERVAL 9 DAY) WHEN 7 THEN DATE_ADD(CURDATE(), INTERVAL 8 DAY) ELSE DATE_ADD(CURDATE(), INTERVAL 7 DAY) END,
     CASE DAYOFWEEK(DATE_ADD(CURDATE(), INTERVAL 6 DAY)) WHEN 6 THEN DATE_ADD(CURDATE(), INTERVAL 9 DAY) WHEN 7 THEN DATE_ADD(CURDATE(), INTERVAL 8 DAY) ELSE DATE_ADD(CURDATE(), INTERVAL 7 DAY) END,
     27.0000, 28.0000, 28.0000, 100.0000,
     'OPEN', 32000000, NOW(), NOW(), 'ACTIVE')
ON DUPLICATE KEY UPDATE
    `subscription_start_date` = DATE_SUB(CURDATE(), INTERVAL 1 DAY),
    `subscription_end_date`   = DATE_ADD(CURDATE(), INTERVAL 3 DAY),
    `ipo_status` = 'OPEN';

-- [케이스 2] UPCOMING — 청약 시작일이 미래, confirmed_offer_price=NULL (가격 미확정)
INSERT INTO `ipos`
    (`id`, `ticker`, `company_name`, `exchange_name`, `sector`,
     `subscription_start_date`, `subscription_end_date`, `listing_date`, `refund_date`, `deposit_date`,
     `offer_price_min`, `offer_price_max`, `confirmed_offer_price`, `minimum_subscription_amount`,
     `ipo_status`, `number_of_shares`, `created_at`, `updated_at`, `status`)
VALUES
    (3, 'CART', 'Instacart, Inc.', 'NASDAQ', 'Consumer Internet',
     DATE_ADD(CURDATE(), INTERVAL 5 DAY), DATE_ADD(CURDATE(), INTERVAL 8 DAY),
     DATE_ADD(CURDATE(), INTERVAL 11 DAY),
     CASE DAYOFWEEK(DATE_ADD(CURDATE(), INTERVAL 11 DAY)) WHEN 6 THEN DATE_ADD(CURDATE(), INTERVAL 14 DAY) WHEN 7 THEN DATE_ADD(CURDATE(), INTERVAL 13 DAY) ELSE DATE_ADD(CURDATE(), INTERVAL 12 DAY) END,
     CASE DAYOFWEEK(DATE_ADD(CURDATE(), INTERVAL 11 DAY)) WHEN 6 THEN DATE_ADD(CURDATE(), INTERVAL 14 DAY) WHEN 7 THEN DATE_ADD(CURDATE(), INTERVAL 13 DAY) ELSE DATE_ADD(CURDATE(), INTERVAL 12 DAY) END,
     26.0000, 28.0000, NULL, 100.0000,
     'UPCOMING', 15000000, NOW(), NOW(), 'ACTIVE')
ON DUPLICATE KEY UPDATE
    `subscription_start_date` = DATE_ADD(CURDATE(), INTERVAL 5 DAY),
    `subscription_end_date`   = DATE_ADD(CURDATE(), INTERVAL 8 DAY),
    `ipo_status` = 'UPCOMING';

-- [케이스 10] ARM — 과거 상장 완료, 주식 입고 완료 (히스토리 확인용)
INSERT INTO `ipos`
    (`id`, `product_id`, `ticker`, `company_name`, `exchange_name`, `sector`,
     `subscription_start_date`, `subscription_end_date`, `listing_date`, `refund_date`, `deposit_date`,
     `offer_price_min`, `offer_price_max`, `confirmed_offer_price`, `minimum_subscription_amount`,
     `ipo_status`, `number_of_shares`, `created_at`, `updated_at`, `status`)
VALUES
    (4, 102, 'ARM', 'Arm Holdings plc', 'NASDAQ', 'Semiconductors',
     DATE_SUB(CURDATE(), INTERVAL 16 DAY), DATE_SUB(CURDATE(), INTERVAL 5 DAY),
     DATE_SUB(CURDATE(), INTERVAL 4 DAY), DATE_SUB(CURDATE(), INTERVAL 3 DAY), DATE_SUB(CURDATE(), INTERVAL 3 DAY),
     51.0000, 51.0000, 51.0000, 100.0000,
     'CLOSED', 95500000, NOW(), NOW(), 'ACTIVE')
ON DUPLICATE KEY UPDATE
    `product_id`  = 102,
    `listing_date` = DATE_SUB(CURDATE(), INTERVAL 4 DAY),
    `refund_date`  = DATE_SUB(CURDATE(), INTERVAL 3 DAY),
    `ipo_status`   = 'CLOSED';

-- [케이스 3] RDDT — 배정완료, 리턴플랜 수정 가능 (listing=@prev_biz, refund=@next_biz)
INSERT INTO `ipos`
    (`id`, `ticker`, `company_name`, `exchange_name`, `sector`,
     `subscription_start_date`, `subscription_end_date`, `listing_date`, `refund_date`, `deposit_date`,
     `offer_price_min`, `offer_price_max`, `confirmed_offer_price`, `minimum_subscription_amount`,
     `ipo_status`, `number_of_shares`, `created_at`, `updated_at`, `status`)
VALUES
    (5, 'RDDT', 'Reddit, Inc.', 'NYSE', 'Consumer Internet',
     DATE_SUB(CURDATE(), INTERVAL 10 DAY), DATE_SUB(CURDATE(), INTERVAL 3 DAY),
     @prev_biz, @next_biz, @next_biz,
     31.0000, 34.0000, 34.0000, 100.0000,
     'CLOSED', 22000000, NOW(), NOW(), 'ACTIVE')
ON DUPLICATE KEY UPDATE
    `listing_date` = @prev_biz,
    `refund_date`  = @next_biz,
    `deposit_date` = @next_biz,
    `ipo_status`   = 'CLOSED';

-- [케이스 4] 오늘 상장 + IpoAllocationJob 배정 대상 (product_id 없어도 됨, 배정만)
INSERT INTO `ipos`
    (`id`, `ticker`, `company_name`, `exchange_name`, `sector`,
     `subscription_start_date`, `subscription_end_date`, `listing_date`, `refund_date`, `deposit_date`,
     `offer_price_min`, `offer_price_max`, `confirmed_offer_price`, `minimum_subscription_amount`,
     `ipo_status`, `number_of_shares`, `created_at`, `updated_at`, `status`)
VALUES
    (6, 'ALLOC', 'AllocTest Corp.', 'NYSE', 'Technology',
     DATE_SUB(CURDATE(), INTERVAL 12 DAY), @prev_biz,
     CURDATE(), @next_biz, @next_biz,
     20.0000, 22.0000, 21.0000, 100.0000,
     'CLOSED', 10000000, NOW(), NOW(), 'ACTIVE')
ON DUPLICATE KEY UPDATE
    `listing_date` = CURDATE(),
    `refund_date`  = @next_biz,
    `ipo_status`   = 'CLOSED';

-- [케이스 5·6] 오늘 상장 + IpoListingJob (product_id=101 필수)
--   sub 400: 미입고 → IpoListingJob 이 처리
--   sub 500: 이미 입고됨 → 중복 방지 확인 (holding_lot 존재)
INSERT INTO `ipos`
    (`id`, `product_id`, `ticker`, `company_name`, `exchange_name`, `sector`,
     `subscription_start_date`, `subscription_end_date`, `listing_date`, `refund_date`, `deposit_date`,
     `offer_price_min`, `offer_price_max`, `confirmed_offer_price`, `minimum_subscription_amount`,
     `ipo_status`, `number_of_shares`, `created_at`, `updated_at`, `status`)
VALUES
    (7, 101, 'LSTK', 'ListingTest Corp.', 'NASDAQ', 'Technology',
     DATE_SUB(CURDATE(), INTERVAL 14 DAY), @prev_biz,
     CURDATE(), @next_biz, @next_biz,
     25.0000, 26.0000, 25.0000, 100.0000,
     'CLOSED', 8000000, NOW(), NOW(), 'ACTIVE')
ON DUPLICATE KEY UPDATE
    `product_id`   = 101,
    `listing_date` = CURDATE(),
    `refund_date`  = @next_biz,
    `ipo_status`   = 'CLOSED';

-- [케이스 7·8] 오늘 환불일 (listing=@prev_biz, refund=CURDATE())
--   sub 600: DRAFT 리턴플랜 있음 → ReturnPlanSettlementJob 실행
--   sub 700: 리턴플랜 없음       → 자동 생성 후 실행
INSERT INTO `ipos`
    (`id`, `ticker`, `company_name`, `exchange_name`, `sector`,
     `subscription_start_date`, `subscription_end_date`, `listing_date`, `refund_date`, `deposit_date`,
     `offer_price_min`, `offer_price_max`, `confirmed_offer_price`, `minimum_subscription_amount`,
     `ipo_status`, `number_of_shares`, `created_at`, `updated_at`, `status`)
VALUES
    (8, 'RFND', 'RefundTest Corp.', 'NASDAQ', 'Fintech',
     DATE_SUB(CURDATE(), INTERVAL 13 DAY), DATE_SUB(CURDATE(), INTERVAL 2 DAY),
     @prev_biz, CURDATE(), CURDATE(),
     30.0000, 35.0000, 34.0000, 100.0000,
     'CLOSED', 5000000, NOW(), NOW(), 'ACTIVE')
ON DUPLICATE KEY UPDATE
    `listing_date` = @prev_biz,
    `refund_date`  = CURDATE(),
    `deposit_date` = CURDATE(),
    `ipo_status`   = 'CLOSED';

-- =============================================================================
-- 4. ipo_subscriptions
-- =============================================================================

-- [케이스 10] sub 100 — ARM 배정완료, 정산+입고 완료 (히스토리)
--   20주 신청 / 15주 배정(75%) / refund=261.375
INSERT INTO `ipo_subscriptions`
    (`id`, `user_id`, `ipo_id`, `securities_account_id`, `requested_shares`, `offer_price`, `subscription_amount`,
     `currency`, `subscription_status`, `execution_mode`, `subscribed_at`,
     `allocated_shares`, `allocated_amount`, `refund_amount`, `allocation_rate`, `result_status`, `confirmed_at`,
     `created_at`, `updated_at`, `status`)
VALUES
    (100, 1, 4, 1, 20, 51.0000, 1020.0000,
     'USD', 'CONFIRMED', 'MOCK', DATE_SUB(CURDATE(), INTERVAL 15 DAY),
     15, 765.0000, 261.3750, 75.0000, 'COMPLETED', DATE_SUB(CURDATE(), INTERVAL 14 DAY),
     NOW(), NOW(), 'ACTIVE')
ON DUPLICATE KEY UPDATE
    `allocated_shares` = 15, `allocated_amount` = 765.0000,
    `refund_amount` = 261.3750, `result_status` = 'COMPLETED';

-- [케이스 3] sub 200 — RDDT 배정완료, 리턴플랜 미생성 (refund=@next_biz, 수정 가능)
--   10주 신청 / 8주 배정(80%) / refund=70.04
INSERT INTO `ipo_subscriptions`
    (`id`, `user_id`, `ipo_id`, `securities_account_id`, `requested_shares`, `offer_price`, `subscription_amount`,
     `currency`, `subscription_status`, `execution_mode`, `subscribed_at`,
     `allocated_shares`, `allocated_amount`, `refund_amount`, `allocation_rate`, `result_status`, `confirmed_at`,
     `created_at`, `updated_at`, `status`)
VALUES
    (200, 1, 5, 1, 10, 34.0000, 340.0000,
     'USD', 'CONFIRMED', 'MOCK', DATE_SUB(CURDATE(), INTERVAL 9 DAY),
     8, 272.0000, 70.0400, 80.0000, 'COMPLETED', DATE_SUB(CURDATE(), INTERVAL 9 DAY),
     NOW(), NOW(), 'ACTIVE')
ON DUPLICATE KEY UPDATE
    `ipo_id` = 5, `allocated_shares` = 8, `allocated_amount` = 272.0000,
    `refund_amount` = 70.0400, `result_status` = 'COMPLETED';

-- [케이스 4] sub 300 — ALLOC(IPO 6) 배정 전 (IpoAllocationJob 대상)
--   50주 신청 / result_status=PENDING / balance_hold LOCKED
INSERT INTO `ipo_subscriptions`
    (`id`, `user_id`, `ipo_id`, `securities_account_id`, `requested_shares`, `offer_price`, `subscription_amount`,
     `currency`, `subscription_status`, `execution_mode`, `subscribed_at`,
     `result_status`,
     `created_at`, `updated_at`, `status`)
VALUES
    (300, 1, 6, 1, 50, 21.0000, 1000.0000,
     'USD', 'CONFIRMED', 'MOCK', DATE_SUB(CURDATE(), INTERVAL 11 DAY),
     'PENDING',
     NOW(), NOW(), 'ACTIVE')
ON DUPLICATE KEY UPDATE
    `ipo_id` = 6, `subscription_status` = 'CONFIRMED', `result_status` = 'PENDING';

-- [케이스 5] sub 400 — LSTK(IPO 7) 배정완료, 미입고 (IpoListingJob 대상)
--   10주 신청 / 5주 배정(50%)
INSERT INTO `ipo_subscriptions`
    (`id`, `user_id`, `ipo_id`, `securities_account_id`, `requested_shares`, `offer_price`, `subscription_amount`,
     `currency`, `subscription_status`, `execution_mode`, `subscribed_at`,
     `allocated_shares`, `allocated_amount`, `refund_amount`, `allocation_rate`, `result_status`, `confirmed_at`,
     `created_at`, `updated_at`, `status`)
VALUES
    (400, 1, 7, 1, 10, 25.0000, 250.0000,
     'USD', 'CONFIRMED', 'MOCK', DATE_SUB(CURDATE(), INTERVAL 13 DAY),
     5, 125.0000, 126.2500, 50.0000, 'COMPLETED', DATE_SUB(CURDATE(), INTERVAL 12 DAY),
     NOW(), NOW(), 'ACTIVE')
ON DUPLICATE KEY UPDATE
    `ipo_id` = 7, `allocated_shares` = 5, `result_status` = 'COMPLETED';

-- [케이스 6] sub 500 — LSTK(IPO 7) 배정완료, 이미 입고됨 (IpoListingJob 중복 방지)
--   8주 신청 / 3주 배정(37.5%)
INSERT INTO `ipo_subscriptions`
    (`id`, `user_id`, `ipo_id`, `securities_account_id`, `requested_shares`, `offer_price`, `subscription_amount`,
     `currency`, `subscription_status`, `execution_mode`, `subscribed_at`,
     `allocated_shares`, `allocated_amount`, `refund_amount`, `allocation_rate`, `result_status`, `confirmed_at`,
     `created_at`, `updated_at`, `status`)
VALUES
    (500, 1, 7, 1, 8, 25.0000, 200.0000,
     'USD', 'CONFIRMED', 'MOCK', DATE_SUB(CURDATE(), INTERVAL 13 DAY),
     3, 75.0000, 126.5000, 37.5000, 'COMPLETED', DATE_SUB(CURDATE(), INTERVAL 12 DAY),
     NOW(), NOW(), 'ACTIVE')
ON DUPLICATE KEY UPDATE
    `ipo_id` = 7, `allocated_shares` = 3, `result_status` = 'COMPLETED';

-- [케이스 7] sub 600 — RFND(IPO 8) 배정완료, DRAFT 리턴플랜 있음 (정산 대상)
--   10주 신청 / 8주 배정(80%) / refund=85.00
INSERT INTO `ipo_subscriptions`
    (`id`, `user_id`, `ipo_id`, `securities_account_id`, `requested_shares`, `offer_price`, `subscription_amount`,
     `currency`, `subscription_status`, `execution_mode`, `subscribed_at`,
     `allocated_shares`, `allocated_amount`, `refund_amount`, `allocation_rate`, `result_status`, `confirmed_at`,
     `created_at`, `updated_at`, `status`)
VALUES
    (600, 1, 8, 1, 10, 34.0000, 340.0000,
     'USD', 'CONFIRMED', 'MOCK', DATE_SUB(CURDATE(), INTERVAL 12 DAY),
     8, 272.0000, 85.0000, 80.0000, 'COMPLETED', DATE_SUB(CURDATE(), INTERVAL 11 DAY),
     NOW(), NOW(), 'ACTIVE')
ON DUPLICATE KEY UPDATE
    `ipo_id` = 8, `allocated_shares` = 8, `refund_amount` = 85.0000, `result_status` = 'COMPLETED';

-- [케이스 8] sub 700 — RFND(IPO 8) 배정완료, 리턴플랜 없음 (자동 생성 대상)
--   6주 신청 / 6주 배정(100%) / refund=60.00
INSERT INTO `ipo_subscriptions`
    (`id`, `user_id`, `ipo_id`, `securities_account_id`, `requested_shares`, `offer_price`, `subscription_amount`,
     `currency`, `subscription_status`, `execution_mode`, `subscribed_at`,
     `allocated_shares`, `allocated_amount`, `refund_amount`, `allocation_rate`, `result_status`, `confirmed_at`,
     `created_at`, `updated_at`, `status`)
VALUES
    (700, 1, 8, 1, 6, 34.0000, 240.0000,
     'USD', 'CONFIRMED', 'MOCK', DATE_SUB(CURDATE(), INTERVAL 12 DAY),
     6, 204.0000, 60.0000, 100.0000, 'COMPLETED', DATE_SUB(CURDATE(), INTERVAL 11 DAY),
     NOW(), NOW(), 'ACTIVE')
ON DUPLICATE KEY UPDATE
    `ipo_id` = 8, `allocated_shares` = 6, `refund_amount` = 60.0000, `result_status` = 'COMPLETED';

-- [케이스 9] sub 800 — CRWV(IPO 1) 취소된 청약
INSERT INTO `ipo_subscriptions`
    (`id`, `user_id`, `ipo_id`, `securities_account_id`, `requested_shares`, `offer_price`, `subscription_amount`,
     `currency`, `subscription_status`, `execution_mode`, `subscribed_at`,
     `created_at`, `updated_at`, `status`)
VALUES
    (800, 1, 1, 1, 5, 40.0000, 200.0000,
     'USD', 'CANCELLED', 'MOCK', DATE_SUB(CURDATE(), INTERVAL 1 DAY),
     NOW(), NOW(), 'ACTIVE')
ON DUPLICATE KEY UPDATE `subscription_status` = 'CANCELLED';

-- =============================================================================
-- 5. balance_holds
-- =============================================================================

-- sub 100 (ARM): 정산 완료
INSERT INTO `balance_holds`
    (`id`, `account_id`, `subscription_id`, `amount`, `hold_status`, `settled_at`, `created_at`, `updated_at`, `status`)
VALUES (100, 1, 100, 1020.0000, 'SETTLED', DATE_SUB(CURDATE(), INTERVAL 14 DAY), NOW(), NOW(), 'ACTIVE')
ON DUPLICATE KEY UPDATE `hold_status` = 'SETTLED';

-- sub 200 (RDDT): 정산 완료
INSERT INTO `balance_holds`
    (`id`, `account_id`, `subscription_id`, `amount`, `hold_status`, `settled_at`, `created_at`, `updated_at`, `status`)
VALUES (200, 1, 200, 340.0000, 'SETTLED', DATE_SUB(CURDATE(), INTERVAL 2 DAY), NOW(), NOW(), 'ACTIVE')
ON DUPLICATE KEY UPDATE `hold_status` = 'SETTLED';

-- sub 300 (ALLOC): 배정 전 LOCKED — account1 reservedBalance=1000 과 대응
INSERT INTO `balance_holds`
    (`id`, `account_id`, `subscription_id`, `amount`, `hold_status`, `created_at`, `updated_at`, `status`)
VALUES (300, 1, 300, 1000.0000, 'LOCKED', NOW(), NOW(), 'ACTIVE')
ON DUPLICATE KEY UPDATE `hold_status` = 'LOCKED';

-- sub 400 (LSTK 미입고): 정산 완료
INSERT INTO `balance_holds`
    (`id`, `account_id`, `subscription_id`, `amount`, `hold_status`, `settled_at`, `created_at`, `updated_at`, `status`)
VALUES (400, 1, 400, 250.0000, 'SETTLED', DATE_SUB(CURDATE(), INTERVAL 12 DAY), NOW(), NOW(), 'ACTIVE')
ON DUPLICATE KEY UPDATE `hold_status` = 'SETTLED';

-- sub 500 (LSTK 이미 입고): 정산 완료
INSERT INTO `balance_holds`
    (`id`, `account_id`, `subscription_id`, `amount`, `hold_status`, `settled_at`, `created_at`, `updated_at`, `status`)
VALUES (500, 1, 500, 200.0000, 'SETTLED', DATE_SUB(CURDATE(), INTERVAL 12 DAY), NOW(), NOW(), 'ACTIVE')
ON DUPLICATE KEY UPDATE `hold_status` = 'SETTLED';

-- sub 600 (RFND 리턴플랜 있음): 정산 완료
INSERT INTO `balance_holds`
    (`id`, `account_id`, `subscription_id`, `amount`, `hold_status`, `settled_at`, `created_at`, `updated_at`, `status`)
VALUES (600, 1, 600, 340.0000, 'SETTLED', DATE_SUB(CURDATE(), INTERVAL 11 DAY), NOW(), NOW(), 'ACTIVE')
ON DUPLICATE KEY UPDATE `hold_status` = 'SETTLED';

-- sub 700 (RFND 자동생성): 정산 완료
INSERT INTO `balance_holds`
    (`id`, `account_id`, `subscription_id`, `amount`, `hold_status`, `settled_at`, `created_at`, `updated_at`, `status`)
VALUES (700, 1, 700, 240.0000, 'SETTLED', DATE_SUB(CURDATE(), INTERVAL 11 DAY), NOW(), NOW(), 'ACTIVE')
ON DUPLICATE KEY UPDATE `hold_status` = 'SETTLED';

-- sub 800 (CANCELLED): 홀딩 해제
INSERT INTO `balance_holds`
    (`id`, `account_id`, `subscription_id`, `amount`, `hold_status`, `settled_at`, `created_at`, `updated_at`, `status`)
VALUES (800, 1, 800, 200.0000, 'RELEASED', DATE_SUB(CURDATE(), INTERVAL 1 DAY), NOW(), NOW(), 'ACTIVE')
ON DUPLICATE KEY UPDATE `hold_status` = 'RELEASED';

-- =============================================================================
-- 6. holdings / holding_lots
--    ARM(102) : 15주 입고 완료 (sub 100 기준, 히스토리)
--    LSTK(101): 3주 입고 완료 (sub 500 기준, 중복 방지 테스트)
--    → sub 400은 holding_lot 없음 → IpoListingJob 이 입고 처리해야 함
-- =============================================================================

INSERT INTO `holdings`
    (`id`, `user_id`, `product_id`, `total_quantity`, `average_price`, `currency`, `created_at`, `updated_at`, `status`)
VALUES
    (1, 1, 102, 15, 51.0000, 'USD', NOW(), NOW(), 'ACTIVE')
ON DUPLICATE KEY UPDATE `total_quantity` = 15, `average_price` = 51.0000;

INSERT INTO `holdings`
    (`id`, `user_id`, `product_id`, `total_quantity`, `average_price`, `currency`, `created_at`, `updated_at`, `status`)
VALUES
    (2, 1, 101, 3, 25.0000, 'USD', NOW(), NOW(), 'ACTIVE')
ON DUPLICATE KEY UPDATE `total_quantity` = 3, `average_price` = 25.0000;

-- holding_lot for ARM (sub 100, 이미 입고)
INSERT INTO `holding_lots`
    (`id`, `holding_id`, `user_id`, `product_id`,
     `source_type`, `source_id`, `quantity`, `remaining_quantity`, `acquisition_price`,
     `acquired_at`, `created_at`, `updated_at`, `status`)
VALUES
    (1, 1, 1, 102, 'IPO_ALLOCATION', 100, 15, 15, 51.0000,
     DATE_SUB(CURDATE(), INTERVAL 4 DAY), NOW(), NOW(), 'ACTIVE')
ON DUPLICATE KEY UPDATE `quantity` = 15, `remaining_quantity` = 15;

-- [케이스 6] holding_lot for LSTK sub 500 (이미 입고됨, 중복 방지)
INSERT INTO `holding_lots`
    (`id`, `holding_id`, `user_id`, `product_id`,
     `source_type`, `source_id`, `quantity`, `remaining_quantity`, `acquisition_price`,
     `acquired_at`, `created_at`, `updated_at`, `status`)
VALUES
    (2, 2, 1, 101, 'IPO_ALLOCATION', 500, 3, 3, 25.0000,
     CURDATE(), NOW(), NOW(), 'ACTIVE')
ON DUPLICATE KEY UPDATE `quantity` = 3, `remaining_quantity` = 3;

-- =============================================================================
-- 7. return_plans / return_plan_allocations
--    sub 600 : DRAFT 리턴플랜 (SECURITIES 60%, SAVINGS 40%, DEPOSIT 0%) → 정산 대상
--    sub 700 : 리턴플랜 없음 → 자동 생성 대상
--    sub 200 : 리턴플랜 없음 → 유저가 직접 생성 테스트
-- =============================================================================

-- sub 600: 재실행 멱등 보장 — 기존 allocation 먼저 삭제 후 재삽입
DELETE rpa FROM `return_plan_allocations` rpa
    INNER JOIN `return_plans` rp ON rpa.`return_plan_id` = rp.`id`
    WHERE rp.`subscription_id` = 600;
DELETE FROM `return_plans` WHERE `subscription_id` = 600;

INSERT INTO `return_plans`
    (`user_id`, `subscription_id`, `total_refund_amount`, `currency`, `plan_status`, `created_at`, `updated_at`, `status`)
VALUES
    (1, 600, 85.0000, 'USD', 'DRAFT', NOW(), NOW(), 'ACTIVE');

SET @rp600 = LAST_INSERT_ID();

INSERT INTO `return_plan_allocations`
    (`return_plan_id`, `destination_type`, `allocation_ratio`, `allocation_amount`, `allocation_status`, `created_at`, `updated_at`, `status`)
VALUES (@rp600, 'SECURITIES', 60.00, 51.0000, 'PENDING', NOW(), NOW(), 'ACTIVE');

INSERT INTO `return_plan_allocations`
    (`return_plan_id`, `destination_type`, `allocation_ratio`, `allocation_amount`, `allocation_status`, `created_at`, `updated_at`, `status`)
VALUES (@rp600, 'SAVINGS', 40.00, 34.0000, 'PENDING', NOW(), NOW(), 'ACTIVE');

INSERT INTO `return_plan_allocations`
    (`return_plan_id`, `destination_type`, `allocation_ratio`, `allocation_amount`, `allocation_status`, `created_at`, `updated_at`, `status`)
VALUES (@rp600, 'DEPOSIT', 0.00, 0.0000, 'PENDING', NOW(), NOW(), 'ACTIVE');

-- sub 700: 리턴플랜 없음 (자동 생성 대상) — 초기화
DELETE rpa FROM `return_plan_allocations` rpa
    INNER JOIN `return_plans` rp ON rpa.`return_plan_id` = rp.`id`
    WHERE rp.`subscription_id` = 700;
DELETE FROM `return_plans` WHERE `subscription_id` = 700;

-- sub 200: 유저 직접 생성 테스트용 — 초기화
DELETE rpa FROM `return_plan_allocations` rpa
    INNER JOIN `return_plans` rp ON rpa.`return_plan_id` = rp.`id`
    WHERE rp.`subscription_id` = 200;
DELETE FROM `return_plans` WHERE `subscription_id` = 200;
