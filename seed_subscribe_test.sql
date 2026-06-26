-- 청약(SubscribePage) 연동 테스트용 시드 데이터
-- user_id = 1 기준으로 증권계좌(KRW/USD) + 청약 가능한 IPO 1건을 채움

-- 1. 사용자 (이미 있으면 스킵)
INSERT INTO `users` (`id`, `name`, `email`, `phone_number`, `onboarding_status`, `created_at`, `updated_at`, `status`)
VALUES (1, '김희선', 'test@example.com', '010-1234-5678', 'COMPLETED', NOW(), NOW(), 'ACTIVE')
ON DUPLICATE KEY UPDATE `name` = `name`;

-- 2. 증권계좌 - USD (CMA)
INSERT INTO `financial_accounts`
    (`id`, `user_id`, `account_type`, `institution_type`, `institution_name`, `account_name`,
     `account_number_masked`, `currency`, `balance`, `linked`, `linked_at`, `created_at`, `updated_at`, `status`)
VALUES
    (1, 1, 'SECURITIES', 'SECURITIES', '신한투자증권', 'CMA',
     '270-91-175039[01]', 'USD', 5000.0000, TRUE, NOW(), NOW(), NOW(), 'ACTIVE')
ON DUPLICATE KEY UPDATE `balance` = 5000.0000, `reserved_balance` = 0.0000;

-- 3. 증권계좌 - KRW (CMA)
INSERT INTO `financial_accounts`
    (`id`, `user_id`, `account_type`, `institution_type`, `institution_name`, `account_name`,
     `account_number_masked`, `currency`, `balance`, `linked`, `linked_at`, `created_at`, `updated_at`, `status`)
VALUES
    (2, 1, 'SECURITIES', 'SECURITIES', '신한투자증권', 'CMA',
     '270-91-175039[01]', 'KRW', 10000000.0000, TRUE, NOW(), NOW(), NOW(), 'ACTIVE')
ON DUPLICATE KEY UPDATE `balance` = 10000000.0000, `reserved_balance` = 0.0000;

-- 4. 청약 가능한 IPO (오늘이 청약기간 안에 들어오도록 설정)
INSERT INTO `ipos`
    (`id`, `ticker`, `company_name`, `exchange_name`, `sector`,
     `subscription_start_date`, `subscription_end_date`, `listing_date`, `refund_date`, `deposit_date`,
     `offer_price_min`, `offer_price_max`, `confirmed_offer_price`, `minimum_subscription_amount`,
     `ipo_status`, `number_of_shares`, `created_at`, `updated_at`, `status`)
VALUES
    (1, 'CRWV', 'CoreWeave, Inc.', 'NASDAQ', 'Technology',
     DATE_SUB(CURDATE(), INTERVAL 1 DAY), DATE_ADD(CURDATE(), INTERVAL 2 DAY),
     DATE_ADD(CURDATE(), INTERVAL 5 DAY), DATE_ADD(CURDATE(), INTERVAL 6 DAY), DATE_ADD(CURDATE(), INTERVAL 6 DAY),
     38.0000, 40.0000, 40.0000, 100.0000,
     'OPEN', 49000000, NOW(), NOW(), 'ACTIVE')
ON DUPLICATE KEY UPDATE
    `subscription_start_date` = DATE_SUB(CURDATE(), INTERVAL 1 DAY),
    `subscription_end_date`   = DATE_ADD(CURDATE(), INTERVAL 2 DAY),
    `ipo_status` = 'OPEN';

-- 5. 청약 가능한 IPO 추가 종목 (다른 종목으로 청약 테스트용)
INSERT INTO `ipos`
    (`id`, `ticker`, `company_name`, `exchange_name`, `sector`,
     `subscription_start_date`, `subscription_end_date`, `listing_date`, `refund_date`, `deposit_date`,
     `offer_price_min`, `offer_price_max`, `confirmed_offer_price`, `minimum_subscription_amount`,
     `ipo_status`, `number_of_shares`, `created_at`, `updated_at`, `status`)
VALUES
    (2, 'CRCL', 'Circle Internet Group, Inc.', 'NYSE', 'Financial Technology',
     DATE_SUB(CURDATE(), INTERVAL 1 DAY), DATE_ADD(CURDATE(), INTERVAL 2 DAY),
     DATE_ADD(CURDATE(), INTERVAL 5 DAY), DATE_ADD(CURDATE(), INTERVAL 6 DAY), DATE_ADD(CURDATE(), INTERVAL 6 DAY),
     27.0000, 28.0000, 28.0000, 100.0000,
     'OPEN', 32000000, NOW(), NOW(), 'ACTIVE')
ON DUPLICATE KEY UPDATE
    `subscription_start_date` = DATE_SUB(CURDATE(), INTERVAL 1 DAY),
    `subscription_end_date`   = DATE_ADD(CURDATE(), INTERVAL 2 DAY),
    `ipo_status` = 'OPEN';

-- 6. 청약예정(UPCOMING) IPO (청약 시작일이 미래)
INSERT INTO `ipos`
    (`id`, `ticker`, `company_name`, `exchange_name`, `sector`,
     `subscription_start_date`, `subscription_end_date`, `listing_date`, `refund_date`, `deposit_date`,
     `offer_price_min`, `offer_price_max`, `confirmed_offer_price`, `minimum_subscription_amount`,
     `ipo_status`, `number_of_shares`, `created_at`, `updated_at`, `status`)
VALUES
    (3, 'CART', 'Instacart, Inc.', 'NASDAQ', 'Consumer Internet',
     DATE_ADD(CURDATE(), INTERVAL 5 DAY), DATE_ADD(CURDATE(), INTERVAL 8 DAY),
     DATE_ADD(CURDATE(), INTERVAL 11 DAY), DATE_ADD(CURDATE(), INTERVAL 12 DAY), DATE_ADD(CURDATE(), INTERVAL 12 DAY),
     26.0000, 28.0000, NULL, 100.0000,
     'UPCOMING', 15000000, NOW(), NOW(), 'ACTIVE')
ON DUPLICATE KEY UPDATE
    `subscription_start_date` = DATE_ADD(CURDATE(), INTERVAL 5 DAY),
    `subscription_end_date`   = DATE_ADD(CURDATE(), INTERVAL 8 DAY),
    `ipo_status` = 'UPCOMING';

-- 7. 청약종료(CLOSED) IPO (청약 종료일이 과거, 상장 완료)
INSERT INTO `ipos`
    (`id`, `ticker`, `company_name`, `exchange_name`, `sector`,
     `subscription_start_date`, `subscription_end_date`, `listing_date`, `refund_date`, `deposit_date`,
     `offer_price_min`, `offer_price_max`, `confirmed_offer_price`, `minimum_subscription_amount`,
     `ipo_status`, `number_of_shares`, `created_at`, `updated_at`, `status`)
VALUES
    (4, 'ARM', 'Arm Holdings plc', 'NASDAQ', 'Semiconductors',
     DATE_SUB(CURDATE(), INTERVAL 10 DAY), DATE_SUB(CURDATE(), INTERVAL 7 DAY),
     DATE_SUB(CURDATE(), INTERVAL 4 DAY), DATE_SUB(CURDATE(), INTERVAL 3 DAY), DATE_SUB(CURDATE(), INTERVAL 3 DAY),
     51.0000, 51.0000, 51.0000, 100.0000,
     'CLOSED', 95500000, NOW(), NOW(), 'ACTIVE')
ON DUPLICATE KEY UPDATE
    `subscription_start_date` = DATE_SUB(CURDATE(), INTERVAL 10 DAY),
    `subscription_end_date`   = DATE_SUB(CURDATE(), INTERVAL 7 DAY),
    `ipo_status` = 'CLOSED';

-- 8. 배정완료(상장완료) 청약 1건: ARM(id=4)에 신청 → 확정 → 배정까지 끝난 상태.
--    20주 신청(증거금 1030.20) 중 15주 배정(75%), 나머지 5주분은 환불 처리됨.
--    이미 정산까지 끝난 상태이므로 reservedBalance는 0, balance는 배정금액(765)만큼 실제로 빠진 값으로 맞춘다.
-- fee_amount = 765 × 0.5% = 3.8250
-- refund_amount = 1020 × 1.01 − 765 − 3.825 = 261.3750
INSERT INTO `ipo_subscriptions`
    (`id`, `user_id`, `ipo_id`, `securities_account_id`, `requested_shares`, `offer_price`, `subscription_amount`,
     `currency`, `subscription_status`, `execution_mode`, `subscribed_at`,
     `allocated_shares`, `allocated_amount`, `refund_amount`, `allocation_rate`, `result_status`, `confirmed_at`,
     `created_at`, `updated_at`, `status`)
VALUES
    (100, 1, 4, 1, 20, 51.0000, 1020.0000,
     'USD', 'CONFIRMED', 'MOCK', DATE_SUB(CURDATE(), INTERVAL 9 DAY),
     15, 765.0000, 261.3750, 75.0000, 'COMPLETED', DATE_SUB(CURDATE(), INTERVAL 9 DAY),
     NOW(), NOW(), 'ACTIVE')
ON DUPLICATE KEY UPDATE
    `allocated_shares` = 15, `allocated_amount` = 765.0000,
    `refund_amount` = 261.3750,
    `allocation_rate` = 75.0000, `result_status` = 'COMPLETED';

INSERT INTO `balance_holds`
    (`id`, `account_id`, `subscription_id`, `amount`, `hold_status`, `settled_at`, `created_at`, `updated_at`, `status`)
VALUES
    (100, 1, 100, 1030.2000, 'SETTLED', DATE_SUB(CURDATE(), INTERVAL 9 DAY), NOW(), NOW(), 'ACTIVE')
ON DUPLICATE KEY UPDATE `hold_status` = 'SETTLED';

-- =====================================================================
-- 리턴플랜 예약 테스트용 데이터
-- 배정은 완료됐지만 리턴플랜 미생성 상태 → 앱에서 리턴플랜 분배 예약 가능
-- listing_date = 어제(배정 완료), refund_date = 내일(수정 마감 전)
-- =====================================================================

-- 9. SAVINGS / DEPOSIT 계좌 (createReturnPlan 통과 조건)
INSERT INTO `financial_accounts`
    (`id`, `user_id`, `account_type`, `institution_type`, `institution_name`, `account_name`,
     `account_number_masked`, `currency`, `balance`, `interest_rate`, `maturity_date`,
     `linked`, `linked_at`, `created_at`, `updated_at`, `status`)
VALUES
    (3, 1, 'SAVINGS', 'BANK', '신한은행', '신한 Value-up 외화적립예금',
     '****-****-3391', 'USD', 200.0000, 4.0000, DATE_ADD(CURDATE(), INTERVAL 1 YEAR),
     TRUE, NOW(), NOW(), NOW(), 'ACTIVE')
ON DUPLICATE KEY UPDATE `linked` = TRUE, `linked_at` = NOW();

INSERT INTO `financial_accounts`
    (`id`, `user_id`, `account_type`, `institution_type`, `institution_name`, `account_name`,
     `account_number_masked`, `currency`, `balance`, `interest_rate`, `maturity_date`,
     `linked`, `linked_at`, `created_at`, `updated_at`, `status`)
VALUES
    (4, 1, 'DEPOSIT', 'BANK', '신한은행', '외화 체인지업 예금',
     '****-****-7742', 'USD', 500.0000, 3.0000, DATE_ADD(CURDATE(), INTERVAL 1 YEAR),
     TRUE, NOW(), NOW(), NOW(), 'ACTIVE')
ON DUPLICATE KEY UPDATE `linked` = TRUE, `linked_at` = NOW();

-- 10. 리턴플랜 예약 가능 IPO: 상장은 어제, 환불일은 내일
--     refund_date가 내일이므로 D+1 20:00 수정 마감 전 → 리턴플랜 생성/수정 가능
INSERT INTO `ipos`
    (`id`, `ticker`, `company_name`, `exchange_name`, `sector`,
     `subscription_start_date`, `subscription_end_date`, `listing_date`, `refund_date`, `deposit_date`,
     `offer_price_min`, `offer_price_max`, `confirmed_offer_price`, `minimum_subscription_amount`,
     `ipo_status`, `number_of_shares`, `created_at`, `updated_at`, `status`)
VALUES
    (5, 'RDDT', 'Reddit, Inc.', 'NYSE', 'Consumer Internet',
     DATE_SUB(CURDATE(), INTERVAL 8 DAY), DATE_SUB(CURDATE(), INTERVAL 5 DAY),
     DATE_SUB(CURDATE(), INTERVAL 1 DAY), DATE_ADD(CURDATE(), INTERVAL 1 DAY), DATE_ADD(CURDATE(), INTERVAL 1 DAY),
     31.0000, 34.0000, 34.0000, 100.0000,
     'CLOSED', 22000000, NOW(), NOW(), 'ACTIVE')
ON DUPLICATE KEY UPDATE
    `listing_date`  = DATE_SUB(CURDATE(), INTERVAL 1 DAY),
    `refund_date`   = DATE_ADD(CURDATE(), INTERVAL 1 DAY),
    `deposit_date`  = DATE_ADD(CURDATE(), INTERVAL 1 DAY),
    `ipo_status`    = 'CLOSED';

-- 11. 배정 완료 청약: RDDT(id=5), 10주 신청 중 8주 배정(80%)
--     subscription_amount = 340, agency_deposit = 340 × 1.01 = 343.40
--     allocated_amount = 34 × 8 = 272.00
--     fee_amount = 272 × 0.005 = 1.3600
--     refund_amount = 343.40 − 272 − 1.36 = 70.0400
INSERT INTO `ipo_subscriptions`
    (`id`, `user_id`, `ipo_id`, `securities_account_id`, `requested_shares`, `offer_price`, `subscription_amount`,
     `currency`, `subscription_status`, `execution_mode`, `subscribed_at`,
     `allocated_shares`, `allocated_amount`, `refund_amount`, `allocation_rate`, `result_status`, `confirmed_at`,
     `created_at`, `updated_at`, `status`)
VALUES
    (200, 1, 5, 1, 10, 34.0000, 340.0000,
     'USD', 'CONFIRMED', 'MOCK', DATE_SUB(CURDATE(), INTERVAL 7 DAY),
     8, 272.0000, 70.0400, 80.0000, 'COMPLETED', DATE_SUB(CURDATE(), INTERVAL 7 DAY),
     NOW(), NOW(), 'ACTIVE')
ON DUPLICATE KEY UPDATE
    `ipo_id` = 5, `subscription_status` = 'CONFIRMED',
    `allocated_shares` = 8, `allocated_amount` = 272.0000,
    `refund_amount` = 70.0400,
    `allocation_rate` = 80.0000, `result_status` = 'COMPLETED';

INSERT INTO `balance_holds`
    (`id`, `account_id`, `subscription_id`, `amount`, `hold_status`, `settled_at`, `created_at`, `updated_at`, `status`)
VALUES
    (200, 1, 200, 343.4000, 'SETTLED', DATE_SUB(CURDATE(), INTERVAL 1 DAY), NOW(), NOW(), 'ACTIVE')
ON DUPLICATE KEY UPDATE `hold_status` = 'SETTLED';

-- return_plans에 subscription_id=200 데이터가 없어야 리턴플랜 생성 가능
-- 재실행 시 기존 리턴플랜 삭제 (테스트 초기화)
DELETE rpa FROM `return_plan_allocations` rpa
    INNER JOIN `return_plans` rp ON rpa.`return_plan_id` = rp.`id`
    WHERE rp.`subscription_id` = 200;
DELETE FROM `return_plans` WHERE `subscription_id` = 200;
