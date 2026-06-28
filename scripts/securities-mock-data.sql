-- =====================================================================
-- 증권탭 로컬 검증용 Mock 데이터
-- 전제: scripts/test-data.sql 이 먼저 적용돼 있어야 함
-- 기준 유저: user_id=1 (김하늘), 기준 계좌: account_id=1 (USD 외화증권)
-- 멱등성: INSERT IGNORE / NOT EXISTS 조합으로 중복 실행 안전
-- =====================================================================

SET @now = NOW();


-- =====================================================================
-- 1. investment_products 확장 (해외주식 12종목 + ETF 3종목)
--    ticker 에 UNIQUE 제약 없음 → WHERE NOT EXISTS 로 중복 방지
-- =====================================================================

INSERT INTO investment_products (product_type, ticker, product_name, exchange_name, currency, sector, created_at, updated_at, status)
SELECT 'OVERSEAS', 'GOOGL', '알파벳', 'NASDAQ', 'USD', 'Technology', @now, @now, 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM investment_products WHERE ticker = 'GOOGL');

INSERT INTO investment_products (product_type, ticker, product_name, exchange_name, currency, sector, created_at, updated_at, status)
SELECT 'OVERSEAS', 'META', '메타플랫폼스', 'NASDAQ', 'USD', 'Technology', @now, @now, 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM investment_products WHERE ticker = 'META');

INSERT INTO investment_products (product_type, ticker, product_name, exchange_name, currency, sector, created_at, updated_at, status)
SELECT 'OVERSEAS', 'AMZN', '아마존', 'NASDAQ', 'USD', 'Consumer Discretionary', @now, @now, 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM investment_products WHERE ticker = 'AMZN');

INSERT INTO investment_products (product_type, ticker, product_name, exchange_name, currency, sector, created_at, updated_at, status)
SELECT 'OVERSEAS', 'NFLX', '넷플릭스', 'NASDAQ', 'USD', 'Communication Services', @now, @now, 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM investment_products WHERE ticker = 'NFLX');

INSERT INTO investment_products (product_type, ticker, product_name, exchange_name, currency, sector, created_at, updated_at, status)
SELECT 'OVERSEAS', 'AMD', 'AMD', 'NASDAQ', 'USD', 'Technology', @now, @now, 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM investment_products WHERE ticker = 'AMD');

INSERT INTO investment_products (product_type, ticker, product_name, exchange_name, currency, sector, created_at, updated_at, status)
SELECT 'OVERSEAS', 'INTC', '인텔', 'NASDAQ', 'USD', 'Technology', @now, @now, 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM investment_products WHERE ticker = 'INTC');

INSERT INTO investment_products (product_type, ticker, product_name, exchange_name, currency, sector, created_at, updated_at, status)
SELECT 'OVERSEAS', 'JPM', 'JP모건체이스', 'NYSE', 'USD', 'Financials', @now, @now, 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM investment_products WHERE ticker = 'JPM');

INSERT INTO investment_products (product_type, ticker, product_name, exchange_name, currency, sector, created_at, updated_at, status)
SELECT 'OVERSEAS', 'V', '비자', 'NYSE', 'USD', 'Financials', @now, @now, 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM investment_products WHERE ticker = 'V');

INSERT INTO investment_products (product_type, ticker, product_name, exchange_name, currency, sector, created_at, updated_at, status)
SELECT 'OVERSEAS', 'JNJ', '존슨앤존슨', 'NYSE', 'USD', 'Healthcare', @now, @now, 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM investment_products WHERE ticker = 'JNJ');

INSERT INTO investment_products (product_type, ticker, product_name, exchange_name, currency, sector, created_at, updated_at, status)
SELECT 'OVERSEAS', 'PLTR', '팔란티어', 'NYSE', 'USD', 'Technology', @now, @now, 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM investment_products WHERE ticker = 'PLTR');

INSERT INTO investment_products (product_type, ticker, product_name, exchange_name, currency, sector, created_at, updated_at, status)
SELECT 'OVERSEAS', 'IONQ', '아이온큐', 'NYSE', 'USD', 'Technology', @now, @now, 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM investment_products WHERE ticker = 'IONQ');

INSERT INTO investment_products (product_type, ticker, product_name, exchange_name, currency, sector, created_at, updated_at, status)
SELECT 'OVERSEAS', 'RIVN', '리비안', 'NASDAQ', 'USD', 'Consumer Discretionary', @now, @now, 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM investment_products WHERE ticker = 'RIVN');

INSERT INTO investment_products (product_type, ticker, product_name, exchange_name, currency, sector, created_at, updated_at, status)
SELECT 'ETF', 'SPY', 'SPDR S&P500 ETF', 'NYSE', 'USD', NULL, @now, @now, 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM investment_products WHERE ticker = 'SPY');

INSERT INTO investment_products (product_type, ticker, product_name, exchange_name, currency, sector, created_at, updated_at, status)
SELECT 'ETF', 'SCHD', 'Schwab Dividend ETF', 'NYSE', 'USD', NULL, @now, @now, 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM investment_products WHERE ticker = 'SCHD');

INSERT INTO investment_products (product_type, ticker, product_name, exchange_name, currency, sector, created_at, updated_at, status)
SELECT 'ETF', 'VTI', 'Vanguard Total Market', 'NYSE', 'USD', NULL, @now, @now, 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM investment_products WHERE ticker = 'VTI');


-- =====================================================================
-- 2. holdings & holding_lots 추가 (MSFT 3주 @415.00, QQQ 2주 @450.00)
--    UNIQUE KEY (user_id, product_id) 로 holdings 중복 방지
-- =====================================================================

INSERT IGNORE INTO holdings (user_id, product_id, total_quantity, average_price, currency, created_at, updated_at, status)
SELECT 1, p.id, 3, 415.0000, 'USD', @now, @now, 'ACTIVE'
FROM investment_products p WHERE p.ticker = 'MSFT' LIMIT 1;

INSERT INTO holding_lots (holding_id, user_id, product_id, source_type, source_id,
    quantity, remaining_quantity, acquisition_price, acquired_at, created_at, updated_at, status)
SELECT h.id, 1, p.id, 'TRADE_ORDER', 2001,
    3, 3, 415.0000, DATE_SUB(@now, INTERVAL 19 DAY), @now, @now, 'ACTIVE'
FROM holdings h JOIN investment_products p ON h.product_id = p.id
WHERE p.ticker = 'MSFT' AND h.user_id = 1
  AND NOT EXISTS (SELECT 1 FROM holding_lots hl WHERE hl.holding_id = h.id AND hl.source_id = 2001);

INSERT IGNORE INTO holdings (user_id, product_id, total_quantity, average_price, currency, created_at, updated_at, status)
SELECT 1, p.id, 2, 450.0000, 'USD', @now, @now, 'ACTIVE'
FROM investment_products p WHERE p.ticker = 'QQQ' LIMIT 1;

INSERT INTO holding_lots (holding_id, user_id, product_id, source_type, source_id,
    quantity, remaining_quantity, acquisition_price, acquired_at, created_at, updated_at, status)
SELECT h.id, 1, p.id, 'TRADE_ORDER', 2002,
    2, 2, 450.0000, DATE_SUB(@now, INTERVAL 54 DAY), @now, @now, 'ACTIVE'
FROM holdings h JOIN investment_products p ON h.product_id = p.id
WHERE p.ticker = 'QQQ' AND h.user_id = 1
  AND NOT EXISTS (SELECT 1 FROM holding_lots hl WHERE hl.holding_id = h.id AND hl.source_id = 2002);


-- =====================================================================
-- 3. trade_orders (주문내역 + 판매수익 화면 검증)
--    BUY 7건 (COMPLETED 5 + PENDING 1 + CANCELLED 1) + SELL 3건
--
--    getProfits() 로직: SELL.executed_price - holdings.average_price 차이로 수익 계산
--      MSFT: avg 415 → SELL 430 → +$15 (+3.6%) [수익]
--      GOOGL: holdings 없음 → fallback avgPrice=executed_price → 0% [정상 동작]
--      META:  holdings 없음 → fallback avgPrice=executed_price → 0% [정상 동작]
-- =====================================================================

-- BUY 내역
INSERT IGNORE INTO trade_orders
  (id, user_id, product_id, account_id, order_side, order_type, quantity,
   requested_price, executed_price, executed_amount, currency,
   order_status, execution_mode, ordered_at, executed_at, created_at, updated_at, status)
VALUES
  -- AAPL 10주 @180.00 매수 (완료)
  (2001, 1, (SELECT id FROM investment_products WHERE ticker = 'AAPL' LIMIT 1), 1,
   'BUY', 'MARKET', 10, 180.0000, 180.0000, 1800.0000, 'USD',
   'COMPLETED', 'MOCK',
   DATE_SUB(@now, INTERVAL 80 DAY), DATE_SUB(@now, INTERVAL 80 DAY), @now, @now, 'ACTIVE'),

  -- NVDA 5주 @850.00 매수 (완료)
  (2002, 1, (SELECT id FROM investment_products WHERE ticker = 'NVDA' LIMIT 1), 1,
   'BUY', 'MARKET', 5, 850.0000, 850.0000, 4250.0000, 'USD',
   'COMPLETED', 'MOCK',
   DATE_SUB(@now, INTERVAL 75 DAY), DATE_SUB(@now, INTERVAL 75 DAY), @now, @now, 'ACTIVE'),

  -- TSLA 3주 @245.00 매수 (완료)
  (2003, 1, (SELECT id FROM investment_products WHERE ticker = 'TSLA' LIMIT 1), 1,
   'BUY', 'MARKET', 3, 245.0000, 245.0000, 735.0000, 'USD',
   'COMPLETED', 'MOCK',
   DATE_SUB(@now, INTERVAL 70 DAY), DATE_SUB(@now, INTERVAL 70 DAY), @now, @now, 'ACTIVE'),

  -- MSFT 3주 @415.00 매수 (완료)
  (2004, 1, (SELECT id FROM investment_products WHERE ticker = 'MSFT' LIMIT 1), 1,
   'BUY', 'MARKET', 3, 415.0000, 415.0000, 1245.0000, 'USD',
   'COMPLETED', 'MOCK',
   DATE_SUB(@now, INTERVAL 19 DAY), DATE_SUB(@now, INTERVAL 19 DAY), @now, @now, 'ACTIVE'),

  -- QQQ 2주 @450.00 매수 (완료)
  (2005, 1, (SELECT id FROM investment_products WHERE ticker = 'QQQ' LIMIT 1), 1,
   'BUY', 'MARKET', 2, 450.0000, 450.0000, 900.0000, 'USD',
   'COMPLETED', 'MOCK',
   DATE_SUB(@now, INTERVAL 54 DAY), DATE_SUB(@now, INTERVAL 54 DAY), @now, @now, 'ACTIVE'),

  -- GOOGL 2주 @170.00 매수 (완료, 이후 전량 매도)
  (2006, 1, (SELECT id FROM investment_products WHERE ticker = 'GOOGL' LIMIT 1), 1,
   'BUY', 'MARKET', 2, 170.0000, 170.0000, 340.0000, 'USD',
   'COMPLETED', 'MOCK',
   DATE_SUB(@now, INTERVAL 58 DAY), DATE_SUB(@now, INTERVAL 58 DAY), @now, @now, 'ACTIVE'),

  -- META 3주 @520.00 매수 (완료, 이후 전량 매도)
  (2007, 1, (SELECT id FROM investment_products WHERE ticker = 'META' LIMIT 1), 1,
   'BUY', 'MARKET', 3, 520.0000, 520.0000, 1560.0000, 'USD',
   'COMPLETED', 'MOCK',
   DATE_SUB(@now, INTERVAL 49 DAY), DATE_SUB(@now, INTERVAL 49 DAY), @now, @now, 'ACTIVE'),

  -- AMD 5주 매수 (체결 대기 중 — PENDING 상태 검증)
  (2008, 1, (SELECT id FROM investment_products WHERE ticker = 'AMD' LIMIT 1), 1,
   'BUY', 'MARKET', 5, 160.0000, NULL, NULL, 'USD',
   'PENDING', 'MOCK',
   @now, NULL, @now, @now, 'ACTIVE'),

  -- NFLX 1주 매수 (취소 — CANCELLED 상태 검증)
  (2009, 1, (SELECT id FROM investment_products WHERE ticker = 'NFLX' LIMIT 1), 1,
   'BUY', 'MARKET', 1, 950.0000, NULL, NULL, 'USD',
   'CANCELLED', 'MOCK',
   DATE_SUB(@now, INTERVAL 34 DAY), NULL, @now, @now, 'ACTIVE');

-- SELL 내역 (판매수익 화면 검증용)
INSERT IGNORE INTO trade_orders
  (id, user_id, product_id, account_id, order_side, order_type, quantity,
   requested_price, executed_price, executed_amount, currency,
   order_status, execution_mode, ordered_at, executed_at, created_at, updated_at, status)
VALUES
  -- GOOGL 2주 @185.00 매도 → avg 170.00 기준 수익 +$30 (+8.8%)
  --   단, holdings에 GOOGL 없으면 fallback: avgPrice=185.00 → 수익률 0%
  (2010, 1, (SELECT id FROM investment_products WHERE ticker = 'GOOGL' LIMIT 1), 1,
   'SELL', 'MARKET', 2, 185.0000, 185.0000, 370.0000, 'USD',
   'COMPLETED', 'MOCK',
   DATE_SUB(@now, INTERVAL 38 DAY), DATE_SUB(@now, INTERVAL 38 DAY), @now, @now, 'ACTIVE'),

  -- META 3주 @495.00 매도 → avg 520.00 기준 손실 -$75 (-4.8%)
  --   단, holdings에 META 없으면 fallback: avgPrice=495.00 → 수익률 0%
  (2011, 1, (SELECT id FROM investment_products WHERE ticker = 'META' LIMIT 1), 1,
   'SELL', 'MARKET', 3, 495.0000, 495.0000, 1485.0000, 'USD',
   'COMPLETED', 'MOCK',
   DATE_SUB(@now, INTERVAL 28 DAY), DATE_SUB(@now, INTERVAL 28 DAY), @now, @now, 'ACTIVE'),

  -- MSFT 1주 @430.00 매도 → avg 415.00 기준 수익 +$15 (+3.6%)
  (2012, 1, (SELECT id FROM investment_products WHERE ticker = 'MSFT' LIMIT 1), 1,
   'SELL', 'MARKET', 1, 430.0000, 430.0000, 430.0000, 'USD',
   'COMPLETED', 'MOCK',
   DATE_SUB(@now, INTERVAL 4 DAY), DATE_SUB(@now, INTERVAL 4 DAY), @now, @now, 'ACTIVE');


-- =====================================================================
-- 4. KRW 입출금 계좌 (MY홈 원화 잔고 표시)
--    id=4: KRW 원화증권 계좌 (기존 존재)
--    id=10: KRW 입출금 통장 (신한은행) ← 이 스크립트에서 추가
--    주의: 실제 컬럼명 account_number_masked (V103 미적용 환경 대응)
-- =====================================================================

INSERT IGNORE INTO financial_accounts (
    id, user_id, account_type, institution_type, institution_name,
    account_name, account_number_masked, currency, balance,
    interest_rate, maturity_date, linked, linked_at, created_at, updated_at, status)
VALUES (
    10, 1, 'DEPOSIT', 'BANK', '신한은행',
    '신한 입출금 통장', '110-****-7890', 'KRW', 3500000.0000,
    NULL, NULL, TRUE, @now, @now, @now, 'ACTIVE');


-- =====================================================================
-- 적용 결과 확인
-- =====================================================================
SELECT '=== securities-mock-data 적용 결과 ===' AS info;
SELECT 'investment_products'                    AS tbl, COUNT(*) AS cnt FROM investment_products WHERE status = 'ACTIVE'
UNION ALL
SELECT 'investment_products(OVERSEAS)',           COUNT(*) FROM investment_products WHERE product_type = 'OVERSEAS' AND status = 'ACTIVE'
UNION ALL
SELECT 'investment_products(ETF)',                COUNT(*) FROM investment_products WHERE product_type = 'ETF'      AND status = 'ACTIVE'
UNION ALL
SELECT 'holdings(user=1)',                        COUNT(*) FROM holdings           WHERE user_id = 1
UNION ALL
SELECT 'holding_lots(user=1)',                    COUNT(*) FROM holding_lots       WHERE user_id = 1
UNION ALL
SELECT 'trade_orders(user=1)',                    COUNT(*) FROM trade_orders       WHERE user_id = 1
UNION ALL
SELECT 'trade_orders(COMPLETED)',                 COUNT(*) FROM trade_orders       WHERE user_id = 1 AND order_status = 'COMPLETED'
UNION ALL
SELECT 'trade_orders(PENDING)',                   COUNT(*) FROM trade_orders       WHERE user_id = 1 AND order_status = 'PENDING'
UNION ALL
SELECT 'trade_orders(CANCELLED)',                 COUNT(*) FROM trade_orders       WHERE user_id = 1 AND order_status = 'CANCELLED'
UNION ALL
SELECT 'trade_orders(SELL/COMPLETED)',            COUNT(*) FROM trade_orders       WHERE user_id = 1 AND order_side = 'SELL' AND order_status = 'COMPLETED'
UNION ALL
SELECT 'financial_accounts(user=1,KRW)',          COUNT(*) FROM financial_accounts WHERE user_id = 1 AND currency = 'KRW';
