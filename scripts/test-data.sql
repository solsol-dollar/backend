-- =====================================================================
-- Eclipse 로컬 테스트용 픽스처 데이터
-- 적용: mysql -u <user> -p <db_name> < scripts/test-data.sql
-- 멱등성: 중복 실행해도 기존 데이터를 덮어쓰지 않음 (INSERT IGNORE)
-- =====================================================================

SET @now = NOW();

-- ---------------------------------------------------------------------
-- 1. 목 유저 4명 (간편 비밀번호 BCrypt 해시)
--    김하늘: 123456 / 이서준: 654321 / 박지민: 111111 / 장원영: 222222
-- ---------------------------------------------------------------------
INSERT IGNORE INTO users (id, name, email, phone_number, onboarding_status, investment_status, simple_password, created_at, updated_at, status)
VALUES
    (1, '김하늘', 'kim@eclipse.dev',  '010-1111-1111', 'COMPLETED', 'COMPLETED', '$2a$10$pSFAP6RtRspRqB4zmpNZWOCBkT1HYzlcSsamx5f5EfqiJfgR1ZvaK', @now, @now, 'ACTIVE'),
    (2, '이서준', 'lee@eclipse.dev',  '010-2222-2222', 'COMPLETED', 'COMPLETED', '$2a$10$YckxBJxddvv6XcyZ0U2NH.YyD6sOKMLF1SEOBZtRfBmMYb5kzr9qq', @now, @now, 'ACTIVE'),
    (3, '박지민', 'park@eclipse.dev', '010-3333-3333', 'COMPLETED', 'COMPLETED', '$2a$10$qmwLYEVLCrbRFLBsKgPNuO.RDFvHT.Ikjvhl5fEDZcPuHKSbdIwhu', @now, @now, 'ACTIVE'),
    (4, '장원영', 'jwy@eclipse.dev',  '010-4444-4444', 'REQUIRED',  'REQUIRED',  '$2a$10$5f.jG/EF0XnaPcKt5SRfeudpjDHPoE1hK9O831BBwJwVkUXq05Iie', @now, @now, 'ACTIVE');


-- ---------------------------------------------------------------------
-- 2. 외화증권 계좌 (id=1, USD $50,000)
-- ---------------------------------------------------------------------
INSERT IGNORE INTO financial_accounts (
    id, user_id, account_type, institution_type, institution_name,
    account_name, account_number, virtual_account_number, currency, balance,
    interest_rate, maturity_date, linked, linked_at, created_at, updated_at, status
) VALUES (
    1, 1, 'SECURITIES', 'SECURITIES_FIRM', '신한투자증권',
    '신한 외화증권 계좌', '****-****-1234', '110-456-789012', 'USD', 50000.0000,
    NULL, NULL, TRUE, @now, @now, @now, 'ACTIVE'
);
INSERT IGNORE INTO financial_accounts (
    id, user_id, account_type, institution_type, institution_name,
    account_name, account_number, virtual_account_number, currency, balance,
    interest_rate, maturity_date, linked, linked_at, created_at, updated_at, status
) VALUES (
    2, 1, 'DEPOSIT', 'BANK', '신한은행',
    '외화 체인지업 예금', '****-****-5678', NULL, 'USD', 10000.0000,
    NULL, NULL, TRUE, @now, @now, @now, 'ACTIVE'
);
INSERT IGNORE INTO financial_accounts (
    id, user_id, account_type, institution_type, institution_name,
    account_name, account_number, virtual_account_number, currency, balance,
    interest_rate, maturity_date, linked, linked_at, created_at, updated_at, status
) VALUES (
    3, 1, 'SAVINGS', 'BANK', '신한은행',
    '신한 Value-up 외화적립예금', '****-****-9012', NULL, 'USD', 0.0000,
    3.5000, '2027-06-22', TRUE, @now, @now, @now, 'ACTIVE'
);

-- ---------------------------------------------------------------------
-- 3. 원화증권 계좌 (id=4, KRW 5,000,000원 — 환전 출금용)
-- ---------------------------------------------------------------------
INSERT IGNORE INTO financial_accounts (
    id, user_id, account_type, institution_type, institution_name,
    account_name, account_number, virtual_account_number, currency, balance,
    interest_rate, maturity_date, linked, linked_at, created_at, updated_at, status
) VALUES (
    4, 1, 'SECURITIES', 'SECURITIES_FIRM', '신한투자증권',
    '신한 원화증권 계좌', '****-****-5678', NULL, 'KRW', 5000000.0000,
    NULL, NULL, TRUE, @now, @now, @now, 'ACTIVE'
);

-- ---------------------------------------------------------------------
-- 4. 장원영 증권 계좌 (외화 $2,000 / 원화 ₩0)
-- ---------------------------------------------------------------------
INSERT IGNORE INTO financial_accounts (
    id, user_id, account_type, institution_type, institution_name,
    account_name, account_number, virtual_account_number, currency, balance,
    interest_rate, maturity_date, linked, linked_at, created_at, updated_at, status
) VALUES (
    5, 4, 'SECURITIES', 'SECURITIES_FIRM', '신한투자증권',
    '신한 외화증권 계좌', '****-****-2024', '110-456-202400', 'USD', 2000.0000,
    NULL, NULL, TRUE, @now, @now, @now, 'ACTIVE'
);
INSERT IGNORE INTO financial_accounts (
    id, user_id, account_type, institution_type, institution_name,
    account_name, account_number, virtual_account_number, currency, balance,
    interest_rate, maturity_date, linked, linked_at, created_at, updated_at, status
) VALUES (
    6, 4, 'SECURITIES', 'SECURITIES_FIRM', '신한투자증권',
    '신한 원화증권 계좌', '****-****-2025', NULL, 'KRW', 0.0000,
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
-- 카드 (김하늘 user_id=1, 계좌 id=1 연결)
-- ---------------------------------------------------------------------
INSERT IGNORE INTO cards (id, user_id, linked_account_id, card_type, card_status,
    card_name, card_number_masked, issuer_name, linked, linked_at, created_at, updated_at, status)
VALUES (1, 1, 1, 'CHECK_CARD', 'LINKED',
    '신한 CMA 체크카드', '****-****-****-7842', '신한카드', true, @now, @now, @now, 'ACTIVE');

-- ---------------------------------------------------------------------
-- 카드 결제 내역 (user_id=1, card_id=1)
-- baseRateAtTime ≈ 1380, ttsAtTime ≈ 1395 (spread 15 KRW)
-- 구독: 4월~6월 3개월 반복 (정기 결제 감지 대상)
-- 일반: 5~6월 다양한 해외 결제
-- ---------------------------------------------------------------------

-- ── 구독 4월 (실제 수출입은행 환율 적용) ────────────────────────────────
INSERT IGNORE INTO card_transactions (id, user_id, card_id, merchant_name, category, amount, currency, transacted_at, base_rate_at_time, tts_at_time, created_at, updated_at, status) VALUES
(101, 1, 1, 'Anthropic',       '구독',  20.0000, 'USD', '2026-04-01 09:00:00', 1530.5000, 1545.8000, @now, @now, 'ACTIVE'),
(102, 1, 1, 'Adobe',           '구독',  54.9900, 'USD', '2026-04-03 09:00:00', 1518.8000, 1533.9800, @now, @now, 'ACTIVE'),
(103, 1, 1, 'Microsoft 365',   '구독',   9.9900, 'USD', '2026-04-08 09:00:00', 1506.8000, 1521.8600, @now, @now, 'ACTIVE'),
(104, 1, 1, 'Spotify',         '콘텐츠',  10.9900, 'USD', '2026-04-10 09:00:00', 1481.1000, 1495.9100, @now, @now, 'ACTIVE'),
(105, 1, 1, 'Notion',          '구독',   8.0000, 'USD', '2026-04-12 09:00:00', 1481.1000, 1495.9100, @now, @now, 'ACTIVE'),  -- 토요일 → 04/10 환율 사용
(106, 1, 1, 'OpenAI',          '구독',  20.0000, 'USD', '2026-04-15 09:00:00', 1481.4000, 1496.2100, @now, @now, 'ACTIVE'),
(107, 1, 1, 'GitHub',          '구독',  10.0000, 'USD', '2026-04-20 09:00:00', 1480.0000, 1494.8000, @now, @now, 'ACTIVE'),
(108, 1, 1, 'Netflix',         '콘텐츠',  15.4900, 'USD', '2026-04-22 09:00:00', 1470.8000, 1485.5000, @now, @now, 'ACTIVE');

-- ── 구독 5월 + 일반 결제 (실제 수출입은행 환율 적용) ─────────────────────
INSERT IGNORE INTO card_transactions (id, user_id, card_id, merchant_name, category, amount, currency, transacted_at, base_rate_at_time, tts_at_time, created_at, updated_at, status) VALUES
(201, 1, 1, 'Anthropic',       '구독',  20.0000, 'USD', '2026-05-01 09:00:00', 1476.1000, 1490.8600, @now, @now, 'ACTIVE'),  -- 공휴일 → 04/30 환율
(202, 1, 1, 'Adobe',           '구독',  54.9900, 'USD', '2026-05-03 09:00:00', 1476.1000, 1490.8600, @now, @now, 'ACTIVE'),  -- 토요일 → 04/30 환율
(203, 1, 1, 'Microsoft 365',   '구독',   9.9900, 'USD', '2026-05-08 09:00:00', 1450.8000, 1465.3000, @now, @now, 'ACTIVE'),
(204, 1, 1, 'Spotify',         '콘텐츠',  10.9900, 'USD', '2026-05-10 09:00:00', 1450.8000, 1465.3000, @now, @now, 'ACTIVE'),  -- 토요일 → 05/08 환율
(205, 1, 1, 'Notion',          '구독',   8.0000, 'USD', '2026-05-12 09:00:00', 1473.0000, 1487.7300, @now, @now, 'ACTIVE'),
(206, 1, 1, 'OpenAI',          '구독',  20.0000, 'USD', '2026-05-15 09:00:00', 1491.8000, 1506.7100, @now, @now, 'ACTIVE'),
(207, 1, 1, 'GitHub',          '구독',  10.0000, 'USD', '2026-05-20 09:00:00', 1503.8000, 1518.8300, @now, @now, 'ACTIVE'),
(208, 1, 1, 'Netflix',         '콘텐츠',  15.4900, 'USD', '2026-05-22 09:00:00', 1503.5000, 1518.5300, @now, @now, 'ACTIVE'),
(209, 1, 1, 'Amazon',          '쇼핑',      89.9900, 'USD', '2026-05-05 14:30:00', 1456.8000, 1471.3600, @now, @now, 'ACTIVE'),  -- 공휴일 → 05/07 환율
(210, 1, 1, 'Starbucks',       '여행',           6.5000, 'USD', '2026-05-07 08:20:00', 1456.8000, 1471.3600, @now, @now, 'ACTIVE'),
(211, 1, 1, 'Uber',            '여행',     23.4000, 'USD', '2026-05-09 19:10:00', 1450.8000, 1465.3000, @now, @now, 'ACTIVE'),  -- N/A → 05/08 환율
(212, 1, 1, 'Chipotle',        '여행',    14.5000, 'USD', '2026-05-14 12:40:00', 1495.1000, 1510.0500, @now, @now, 'ACTIVE'),
(213, 1, 1, 'Walmart',         '여행',      145.6000, 'USD', '2026-05-18 16:00:00', 1499.8000, 1514.7900, @now, @now, 'ACTIVE'),
(214, 1, 1, 'McDonald',        '여행',    12.3000, 'USD', '2026-05-21 13:00:00', 1509.7000, 1524.7900, @now, @now, 'ACTIVE'),
(215, 1, 1, 'Starbucks',       '여행',           5.8000, 'USD', '2026-05-25 09:00:00', 1503.5000, 1518.5300, @now, @now, 'ACTIVE'),  -- 공휴일 → 05/22 환율
(216, 1, 1, 'Uber Eats',       '여행',      32.9000, 'USD', '2026-05-27 19:30:00', 1507.9000, 1522.9700, @now, @now, 'ACTIVE');

-- ── 구독 6월 + 일반 결제 (실제 수출입은행 환율 적용) ─────────────────────
INSERT IGNORE INTO card_transactions (id, user_id, card_id, merchant_name, category, amount, currency, transacted_at, base_rate_at_time, tts_at_time, created_at, updated_at, status) VALUES
(301, 1, 1, 'Anthropic',       '구독',  20.0000, 'USD', '2026-06-01 09:00:00', 1503.2000, 1518.2300, @now, @now, 'ACTIVE'),
(302, 1, 1, 'Adobe',           '구독',  54.9900, 'USD', '2026-06-03 09:00:00', 1511.3000, 1526.4100, @now, @now, 'ACTIVE'),  -- 토요일 → 06/02 환율
(303, 1, 1, 'Microsoft 365',   '구독',   9.9900, 'USD', '2026-06-08 09:00:00', 1543.0000, 1558.4300, @now, @now, 'ACTIVE'),
(304, 1, 1, 'Spotify',         '콘텐츠',  10.9900, 'USD', '2026-06-10 09:00:00', 1518.4000, 1533.5800, @now, @now, 'ACTIVE'),
(305, 1, 1, 'Notion',          '구독',   8.0000, 'USD', '2026-06-12 09:00:00', 1527.0000, 1542.2700, @now, @now, 'ACTIVE'),
(306, 1, 1, 'OpenAI',          '구독',  20.0000, 'USD', '2026-06-15 09:00:00', 1520.4000, 1535.6000, @now, @now, 'ACTIVE'),
(307, 1, 1, 'GitHub',          '구독',  10.0000, 'USD', '2026-06-20 09:00:00', 1523.4000, 1538.6300, @now, @now, 'ACTIVE'),  -- N/A → 06/19 환율
(308, 1, 1, 'Netflix',         '콘텐츠',  15.4900, 'USD', '2026-06-22 09:00:00', 1535.0000, 1550.3500, @now, @now, 'ACTIVE'),
(309, 1, 1, 'Amazon',          '쇼핑',     134.9900, 'USD', '2026-06-02 11:20:00', 1511.3000, 1526.4100, @now, @now, 'ACTIVE'),
(310, 1, 1, 'Starbucks',       '여행',           7.2000, 'USD', '2026-06-04 08:10:00', 1515.6000, 1530.7500, @now, @now, 'ACTIVE'),
(311, 1, 1, 'Uber',            '여행',     18.9000, 'USD', '2026-06-05 20:00:00', 1528.6000, 1543.8800, @now, @now, 'ACTIVE'),
(312, 1, 1, 'Target',          '여행',       67.8000, 'USD', '2026-06-07 15:30:00', 1528.6000, 1543.8800, @now, @now, 'ACTIVE'),  -- 일요일 → 06/05 환율
(313, 1, 1, 'Chipotle',        '여행',    15.2000, 'USD', '2026-06-09 12:30:00', 1546.5000, 1561.9600, @now, @now, 'ACTIVE'),
(314, 1, 1, 'McDonald',        '여행',     8.9000, 'USD', '2026-06-11 13:10:00', 1522.4000, 1537.6200, @now, @now, 'ACTIVE'),
(315, 1, 1, 'Shake Shack',     '여행',    18.5000, 'USD', '2026-06-13 12:00:00', 1527.0000, 1542.2700, @now, @now, 'ACTIVE'),  -- 토요일 → 06/12 환율
(316, 1, 1, 'AliExpress',      '쇼핑',      42.3000, 'USD', '2026-06-16 10:00:00', 1510.0000, 1525.1000, @now, @now, 'ACTIVE'),
(317, 1, 1, 'Uber Eats',       '여행',      28.7000, 'USD', '2026-06-18 19:00:00', 1512.8000, 1527.9200, @now, @now, 'ACTIVE'),
(318, 1, 1, 'Walmart',         '여행',      112.4000, 'USD', '2026-06-19 16:30:00', 1523.4000, 1538.6300, @now, @now, 'ACTIVE'),
(319, 1, 1, 'PlayStation',     '콘텐츠',          59.9900, 'USD', '2026-06-21 22:00:00', 1523.4000, 1538.6300, @now, @now, 'ACTIVE'),  -- 일요일 → 06/19 환율
(320, 1, 1, 'Starbucks',       '여행',           6.8000, 'USD', '2026-06-23 08:30:00', 1535.7000, 1551.0500, @now, @now, 'ACTIVE'),
(321, 1, 1, 'DoorDash',        '여행',      35.6000, 'USD', '2026-06-24 18:30:00', 1537.2000, 1552.5700, @now, @now, 'ACTIVE');

-- ---------------------------------------------------------------------
-- 확인
-- ---------------------------------------------------------------------
SELECT 'users'               AS tbl, COUNT(*) AS cnt FROM users WHERE id IN (1, 4)
UNION ALL
SELECT 'financial_accounts(김하늘)',  COUNT(*) FROM financial_accounts WHERE user_id = 1
UNION ALL
SELECT 'financial_accounts(장원영)',  COUNT(*) FROM financial_accounts WHERE user_id = 4
UNION ALL
SELECT 'investment_products', COUNT(*) FROM investment_products WHERE status = 'ACTIVE'
UNION ALL
SELECT 'holdings',            COUNT(*) FROM holdings WHERE user_id = 1
UNION ALL
SELECT 'holding_lots',        COUNT(*) FROM holding_lots WHERE user_id = 1
UNION ALL
SELECT 'cards',               COUNT(*) FROM cards WHERE user_id = 1
UNION ALL
SELECT 'card_transactions',   COUNT(*) FROM card_transactions WHERE user_id = 1;
