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
ON DUPLICATE KEY UPDATE `balance` = 5000.0000;

-- 3. 증권계좌 - KRW (CMA)
INSERT INTO `financial_accounts`
    (`id`, `user_id`, `account_type`, `institution_type`, `institution_name`, `account_name`,
     `account_number_masked`, `currency`, `balance`, `linked`, `linked_at`, `created_at`, `updated_at`, `status`)
VALUES
    (2, 1, 'SECURITIES', 'SECURITIES', '신한투자증권', 'CMA',
     '270-91-175039[01]', 'KRW', 10000000.0000, TRUE, NOW(), NOW(), NOW(), 'ACTIVE')
ON DUPLICATE KEY UPDATE `balance` = 10000000.0000;

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
