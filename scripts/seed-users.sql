-- =====================================================================
-- 서비스 유저 시드 데이터 (7명)
-- 쏠: 333333 / 몰리: 444444 / 리노: 555555 / 슈: 666666
-- 도레미: 777777 / 플리: 888888 / 레이: 999999
--
-- 계좌 구조 (유저당):
--   - 신한투자증권 CMA 외화 계좌 (SECURITIES/USD) ─┐ 계좌번호 동일
--   - 신한투자증권 CMA 원화 계좌  (SECURITIES/KRW) ─┘
--   - 신한 외화 체인지업 예금     (DEPOSIT/USD)
--   - 신한 체인지업 체크카드      (DEPOSIT 계좌에 연결)
-- =====================================================================

SET @now = NOW();

-- ---------------------------------------------------------------------
-- 1. 유저 7명
-- ---------------------------------------------------------------------
INSERT IGNORE INTO users (id, name, email, phone_number, onboarding_status, investment_status, simple_password, created_at, updated_at, status)
VALUES
    (1,  '쏠',   'sol@eclipse.dev',    '010-1001-0001', 'REQUIRED', 'REQUIRED', '$2a$10$imo0.m7TXNmeruz/5RT3Ae7LYOr5JHqMB8/MS4R6Qu8M8mZshM5Xq', @now, @now, 'ACTIVE'),
    (2,  '몰리', 'molly@eclipse.dev',  '010-1001-0002', 'REQUIRED', 'REQUIRED', '$2a$10$HJN5q9UGQUNURls4078Aguzq1Gvtr/egXbHFeJ.lAebAbceM8/5si', @now, @now, 'ACTIVE'),
    (3,  '리노', 'rino@eclipse.dev',   '010-1001-0003', 'REQUIRED', 'REQUIRED', '$2a$10$XAMgHQJG1JSg0i96FI9KDu2XBWmJMLlVwMxbUF8cfBMdTMJ01w5c2', @now, @now, 'ACTIVE'),
    (4,  '슈',   'shu@eclipse.dev',    '010-1001-0004', 'REQUIRED', 'REQUIRED', '$2a$10$UAe0Sd9b.FIXK5H20XQ6jeAsXQmjb6R7a32EwRxP1VtR.k93hnFHW',  @now, @now, 'ACTIVE'),
    (5, '도레미', 'doremi@eclipse.dev','010-1001-0005', 'REQUIRED', 'REQUIRED', '$2a$10$mm7KIA8GLVECJTj8V1RFBujlnDtPYEun.yn9GpqSGAOPDNQgV/YrW',  @now, @now, 'ACTIVE'),
    (6,  '플리', 'pully@eclipse.dev',  '010-1001-0006', 'REQUIRED', 'REQUIRED', '$2a$10$WFkKtjcerWmgmTLQGbXry.bIo.DwbQXAMDkdSzJNtqPq5o89CyhZS',  @now, @now, 'ACTIVE'),
    (7,  '레이', 'ray@eclipse.dev',    '010-1001-0007', 'REQUIRED', 'REQUIRED', '$2a$10$2XvfBk8imQHvcBqiXfy9T.dK70g52kSc3C07gDZATgkVJ.GKTvKlC',  @now, @now, 'ACTIVE');

-- ---------------------------------------------------------------------
-- 2. 금융 계좌 (유저당 3개: CMA USD, CMA KRW, DEPOSIT USD)
--    CMA USD/KRW는 account_number 동일, CMA USD만 virtual_account_number 보유
--    account_number: 11자리, 110 으로 시작, 계좌 간 중복 없음
-- ---------------------------------------------------------------------
INSERT IGNORE INTO financial_accounts (
    id, user_id, account_type, institution_type, institution_name,
    account_name, account_number, virtual_account_number, currency, balance,
    interest_rate, maturity_date, linked, linked_at, created_at, updated_at, status, reserved_balance
) VALUES
-- 쏠 (user_id=1) ─────────────────────────────────────────────────────
    (15, 1, 'SECURITIES', 'SECURITIES_FIRM', '신한투자증권',
     '신한투자증권 CMA 외화 계좌', '110-023-847291', '110-456-182934', 'USD', 5000.0000,
     NULL, NULL, TRUE, @now, @now, @now, 'ACTIVE', 0.0000),
    (16, 1, 'SECURITIES', 'SECURITIES_FIRM', '신한투자증권',
     '신한투자증권 CMA 원화 계좌', '110-023-847291', NULL, 'KRW', 0.0000,
     NULL, NULL, TRUE, @now, @now, @now, 'ACTIVE', 0.0000),
    (17, 1, 'DEPOSIT', 'BANK', '신한은행',
     '신한 외화 체인지업 예금', '110-781-293047', NULL, 'USD', 0.0000,
     2.5000, NULL, TRUE, @now, @now, @now, 'ACTIVE', 0.0000),

-- 몰리 (user_id=2) ────────────────────────────────────────────────────
    (18, 2, 'SECURITIES', 'SECURITIES_FIRM', '신한투자증권',
     '신한투자증권 CMA 외화 계좌', '110-047-293810', '110-234-910284', 'USD', 5000.0000,
     NULL, NULL, TRUE, @now, @now, @now, 'ACTIVE', 0.0000),
    (19, 2, 'SECURITIES', 'SECURITIES_FIRM', '신한투자증권',
     '신한투자증권 CMA 원화 계좌', '110-047-293810', NULL, 'KRW', 0.0000,
     NULL, NULL, TRUE, @now, @now, @now, 'ACTIVE', 0.0000),
    (20, 2, 'DEPOSIT', 'BANK', '신한은행',
     '신한 외화 체인지업 예금', '110-812-047391', NULL, 'USD', 0.0000,
     2.5000, NULL, TRUE, @now, @now, @now, 'ACTIVE', 0.0000),

-- 리노 (user_id=3) ────────────────────────────────────────────────────
    (21, 3, 'SECURITIES', 'SECURITIES_FIRM', '신한투자증권',
     '신한투자증권 CMA 외화 계좌', '110-091-738204', '110-374-820193', 'USD', 5000.0000,
     NULL, NULL, TRUE, @now, @now, @now, 'ACTIVE', 0.0000),
    (22, 3, 'SECURITIES', 'SECURITIES_FIRM', '신한투자증권',
     '신한투자증권 CMA 원화 계좌', '110-091-738204', NULL, 'KRW', 0.0000,
     NULL, NULL, TRUE, @now, @now, @now, 'ACTIVE', 0.0000),
    (23, 3, 'DEPOSIT', 'BANK', '신한은행',
     '신한 외화 체인지업 예금', '110-629-183047', NULL, 'USD', 0.0000,
     2.5000, NULL, TRUE, @now, @now, @now, 'ACTIVE', 0.0000),

-- 슈 (user_id=4) ──────────────────────────────────────────────────────
    (24, 4, 'SECURITIES', 'SECURITIES_FIRM', '신한투자증권',
     '신한투자증권 CMA 외화 계좌', '110-138-204917', '110-519-047283', 'USD', 5000.0000,
     NULL, NULL, TRUE, @now, @now, @now, 'ACTIVE', 0.0000),
    (25, 4, 'SECURITIES', 'SECURITIES_FIRM', '신한투자증권',
     '신한투자증권 CMA 원화 계좌', '110-138-204917', NULL, 'KRW', 0.0000,
     NULL, NULL, TRUE, @now, @now, @now, 'ACTIVE', 0.0000),
    (26, 4, 'DEPOSIT', 'BANK', '신한은행',
     '신한 외화 체인지업 예금', '110-847-391020', NULL, 'USD', 0.0000,
     2.5000, NULL, TRUE, @now, @now, @now, 'ACTIVE', 0.0000),

-- 도레미 (user_id=5) ──────────────────────────────────────────────────
    (27, 5, 'SECURITIES', 'SECURITIES_FIRM', '신한투자증권',
     '신한투자증권 CMA 외화 계좌', '110-204-918374', '110-638-290471', 'USD', 5000.0000,
     NULL, NULL, TRUE, @now, @now, @now, 'ACTIVE', 0.0000),
    (28, 5, 'SECURITIES', 'SECURITIES_FIRM', '신한투자증권',
     '신한투자증권 CMA 원화 계좌', '110-204-918374', NULL, 'KRW', 0.0000,
     NULL, NULL, TRUE, @now, @now, @now, 'ACTIVE', 0.0000),
    (29, 5, 'DEPOSIT', 'BANK', '신한은행',
     '신한 외화 체인지업 예금', '110-912-047183', NULL, 'USD', 0.0000,
     2.5000, NULL, TRUE, @now, @now, @now, 'ACTIVE', 0.0000),

-- 플리 (user_id=6) ────────────────────────────────────────────────────
    (30, 6, 'SECURITIES', 'SECURITIES_FIRM', '신한투자증권',
     '신한투자증권 CMA 외화 계좌', '110-319-047281', '110-742-183920', 'USD', 5000.0000,
     NULL, NULL, TRUE, @now, @now, @now, 'ACTIVE', 0.0000),
    (31, 6, 'SECURITIES', 'SECURITIES_FIRM', '신한투자증권',
     '신한투자증권 CMA 원화 계좌', '110-319-047281', NULL, 'KRW', 0.0000,
     NULL, NULL, TRUE, @now, @now, @now, 'ACTIVE', 0.0000),
    (32, 6, 'DEPOSIT', 'BANK', '신한은행',
     '신한 외화 체인지업 예금', '110-081-294738', NULL, 'USD', 0.0000,
     2.5000, NULL, TRUE, @now, @now, @now, 'ACTIVE', 0.0000),

-- 레이 (user_id=7) ────────────────────────────────────────────────────
    (33, 7, 'SECURITIES', 'SECURITIES_FIRM', '신한투자증권',
     '신한투자증권 CMA 외화 계좌', '110-427-819304', '110-861-034729', 'USD', 5000.0000,
     NULL, NULL, TRUE, @now, @now, @now, 'ACTIVE', 0.0000),
    (34, 7, 'SECURITIES', 'SECURITIES_FIRM', '신한투자증권',
     '신한투자증권 CMA 원화 계좌', '110-427-819304', NULL, 'KRW', 0.0000,
     NULL, NULL, TRUE, @now, @now, @now, 'ACTIVE', 0.0000),
    (35, 7, 'DEPOSIT', 'BANK', '신한은행',
     '신한 외화 체인지업 예금', '110-193-820471', NULL, 'USD', 0.0000,
     2.5000, NULL, TRUE, @now, @now, @now, 'ACTIVE', 0.0000);

-- ---------------------------------------------------------------------
-- 3. 체크카드 (유저당 1장, DEPOSIT 계좌에 연결)
-- ---------------------------------------------------------------------
INSERT IGNORE INTO cards (id, user_id, linked_account_id, card_type, card_status,
    card_name, card_number_masked, issuer_name, linked, linked_at, created_at, updated_at, status)
VALUES
    (5,  1,  17, 'CHECK_CARD', 'LINKED', '신한 체인지업 체크카드', '9411-1023-4567-5274', '신한카드', TRUE, @now, @now, @now, 'ACTIVE'),
    (6,  2,  20, 'CHECK_CARD', 'LINKED', '신한 체인지업 체크카드', '9411-2134-5678-3891', '신한카드', TRUE, @now, @now, @now, 'ACTIVE'),
    (7,  3,  23, 'CHECK_CARD', 'LINKED', '신한 체인지업 체크카드', '9411-3245-6789-7463', '신한카드', TRUE, @now, @now, @now, 'ACTIVE'),
    (8,  4,  26, 'CHECK_CARD', 'LINKED', '신한 체인지업 체크카드', '9411-4356-7890-2158', '신한카드', TRUE, @now, @now, @now, 'ACTIVE'),
    (9,  5,  29, 'CHECK_CARD', 'LINKED', '신한 체인지업 체크카드', '9411-5467-8901-9037', '신한카드', TRUE, @now, @now, @now, 'ACTIVE'),
    (10, 6,  32, 'CHECK_CARD', 'LINKED', '신한 체인지업 체크카드', '9411-6578-9012-4621', '신한카드', TRUE, @now, @now, @now, 'ACTIVE'),
    (11, 7,  35, 'CHECK_CARD', 'LINKED', '신한 체인지업 체크카드', '9411-7689-0123-8345', '신한카드', TRUE, @now, @now, @now, 'ACTIVE');

-- ---------------------------------------------------------------------
-- 4. 알림 설정 (기본값 전체 ON)
-- ---------------------------------------------------------------------
INSERT IGNORE INTO notification_settings (user_id, ipo_allocation_enabled, ipo_refund_enabled, idle_dollar_enabled, created_at, updated_at, status)
VALUES
    (1,  TRUE, TRUE, TRUE, @now, @now, 'ACTIVE'),
    (2,  TRUE, TRUE, TRUE, @now, @now, 'ACTIVE'),
    (3,  TRUE, TRUE, TRUE, @now, @now, 'ACTIVE'),
    (4,  TRUE, TRUE, TRUE, @now, @now, 'ACTIVE'),
    (5,  TRUE, TRUE, TRUE, @now, @now, 'ACTIVE'),
    (6,  TRUE, TRUE, TRUE, @now, @now, 'ACTIVE'),
    (7,  TRUE, TRUE, TRUE, @now, @now, 'ACTIVE');

-- ---------------------------------------------------------------------
-- 5. 카드 사용 내역 (유저별 소비 패턴 상이, 3개월치 Apr~Jun 2026)
--    환율 출처: 수출입은행 API (deal_bas_r → base_rate, tts → tts)
--    주말/공휴일은 직전 영업일 환율 사용
-- ---------------------------------------------------------------------

-- 쏠 (user_id=1, card_id=5) ─ 구독(Netflix·GitHub)·쇼핑·여행 혼합
--   매월 고정: Netflix(1일 $15.99), GitHub(1일 $4.00), Spotify(10일 $9.99)
INSERT IGNORE INTO card_transactions (id, user_id, card_id, merchant_name, category, amount, currency, transacted_at, base_rate_at_time, tts_at_time, created_at, updated_at, status) VALUES
(501, 1, 5, 'Netflix', '콘텐츠', 15.9900, 'USD', '2026-04-01 09:00:00', 1530.5000, 1545.8000, @now, @now, 'ACTIVE'),
(502, 1, 5, 'GitHub',  '구독',    4.0000, 'USD', '2026-04-01 09:01:00', 1530.5000, 1545.8000, @now, @now, 'ACTIVE'),
(503, 1, 5, 'Spotify', '콘텐츠',  9.9900, 'USD', '2026-04-10 09:00:00', 1481.1000, 1495.9100, @now, @now, 'ACTIVE'),
(504, 1, 5, 'ASOS',    '쇼핑',   67.9000, 'USD', '2026-04-15 14:00:00', 1481.4000, 1496.2100, @now, @now, 'ACTIVE'),
(505, 1, 5, 'Starbucks','여행',    6.5000, 'USD', '2026-04-20 08:30:00', 1480.0000, 1494.8000, @now, @now, 'ACTIVE'),
(506, 1, 5, 'Netflix', '콘텐츠', 15.9900, 'USD', '2026-05-07 09:00:00', 1456.8000, 1471.3600, @now, @now, 'ACTIVE'),  -- 05/01 공휴일
(507, 1, 5, 'GitHub',  '구독',    4.0000, 'USD', '2026-05-07 09:01:00', 1456.8000, 1471.3600, @now, @now, 'ACTIVE'),
(508, 1, 5, 'Spotify', '콘텐츠',  9.9900, 'USD', '2026-05-12 09:00:00', 1473.0000, 1487.7300, @now, @now, 'ACTIVE'),
(509, 1, 5, 'Nike',    '쇼핑',   95.0000, 'USD', '2026-05-20 15:00:00', 1503.8000, 1518.8300, @now, @now, 'ACTIVE'),
(510, 1, 5, 'DoorDash','여행',   31.5000, 'USD', '2026-05-20 20:00:00', 1503.8000, 1518.8300, @now, @now, 'ACTIVE'),
(511, 1, 5, 'Netflix', '콘텐츠', 15.9900, 'USD', '2026-06-02 09:00:00', 1511.3000, 1526.4100, @now, @now, 'ACTIVE'),  -- 06/01 월요일
(512, 1, 5, 'GitHub',  '구독',    4.0000, 'USD', '2026-06-02 09:01:00', 1511.3000, 1526.4100, @now, @now, 'ACTIVE'),
(513, 1, 5, 'Spotify', '콘텐츠',  9.9900, 'USD', '2026-06-10 09:00:00', 1518.4000, 1533.5800, @now, @now, 'ACTIVE'),
(514, 1, 5, 'Amazon',  '쇼핑',   54.9900, 'USD', '2026-06-19 16:00:00', 1523.4000, 1538.6300, @now, @now, 'ACTIVE'),
(515, 1, 5, 'Uber',    '여행',   22.3000, 'USD', '2026-06-26 21:00:00', 1545.3000, 1560.7500, @now, @now, 'ACTIVE');

-- 몰리 (user_id=2, card_id=6) ─ 구독(Adobe·Notion)·쇼핑·여행 혼합
--   매월 고정: Adobe(3일 $54.99), Notion(1일 $8.00), Netflix(10일 $15.99)
INSERT IGNORE INTO card_transactions (id, user_id, card_id, merchant_name, category, amount, currency, transacted_at, base_rate_at_time, tts_at_time, created_at, updated_at, status) VALUES
(601, 2, 6, 'Notion',  '구독',    8.0000, 'USD', '2026-04-01 09:00:00', 1530.5000, 1545.8000, @now, @now, 'ACTIVE'),
(602, 2, 6, 'Adobe',   '구독',   54.9900, 'USD', '2026-04-01 09:05:00', 1530.5000, 1545.8000, @now, @now, 'ACTIVE'),
(603, 2, 6, 'Netflix', '콘텐츠', 15.9900, 'USD', '2026-04-10 09:00:00', 1481.1000, 1495.9100, @now, @now, 'ACTIVE'),
(604, 2, 6, 'Nike',    '쇼핑',   95.0000, 'USD', '2026-04-20 14:00:00', 1480.0000, 1494.8000, @now, @now, 'ACTIVE'),
(605, 2, 6, 'Chipotle','여행',   12.7500, 'USD', '2026-04-30 12:30:00', 1476.1000, 1490.8600, @now, @now, 'ACTIVE'),
(606, 2, 6, 'Notion',  '구독',    8.0000, 'USD', '2026-05-07 09:00:00', 1456.8000, 1471.3600, @now, @now, 'ACTIVE'),
(607, 2, 6, 'Adobe',   '구독',   54.9900, 'USD', '2026-05-07 09:05:00', 1456.8000, 1471.3600, @now, @now, 'ACTIVE'),
(608, 2, 6, 'Netflix', '콘텐츠', 15.9900, 'USD', '2026-05-12 09:00:00', 1473.0000, 1487.7300, @now, @now, 'ACTIVE'),
(609, 2, 6, 'ASOS',    '쇼핑',   54.9500, 'USD', '2026-05-20 16:00:00', 1503.8000, 1518.8300, @now, @now, 'ACTIVE'),
(610, 2, 6, 'Starbucks','여행',    7.2000, 'USD', '2026-05-12 08:30:00', 1473.0000, 1487.7300, @now, @now, 'ACTIVE'),
(611, 2, 6, 'Notion',  '구독',    8.0000, 'USD', '2026-06-02 09:00:00', 1511.3000, 1526.4100, @now, @now, 'ACTIVE'),
(612, 2, 6, 'Adobe',   '구독',   54.9900, 'USD', '2026-06-02 09:05:00', 1511.3000, 1526.4100, @now, @now, 'ACTIVE'),
(613, 2, 6, 'Netflix', '콘텐츠', 15.9900, 'USD', '2026-06-10 09:00:00', 1518.4000, 1533.5800, @now, @now, 'ACTIVE'),
(614, 2, 6, 'Adidas',  '쇼핑',   89.9500, 'USD', '2026-06-19 15:00:00', 1523.4000, 1538.6300, @now, @now, 'ACTIVE'),
(615, 2, 6, 'Uber Eats','여행',   28.9000, 'USD', '2026-06-26 20:00:00', 1545.3000, 1560.7500, @now, @now, 'ACTIVE');

-- 리노 (user_id=3, card_id=7) ─ 구독(GitHub·Adobe·Spotify)·쇼핑·여행 혼합
--   매월 고정: GitHub(1일 $4.00), Adobe(1일 $54.99), Spotify(8일 $9.99)
INSERT IGNORE INTO card_transactions (id, user_id, card_id, merchant_name, category, amount, currency, transacted_at, base_rate_at_time, tts_at_time, created_at, updated_at, status) VALUES
(701, 3, 7, 'GitHub',  '구독',    4.0000, 'USD', '2026-04-01 09:00:00', 1530.5000, 1545.8000, @now, @now, 'ACTIVE'),
(702, 3, 7, 'Adobe',   '구독',   54.9900, 'USD', '2026-04-01 09:05:00', 1530.5000, 1545.8000, @now, @now, 'ACTIVE'),
(703, 3, 7, 'Spotify', '콘텐츠',  9.9900, 'USD', '2026-04-10 09:00:00', 1481.1000, 1495.9100, @now, @now, 'ACTIVE'),
(704, 3, 7, 'Steam',   '콘텐츠', 29.9900, 'USD', '2026-04-15 20:00:00', 1481.4000, 1496.2100, @now, @now, 'ACTIVE'),
(705, 3, 7, 'DoorDash','여행',   26.4000, 'USD', '2026-04-30 19:30:00', 1476.1000, 1490.8600, @now, @now, 'ACTIVE'),
(706, 3, 7, 'GitHub',  '구독',    4.0000, 'USD', '2026-05-07 09:00:00', 1456.8000, 1471.3600, @now, @now, 'ACTIVE'),
(707, 3, 7, 'Adobe',   '구독',   54.9900, 'USD', '2026-05-07 09:05:00', 1456.8000, 1471.3600, @now, @now, 'ACTIVE'),
(708, 3, 7, 'Spotify', '콘텐츠',  9.9900, 'USD', '2026-05-12 09:00:00', 1473.0000, 1487.7300, @now, @now, 'ACTIVE'),
(709, 3, 7, 'Amazon',  '쇼핑',   67.4900, 'USD', '2026-05-20 16:00:00', 1503.8000, 1518.8300, @now, @now, 'ACTIVE'),
(710, 3, 7, 'Uber',    '여행',   19.8000, 'USD', '2026-05-12 22:00:00', 1473.0000, 1487.7300, @now, @now, 'ACTIVE'),
(711, 3, 7, 'GitHub',  '구독',    4.0000, 'USD', '2026-06-02 09:00:00', 1511.3000, 1526.4100, @now, @now, 'ACTIVE'),
(712, 3, 7, 'Adobe',   '구독',   54.9900, 'USD', '2026-06-02 09:05:00', 1511.3000, 1526.4100, @now, @now, 'ACTIVE'),
(713, 3, 7, 'Spotify', '콘텐츠',  9.9900, 'USD', '2026-06-10 09:00:00', 1518.4000, 1533.5800, @now, @now, 'ACTIVE'),
(714, 3, 7, 'Nike',    '쇼핑',  105.0000, 'USD', '2026-06-19 14:00:00', 1523.4000, 1538.6300, @now, @now, 'ACTIVE'),
(715, 3, 7, 'Starbucks','여행',    6.8000, 'USD', '2026-06-26 08:30:00', 1545.3000, 1560.7500, @now, @now, 'ACTIVE');

-- 슈 (user_id=4, card_id=8) ─ 구독(Netflix·Notion)·콘텐츠·쇼핑·여행 혼합
--   매월 고정: Netflix(3일 $15.99), Notion(5일 $8.00)
INSERT IGNORE INTO card_transactions (id, user_id, card_id, merchant_name, category, amount, currency, transacted_at, base_rate_at_time, tts_at_time, created_at, updated_at, status) VALUES
(801, 4, 8, 'Netflix',  '콘텐츠', 15.9900, 'USD', '2026-04-01 09:00:00', 1530.5000, 1545.8000, @now, @now, 'ACTIVE'),  -- 04/03 주말→04/01
(802, 4, 8, 'Notion',   '구독',    8.0000, 'USD', '2026-04-01 09:03:00', 1530.5000, 1545.8000, @now, @now, 'ACTIVE'),
(803, 4, 8, 'Starbucks','여행',    6.5000, 'USD', '2026-04-10 08:30:00', 1481.1000, 1495.9100, @now, @now, 'ACTIVE'),
(804, 4, 8, 'ASOS',     '쇼핑',   44.9900, 'USD', '2026-04-15 15:00:00', 1481.4000, 1496.2100, @now, @now, 'ACTIVE'),
(805, 4, 8, 'Chipotle', '여행',   12.7500, 'USD', '2026-04-30 12:00:00', 1476.1000, 1490.8600, @now, @now, 'ACTIVE'),
(806, 4, 8, 'Netflix',  '콘텐츠', 15.9900, 'USD', '2026-05-07 09:00:00', 1456.8000, 1471.3600, @now, @now, 'ACTIVE'),
(807, 4, 8, 'Notion',   '구독',    8.0000, 'USD', '2026-05-07 09:03:00', 1456.8000, 1471.3600, @now, @now, 'ACTIVE'),
(808, 4, 8, 'DoorDash', '여행',   33.8000, 'USD', '2026-05-12 20:00:00', 1473.0000, 1487.7300, @now, @now, 'ACTIVE'),
(809, 4, 8, 'Nike',     '쇼핑',   89.9500, 'USD', '2026-05-20 14:00:00', 1503.8000, 1518.8300, @now, @now, 'ACTIVE'),
(810, 4, 8, 'Starbucks','여행',    7.0000, 'USD', '2026-05-20 08:00:00', 1503.8000, 1518.8300, @now, @now, 'ACTIVE'),
(811, 4, 8, 'Netflix',  '콘텐츠', 15.9900, 'USD', '2026-06-02 09:00:00', 1511.3000, 1526.4100, @now, @now, 'ACTIVE'),
(812, 4, 8, 'Notion',   '구독',    8.0000, 'USD', '2026-06-02 09:03:00', 1511.3000, 1526.4100, @now, @now, 'ACTIVE'),
(813, 4, 8, 'Chipotle', '여행',   13.2500, 'USD', '2026-06-05 12:30:00', 1528.6000, 1543.8800, @now, @now, 'ACTIVE'),
(814, 4, 8, 'Etsy',     '쇼핑',   34.5000, 'USD', '2026-06-19 17:00:00', 1523.4000, 1538.6300, @now, @now, 'ACTIVE'),
(815, 4, 8, 'Uber Eats','여행',   29.7000, 'USD', '2026-06-26 20:30:00', 1545.3000, 1560.7500, @now, @now, 'ACTIVE');

-- 도레미 (user_id=5, card_id=9) ─ 구독(Spotify·Zoom)·콘텐츠·쇼핑·여행 혼합
--   매월 고정: Spotify(2일 $9.99), Zoom(5일 $13.33), YouTube Premium(10일 $13.99)
INSERT IGNORE INTO card_transactions (id, user_id, card_id, merchant_name, category, amount, currency, transacted_at, base_rate_at_time, tts_at_time, created_at, updated_at, status) VALUES
(901,  5, 9, 'Spotify',         '콘텐츠',  9.9900, 'USD', '2026-04-01 09:00:00', 1530.5000, 1545.8000, @now, @now, 'ACTIVE'),  -- 04/02 목요일
(902,  5, 9, 'YouTube Premium', '콘텐츠', 13.9900, 'USD', '2026-04-10 09:00:00', 1481.1000, 1495.9100, @now, @now, 'ACTIVE'),
(903,  5, 9, 'Zoom',            '구독',   13.3300, 'USD', '2026-04-15 09:00:00', 1481.4000, 1496.2100, @now, @now, 'ACTIVE'),
(904,  5, 9, 'Airbnb',          '여행',  185.0000, 'USD', '2026-04-20 11:00:00', 1480.0000, 1494.8000, @now, @now, 'ACTIVE'),
(905,  5, 9, 'H&M',             '쇼핑',   48.0000, 'USD', '2026-04-30 15:00:00', 1476.1000, 1490.8600, @now, @now, 'ACTIVE'),
(906,  5, 9, 'Spotify',         '콘텐츠',  9.9900, 'USD', '2026-05-07 09:00:00', 1456.8000, 1471.3600, @now, @now, 'ACTIVE'),
(907,  5, 9, 'YouTube Premium', '콘텐츠', 13.9900, 'USD', '2026-05-12 09:00:00', 1473.0000, 1487.7300, @now, @now, 'ACTIVE'),
(908,  5, 9, 'Zoom',            '구독',   13.3300, 'USD', '2026-05-07 09:05:00', 1456.8000, 1471.3600, @now, @now, 'ACTIVE'),
(909,  5, 9, 'Uber',            '여행',   24.3000, 'USD', '2026-05-20 18:00:00', 1503.8000, 1518.8300, @now, @now, 'ACTIVE'),
(910,  5, 9, 'Amazon',          '쇼핑',   79.9900, 'USD', '2026-05-20 14:00:00', 1503.8000, 1518.8300, @now, @now, 'ACTIVE'),
(911,  5, 9, 'Spotify',         '콘텐츠',  9.9900, 'USD', '2026-06-02 09:00:00', 1511.3000, 1526.4100, @now, @now, 'ACTIVE'),
(912,  5, 9, 'YouTube Premium', '콘텐츠', 13.9900, 'USD', '2026-06-10 09:00:00', 1518.4000, 1533.5800, @now, @now, 'ACTIVE'),
(913,  5, 9, 'Zoom',            '구독',   13.3300, 'USD', '2026-06-05 09:00:00', 1528.6000, 1543.8800, @now, @now, 'ACTIVE'),
(914,  5, 9, 'Expedia',         '여행',  320.0000, 'USD', '2026-06-19 11:00:00', 1523.4000, 1538.6300, @now, @now, 'ACTIVE'),
(915,  5, 9, 'ASOS',            '쇼핑',   67.9000, 'USD', '2026-06-26 16:00:00', 1545.3000, 1560.7500, @now, @now, 'ACTIVE');

-- 플리 (user_id=6, card_id=10) ─ 구독(Disney+·Coursera)·쇼핑·여행 혼합
--   매월 고정: Disney+(1일 $13.99), Coursera(5일 $39.00), Dropbox(10일 $9.99)
INSERT IGNORE INTO card_transactions (id, user_id, card_id, merchant_name, category, amount, currency, transacted_at, base_rate_at_time, tts_at_time, created_at, updated_at, status) VALUES
(1001, 6, 10, 'Disney+',  '콘텐츠', 13.9900, 'USD', '2026-04-01 09:00:00', 1530.5000, 1545.8000, @now, @now, 'ACTIVE'),
(1002, 6, 10, 'Coursera', '구독',   39.0000, 'USD', '2026-04-01 09:03:00', 1530.5000, 1545.8000, @now, @now, 'ACTIVE'),
(1003, 6, 10, 'Dropbox',  '구독',    9.9900, 'USD', '2026-04-10 09:00:00', 1481.1000, 1495.9100, @now, @now, 'ACTIVE'),
(1004, 6, 10, 'Nike',     '쇼핑',   89.9500, 'USD', '2026-04-20 13:00:00', 1480.0000, 1494.8000, @now, @now, 'ACTIVE'),
(1005, 6, 10, 'Chipotle', '여행',   13.5000, 'USD', '2026-04-30 12:00:00', 1476.1000, 1490.8600, @now, @now, 'ACTIVE'),
(1006, 6, 10, 'Disney+',  '콘텐츠', 13.9900, 'USD', '2026-05-07 09:00:00', 1456.8000, 1471.3600, @now, @now, 'ACTIVE'),
(1007, 6, 10, 'Coursera', '구독',   39.0000, 'USD', '2026-05-07 09:03:00', 1456.8000, 1471.3600, @now, @now, 'ACTIVE'),
(1008, 6, 10, 'Dropbox',  '구독',    9.9900, 'USD', '2026-05-12 09:00:00', 1473.0000, 1487.7300, @now, @now, 'ACTIVE'),
(1009, 6, 10, 'Adidas',   '쇼핑',   74.9500, 'USD', '2026-05-20 14:00:00', 1503.8000, 1518.8300, @now, @now, 'ACTIVE'),
(1010, 6, 10, 'Uber Eats','여행',   35.2000, 'USD', '2026-05-12 20:00:00', 1473.0000, 1487.7300, @now, @now, 'ACTIVE'),
(1011, 6, 10, 'Disney+',  '콘텐츠', 13.9900, 'USD', '2026-06-02 09:00:00', 1511.3000, 1526.4100, @now, @now, 'ACTIVE'),
(1012, 6, 10, 'Coursera', '구독',   39.0000, 'USD', '2026-06-02 09:03:00', 1511.3000, 1526.4100, @now, @now, 'ACTIVE'),
(1013, 6, 10, 'Dropbox',  '구독',    9.9900, 'USD', '2026-06-10 09:00:00', 1518.4000, 1533.5800, @now, @now, 'ACTIVE'),
(1014, 6, 10, 'Amazon',   '쇼핑',   55.9900, 'USD', '2026-06-19 15:00:00', 1523.4000, 1538.6300, @now, @now, 'ACTIVE'),
(1015, 6, 10, 'DoorDash', '여행',   38.7000, 'USD', '2026-06-26 20:00:00', 1545.3000, 1560.7500, @now, @now, 'ACTIVE');

-- 레이 (user_id=7, card_id=11) ─ 구독(Netflix·Slack)·콘텐츠·쇼핑·여행 혼합
--   매월 고정: Netflix(2일 $15.99), Slack(5일 $7.25), Spotify(10일 $9.99)
INSERT IGNORE INTO card_transactions (id, user_id, card_id, merchant_name, category, amount, currency, transacted_at, base_rate_at_time, tts_at_time, created_at, updated_at, status) VALUES
(1101, 7, 11, 'Netflix', '콘텐츠', 15.9900, 'USD', '2026-04-01 09:00:00', 1530.5000, 1545.8000, @now, @now, 'ACTIVE'),  -- 04/02 목요일
(1102, 7, 11, 'Slack',   '구독',    7.2500, 'USD', '2026-04-01 09:03:00', 1530.5000, 1545.8000, @now, @now, 'ACTIVE'),
(1103, 7, 11, 'Spotify', '콘텐츠',  9.9900, 'USD', '2026-04-10 09:00:00', 1481.1000, 1495.9100, @now, @now, 'ACTIVE'),
(1104, 7, 11, 'Expedia', '여행',  210.0000, 'USD', '2026-04-15 11:00:00', 1481.4000, 1496.2100, @now, @now, 'ACTIVE'),
(1105, 7, 11, 'Amazon',  '쇼핑',   89.9900, 'USD', '2026-04-30 16:00:00', 1476.1000, 1490.8600, @now, @now, 'ACTIVE'),
(1106, 7, 11, 'Netflix', '콘텐츠', 15.9900, 'USD', '2026-05-07 09:00:00', 1456.8000, 1471.3600, @now, @now, 'ACTIVE'),
(1107, 7, 11, 'Slack',   '구독',    7.2500, 'USD', '2026-05-07 09:03:00', 1456.8000, 1471.3600, @now, @now, 'ACTIVE'),
(1108, 7, 11, 'Spotify', '콘텐츠',  9.9900, 'USD', '2026-05-12 09:00:00', 1473.0000, 1487.7300, @now, @now, 'ACTIVE'),
(1109, 7, 11, 'Uber',    '여행',   27.4000, 'USD', '2026-05-20 22:00:00', 1503.8000, 1518.8300, @now, @now, 'ACTIVE'),
(1110, 7, 11, 'Nike',    '쇼핑',  120.0000, 'USD', '2026-05-20 15:00:00', 1503.8000, 1518.8300, @now, @now, 'ACTIVE'),
(1111, 7, 11, 'Netflix', '콘텐츠', 15.9900, 'USD', '2026-06-02 09:00:00', 1511.3000, 1526.4100, @now, @now, 'ACTIVE'),
(1112, 7, 11, 'Slack',   '구독',    7.2500, 'USD', '2026-06-02 09:03:00', 1511.3000, 1526.4100, @now, @now, 'ACTIVE'),
(1113, 7, 11, 'Spotify', '콘텐츠',  9.9900, 'USD', '2026-06-10 09:00:00', 1518.4000, 1533.5800, @now, @now, 'ACTIVE'),
(1114, 7, 11, 'Booking.com','여행',195.0000, 'USD', '2026-06-19 11:00:00', 1523.4000, 1538.6300, @now, @now, 'ACTIVE'),
(1115, 7, 11, 'Adidas',  '쇼핑',   64.9500, 'USD', '2026-06-26 15:00:00', 1545.3000, 1560.7500, @now, @now, 'ACTIVE');


