-- =====================================================================
-- Eclipse 로컬 테스트용 픽스처 데이터
-- 적용: mysql -u <user> -p <db_name> < scripts/test-data.sql
-- 멱등성: 중복 실행해도 기존 데이터를 덮어쓰지 않음 (INSERT IGNORE)
-- =====================================================================

SET @now = NOW();

-- ---------------------------------------------------------------------
-- 1. 목 유저 3명 (간편 비밀번호 BCrypt 해시)
--    김하늘: 123456 / 이서준: 654321 / 박지민: 111111
-- ---------------------------------------------------------------------
INSERT IGNORE INTO users (id, name, email, phone_number, onboarding_status, simple_password, created_at, updated_at, status)
VALUES
    (1, '김하늘', 'kim@eclipse.dev',  '010-1111-1111', 'COMPLETED', '$2a$10$pSFAP6RtRspRqB4zmpNZWOCBkT1HYzlcSsamx5f5EfqiJfgR1ZvaK', @now, @now, 'ACTIVE'),
    (2, '이서준', 'lee@eclipse.dev',  '010-2222-2222', 'COMPLETED', '$2a$10$YckxBJxddvv6XcyZ0U2NH.YyD6sOKMLF1SEOBZtRfBmMYb5kzr9qq', @now, @now, 'ACTIVE'),
    (3, '박지민', 'park@eclipse.dev', '010-3333-3333', 'COMPLETED', '$2a$10$qmwLYEVLCrbRFLBsKgPNuO.RDFvHT.Ikjvhl5fEDZcPuHKSbdIwhu', @now, @now, 'ACTIVE');


-- ---------------------------------------------------------------------
-- 2. 외화증권 계좌 (id=1, USD $50,000)
-- ---------------------------------------------------------------------
INSERT IGNORE INTO financial_accounts (
    id, user_id, account_type, institution_type, institution_name,
    account_name, account_number_masked, currency, balance,
    interest_rate, maturity_date, linked, linked_at, created_at, updated_at, status
) VALUES (
    1, 1, 'SECURITIES', 'SECURITIES', '신한투자증권',
    '신한 외화증권 계좌', '****-****-1234', 'USD', 50000.0000,
    NULL, NULL, TRUE, @now, @now, @now, 'ACTIVE'
);
INSERT IGNORE INTO financial_accounts (
    id, user_id, account_type, institution_type, institution_name,
    account_name, account_number_masked, currency, balance,
    interest_rate, maturity_date, linked, linked_at, created_at, updated_at, status
) VALUES (
    2, 1, 'DEPOSIT', 'BANK', '신한은행',
    '신한 외화예금 계좌', '****-****-5678', 'USD', 10000.0000,
    NULL, NULL, TRUE, @now, @now, @now, 'ACTIVE'
);
INSERT IGNORE INTO financial_accounts (
    id, user_id, account_type, institution_type, institution_name,
    account_name, account_number_masked, currency, balance,
    interest_rate, maturity_date, linked, linked_at, created_at, updated_at, status
) VALUES (
    3, 1, 'SAVINGS', 'BANK', '신한은행',
    '신한 외화적금 계좌', '****-****-9012', 'USD', 0.0000,
    3.5000, '2027-06-22', TRUE, @now, @now, @now, 'ACTIVE'
);

-- ---------------------------------------------------------------------
-- 3. 원화증권 계좌 (id=2, KRW 5,000,000원 — 환전 출금용)
-- ---------------------------------------------------------------------
INSERT IGNORE INTO financial_accounts (
    id, user_id, account_type, institution_type, institution_name,
    account_name, account_number_masked, currency, balance,
    interest_rate, maturity_date, linked, linked_at, created_at, updated_at, status
) VALUES (
    2, 1, 'SECURITIES', 'SECURITIES', '신한투자증권',
    '신한 원화증권 계좌', '****-****-5678', 'KRW', 5000000.0000,
    NULL, NULL, TRUE, @now, @now, @now, 'ACTIVE'
);

-- ---------------------------------------------------------------------
-- 4. 종목 시드 (앱 기동 전 수동 실행하는 경우에만 필요)
--    ProductDataInitializer 가 이미 삽입했으면 INSERT IGNORE 로 스킵됨
--    주의: ticker 에 UNIQUE 제약이 없으므로 앱 시드와 중복 삽입되지 않도록
--          existsByTicker 체크를 앱이 하고 있음 — 여기서는 앱 시드 순서대로 id를 매기지 않음
--          holdings 에서 ticker 기준 서브쿼리로 product_id 를 찾으므로 id 순서 무관
-- ---------------------------------------------------------------------
INSERT IGNORE INTO investment_products (product_type, ticker, product_name, exchange_name, currency, sector, created_at, updated_at, status)
SELECT 'OVERSEAS', 'AAPL', 'Apple Inc',          'NASDAQ', 'USD', 'Technology',             @now, @now, 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM investment_products WHERE ticker = 'AAPL');

INSERT IGNORE INTO investment_products (product_type, ticker, product_name, exchange_name, currency, sector, created_at, updated_at, status)
SELECT 'OVERSEAS', 'NVDA', 'NVIDIA Corp',         'NASDAQ', 'USD', 'Semiconductors',         @now, @now, 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM investment_products WHERE ticker = 'NVDA');

INSERT IGNORE INTO investment_products (product_type, ticker, product_name, exchange_name, currency, sector, created_at, updated_at, status)
SELECT 'OVERSEAS', 'TSLA', 'Tesla Inc',           'NASDAQ', 'USD', 'Consumer Discretionary', @now, @now, 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM investment_products WHERE ticker = 'TSLA');

INSERT IGNORE INTO investment_products (product_type, ticker, product_name, exchange_name, currency, sector, created_at, updated_at, status)
SELECT 'OVERSEAS', 'MSFT', 'Microsoft Corp',      'NASDAQ', 'USD', 'Technology',             @now, @now, 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM investment_products WHERE ticker = 'MSFT');

INSERT IGNORE INTO investment_products (product_type, ticker, product_name, exchange_name, currency, sector, created_at, updated_at, status)
SELECT 'ETF', 'QQQ', 'Invesco QQQ Trust',         'NASDAQ', 'USD', 'NASDAQ 100 Index',       @now, @now, 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM investment_products WHERE ticker = 'QQQ');

-- ---------------------------------------------------------------------
-- 4. 보유 종목 (holdings) — AAPL 10주, NVDA 5주, TSLA 3주
--    UNIQUE KEY (user_id, product_id) 로 중복 방지
-- ---------------------------------------------------------------------
INSERT IGNORE INTO holdings (user_id, product_id, total_quantity, average_price, currency, created_at, updated_at, status)
SELECT 1, id, 10, 195.5000, 'USD', @now, @now, 'ACTIVE'
FROM investment_products WHERE ticker = 'AAPL' LIMIT 1;

INSERT IGNORE INTO holdings (user_id, product_id, total_quantity, average_price, currency, created_at, updated_at, status)
SELECT 1, id, 5, 870.2000, 'USD', @now, @now, 'ACTIVE'
FROM investment_products WHERE ticker = 'NVDA' LIMIT 1;

INSERT IGNORE INTO holdings (user_id, product_id, total_quantity, average_price, currency, created_at, updated_at, status)
SELECT 1, id, 3, 245.8000, 'USD', @now, @now, 'ACTIVE'
FROM investment_products WHERE ticker = 'TSLA' LIMIT 1;

-- ---------------------------------------------------------------------
-- 5. 보유 로트 (holding_lots) — 중복 방지를 위해 NOT EXISTS 체크
-- ---------------------------------------------------------------------
INSERT INTO holding_lots (holding_id, user_id, product_id, source_type, source_id,
    quantity, remaining_quantity, acquisition_price, acquired_at, created_at, updated_at, status)
SELECT h.id, 1, p.id, 'TRADE_ORDER', 1001,
    10, 10, 195.5000, @now, @now, @now, 'ACTIVE'
FROM holdings h
JOIN investment_products p ON h.product_id = p.id
WHERE p.ticker = 'AAPL' AND h.user_id = 1
  AND NOT EXISTS (SELECT 1 FROM holding_lots hl
                  WHERE hl.holding_id = h.id AND hl.source_id = 1001);

INSERT INTO holding_lots (holding_id, user_id, product_id, source_type, source_id,
    quantity, remaining_quantity, acquisition_price, acquired_at, created_at, updated_at, status)
SELECT h.id, 1, p.id, 'TRADE_ORDER', 1002,
    5, 5, 870.2000, @now, @now, @now, 'ACTIVE'
FROM holdings h
JOIN investment_products p ON h.product_id = p.id
WHERE p.ticker = 'NVDA' AND h.user_id = 1
  AND NOT EXISTS (SELECT 1 FROM holding_lots hl
                  WHERE hl.holding_id = h.id AND hl.source_id = 1002);

INSERT INTO holding_lots (holding_id, user_id, product_id, source_type, source_id,
    quantity, remaining_quantity, acquisition_price, acquired_at, created_at, updated_at, status)
SELECT h.id, 1, p.id, 'TRADE_ORDER', 1003,
    3, 3, 245.8000, @now, @now, @now, 'ACTIVE'
FROM holdings h
JOIN investment_products p ON h.product_id = p.id
WHERE p.ticker = 'TSLA' AND h.user_id = 1
  AND NOT EXISTS (SELECT 1 FROM holding_lots hl
                  WHERE hl.holding_id = h.id AND hl.source_id = 1003);

-- ---------------------------------------------------------------------
-- 확인
-- ---------------------------------------------------------------------
SELECT 'users'               AS tbl, COUNT(*) AS cnt FROM users WHERE id = 1
UNION ALL
SELECT 'financial_accounts',  COUNT(*) FROM financial_accounts WHERE user_id = 1
UNION ALL
SELECT 'investment_products', COUNT(*) FROM investment_products WHERE status = 'ACTIVE'
UNION ALL
SELECT 'holdings',            COUNT(*) FROM holdings WHERE user_id = 1
UNION ALL
SELECT 'holding_lots',        COUNT(*) FROM holding_lots WHERE user_id = 1;
