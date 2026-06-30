-- =====================================================================
-- 서비스 유저 7명 시연 데이터 (user_id 1~7)
-- 전제조건: seed-users.sql 실행 완료
--   유저 계좌 고정 ID:
--     쏠(1):   CMA=15, DEP=17  /  몰리(2): CMA=18, DEP=20
--     리노(3): CMA=21, DEP=23  /  슈(4):   CMA=24, DEP=26
--     도레미(5):CMA=27, DEP=29 /  플리(6): CMA=30, DEP=32
--     레이(7): CMA=33, DEP=35
--
-- IPO (RDS 실제 ID):
--   MANE(78)  $17  listed 2026-02-04  현재 $123.58 ↑↑
--   EIKN(76)  $18  listed 2026-02-05  현재 $12.74  ↓↓
--   CBRS(98)  $185 listed 2026-05-14  현재 $216.16 ↑
--   LFTO(65)  $23  listed 2026-06-04  현재 $25.47  ↑
--   SPCX(89)  $135 listed 2026-06-12  현재 $164.19 ↑
--   DSC(63)   $17  listed 2026-06-25  현재 $5.77   ↓↓
--   BLZX(202) $22  listed 2026-06-30  현재 $28.50  ↑  (LOCKED)
--   NXTN(203) $45  listed 2026-06-30  현재 $52.30  ↑  (LOCKED)
--
-- 배정율 프로파일:
--   쏠(1):    중간  (0~67%)   / 몰리(2):  낙첨 위주 (0~25%)
--   리노(3):  고배정 (80%↑)  / 슈(4):    균형 (0~50%)
--   도레미(5):다양  (0~60%)  / 플리(6):  신규·소극 (0~33%)
--   레이(7):  분산  (20~75%)
--
-- ID 범위 (sub/plan/alloc):
--   쏠:    1001~1005 / 101~104  / 1001~1008
--   몰리:  1101~1103 / 111~112  / 1101~1104
--   리노:  1201~1205 / 121~124  / 1201~1208
--   슈:    1301~1304 / 131~133  / 1301~1306
--   도레미:1401~1406 / 141~144  / 1401~1408
--   플리:  1501~1503 / 151~152  / 1501~1504
--   레이:  1601~1605 / 161~163  / 1601~1606
-- =====================================================================

SET @now = NOW();

-- =====================================================================
-- 0. 전체 초기화 (FK 역순)
-- =====================================================================
DELETE tt FROM transfer_transactions tt
    JOIN return_plan_allocations rpa ON tt.allocation_id = rpa.id
    JOIN return_plans rp ON rpa.return_plan_id = rp.id
    WHERE rp.user_id BETWEEN 1 AND 7;
DELETE rpa FROM return_plan_allocations rpa
    JOIN return_plans rp ON rpa.return_plan_id = rp.id
    WHERE rp.user_id BETWEEN 1 AND 7;
DELETE FROM return_plans WHERE user_id BETWEEN 1 AND 7;

DELETE bh FROM balance_holds bh
    JOIN ipo_subscriptions s ON bh.subscription_id = s.id
    WHERE s.user_id BETWEEN 1 AND 7;
DELETE FROM ipo_subscriptions WHERE user_id BETWEEN 1 AND 7;

DELETE FROM rp_contracts             WHERE user_id BETWEEN 1 AND 7;
DELETE FROM trade_orders             WHERE user_id BETWEEN 1 AND 7;
DELETE FROM fx_exchange_transactions WHERE user_id BETWEEN 1 AND 7;

-- 온보딩 완료 처리
UPDATE users SET onboarding_status='COMPLETED', investment_status='COMPLETED', updated_at=@now
WHERE id BETWEEN 1 AND 7;


-- =====================================================================
-- ① 쏠 (user_id=1, CMA=15, DEP=17)
--    중간 배정율 / BLZX 현재 보유
--
--    MANE(78)  30주×$17=$510   → 20주(67%) 배정  DEPOSITED
--    EIKN(76)  20주×$18=$360   →  0주(낙첨)       DEPOSITED
--    CBRS(98)   4주×$185=$740  →  2주(50%) 배정  DEPOSITED
--    DSC(63)   40주×$17=$680   →  8주(20%) 배정  DEPOSITED
--    BLZX(202) 20주×$22=$440   → 10주(50%) 배정  COMPLETED
--
--    잔액: CMA $4,830.67  reserved $444.40  DEP $566.00
-- =====================================================================
INSERT INTO ipo_subscriptions (
    id, user_id, ipo_id, securities_account_id,
    requested_shares, offer_price, subscription_amount, currency,
    subscription_status, execution_mode, subscribed_at,
    allocated_shares, allocated_amount, refund_amount, allocation_rate,
    result_status, confirmed_at, created_at, updated_at, status
) VALUES
    (1001,1,78,15, 30,17.0000, 510.0000,'USD','CONFIRMED','MOCK','2026-01-25 10:00:00', 20,340.0000, 173.4000,0.6667,'DEPOSITED','2026-02-04 21:00:00',@now,@now,'ACTIVE'),
    (1002,1,76,15, 20,18.0000, 360.0000,'USD','CONFIRMED','MOCK','2026-01-26 10:00:00',  0,  0.0000, 363.6000,0.0000,'DEPOSITED','2026-02-05 21:00:00',@now,@now,'ACTIVE'),
    (1003,1,98,15,  4,185.0000,740.0000,'USD','CONFIRMED','MOCK','2026-05-05 10:00:00',  2,370.0000, 375.5500,0.5000,'DEPOSITED','2026-05-14 21:00:00',@now,@now,'ACTIVE'),
    (1004,1,63,15, 40,17.0000, 680.0000,'USD','CONFIRMED','MOCK','2026-06-15 10:00:00',  8,136.0000, 550.1200,0.2000,'DEPOSITED','2026-06-25 21:00:00',@now,@now,'ACTIVE'),
    (1005,1,202,15,20,22.0000, 440.0000,'USD','CONFIRMED','MOCK','2026-06-22 10:00:00', 10,220.0000,1001.5500,0.5000,'COMPLETED','2026-06-30 12:00:00',@now,@now,'ACTIVE');
-- refund 검증: MANE $515.10-$340-$1.70=$173.40 / EIKN $363.60 / CBRS $747.40-$370-$1.85=$375.55
--              DSC $686.80-$136-$0.68=$550.12 / BLZX $444.40-$220-$1.10=$223.30 (COMPLETED)

INSERT INTO balance_holds (account_id,subscription_id,amount,hold_status,released_at,settled_at,created_at,updated_at,status) VALUES
    (15,1001, 515.1000,'RELEASED','2026-02-04 21:30:00',NULL,'2026-01-25 10:00:00','2026-02-04 21:30:00','ACTIVE'),
    (15,1002, 363.6000,'RELEASED','2026-02-05 21:30:00',NULL,'2026-01-26 10:00:00','2026-02-05 21:30:00','ACTIVE'),
    (15,1003, 747.4000,'RELEASED','2026-05-14 21:30:00',NULL,'2026-05-05 10:00:00','2026-05-14 21:30:00','ACTIVE'),
    (15,1004, 686.8000,'RELEASED','2026-06-25 21:30:00',NULL,'2026-06-15 10:00:00','2026-06-25 21:30:00','ACTIVE'),
    (15,1005, 444.4000,'LOCKED',  NULL,                 NULL,'2026-06-22 10:00:00',@now,                'ACTIVE');

INSERT INTO return_plans (id,user_id,subscription_id,next_ipo_id,total_refund_amount,currency,current_securities_balance,savings_interest_rate,plan_status,executed_at,created_at,updated_at,status) VALUES
    (101,1,1001,NULL, 173.4000,'USD',5000.0000,2.5000,'EXECUTED','2026-02-05 10:00:00','2026-02-04 21:30:00','2026-02-05 10:00:00','ACTIVE'),
    (102,1,1002,NULL, 363.6000,'USD',5000.0000,2.5000,'EXECUTED','2026-02-06 10:00:00','2026-02-05 21:30:00','2026-02-06 10:00:00','ACTIVE'),
    (103,1,1003,NULL, 375.5500,'USD',5000.0000,2.5000,'EXECUTED','2026-05-15 10:00:00','2026-05-14 21:30:00','2026-05-15 10:00:00','ACTIVE'),
    (104,1,1004,NULL, 550.1200,'USD',5000.0000,2.5000,'EXECUTED','2026-06-26 10:00:00','2026-06-25 21:30:00','2026-06-26 10:00:00','ACTIVE');

-- DEP: MANE 20%=$34 / EIKN 40%=$145 / CBRS 30%=$112 / DSC 50%=$275 → Σ=$566
INSERT INTO return_plan_allocations (id,return_plan_id,destination_type,destination_account_id,allocation_ratio,allocation_amount,allocation_status,created_at,updated_at,status) VALUES
    (1001,101,'SECURITIES',15, 80.00, 139.4000,'EXECUTED','2026-02-05 10:00:00','2026-02-05 10:00:00','ACTIVE'),
    (1002,101,'DEPOSIT',   17, 20.00,  34.0000,'EXECUTED','2026-02-05 10:00:00','2026-02-05 10:00:00','ACTIVE'),
    (1003,102,'SECURITIES',15, 60.00, 218.6000,'EXECUTED','2026-02-06 10:00:00','2026-02-06 10:00:00','ACTIVE'),
    (1004,102,'DEPOSIT',   17, 40.00, 145.0000,'EXECUTED','2026-02-06 10:00:00','2026-02-06 10:00:00','ACTIVE'),
    (1005,103,'SECURITIES',15, 70.00, 263.5500,'EXECUTED','2026-05-15 10:00:00','2026-05-15 10:00:00','ACTIVE'),
    (1006,103,'DEPOSIT',   17, 30.00, 112.0000,'EXECUTED','2026-05-15 10:00:00','2026-05-15 10:00:00','ACTIVE'),
    (1007,104,'SECURITIES',15, 50.00, 275.1200,'EXECUTED','2026-06-26 10:00:00','2026-06-26 10:00:00','ACTIVE'),
    (1008,104,'DEPOSIT',   17, 50.00, 275.0000,'EXECUTED','2026-06-26 10:00:00','2026-06-26 10:00:00','ACTIVE');

-- CMA: $5000 - ($340+$0+$370+$136+$220) + ($173.40+$363.60+$375.55+$550.12) - $566 = $4,830.67
UPDATE financial_accounts SET balance=4830.6700, reserved_balance=444.4000, updated_at=@now WHERE id=15;
UPDATE financial_accounts SET balance= 566.0000, reserved_balance=  0.0000, updated_at=@now WHERE id=17;


-- =====================================================================
-- ② 몰리 (user_id=2, CMA=18, DEP=20)
--    낙첨 위주, 소극적 / NXTN 현재 보유
--
--    LFTO(65)  20주×$23=$460   →  0주(낙첨)       DEPOSITED
--    DSC(63)   20주×$17=$340   →  5주(25%) 배정   DEPOSITED
--    NXTN(203)  8주×$45=$360   →  2주(25%) 배정   COMPLETED
--
--    잔액: CMA $5,424.58  reserved $363.60  DEP $123.00
-- =====================================================================
INSERT INTO ipo_subscriptions (
    id, user_id, ipo_id, securities_account_id,
    requested_shares, offer_price, subscription_amount, currency,
    subscription_status, execution_mode, subscribed_at,
    allocated_shares, allocated_amount, refund_amount, allocation_rate,
    result_status, confirmed_at, created_at, updated_at, status
) VALUES
    (1101,2,65,18, 20,23.0000,460.0000,'USD','CONFIRMED','MOCK','2026-05-26 10:00:00',  0,  0.0000, 464.6000,0.0000,'DEPOSITED','2026-06-04 21:00:00',@now,@now,'ACTIVE'),
    (1102,2,63,18, 20,17.0000,340.0000,'USD','CONFIRMED','MOCK','2026-06-15 10:00:00',  5, 85.0000, 257.9750,0.2500,'DEPOSITED','2026-06-25 21:00:00',@now,@now,'ACTIVE'),
    (1103,2,203,18, 8,45.0000,360.0000,'USD','CONFIRMED','MOCK','2026-06-22 10:00:00',  2, 90.0000, 273.1500,0.2500,'COMPLETED','2026-06-30 12:00:00',@now,@now,'ACTIVE');
-- refund: LFTO $464.60 / DSC $343.40-$85-$0.425=$257.975 / NXTN $363.60-$90-$0.45=$273.15

INSERT INTO balance_holds (account_id,subscription_id,amount,hold_status,released_at,settled_at,created_at,updated_at,status) VALUES
    (18,1101,464.6000,'RELEASED','2026-06-04 21:30:00',NULL,'2026-05-26 10:00:00','2026-06-04 21:30:00','ACTIVE'),
    (18,1102,343.4000,'RELEASED','2026-06-25 21:30:00',NULL,'2026-06-15 10:00:00','2026-06-25 21:30:00','ACTIVE'),
    (18,1103,363.6000,'LOCKED',  NULL,                 NULL,'2026-06-22 10:00:00',@now,                'ACTIVE');

INSERT INTO return_plans (id,user_id,subscription_id,next_ipo_id,total_refund_amount,currency,current_securities_balance,savings_interest_rate,plan_status,executed_at,created_at,updated_at,status) VALUES
    (111,2,1101,NULL,464.6000,'USD',5000.0000,2.5000,'EXECUTED','2026-06-05 10:00:00','2026-06-04 21:30:00','2026-06-05 10:00:00','ACTIVE'),
    (112,2,1102,NULL,257.9750,'USD',5000.0000,2.5000,'EXECUTED','2026-06-26 10:00:00','2026-06-25 21:30:00','2026-06-26 10:00:00','ACTIVE');

-- DEP: LFTO 10%=$46 / DSC 30%=$77 → Σ=$123
INSERT INTO return_plan_allocations (id,return_plan_id,destination_type,destination_account_id,allocation_ratio,allocation_amount,allocation_status,created_at,updated_at,status) VALUES
    (1101,111,'SECURITIES',18, 90.00, 418.6000,'EXECUTED','2026-06-05 10:00:00','2026-06-05 10:00:00','ACTIVE'),
    (1102,111,'DEPOSIT',   20, 10.00,  46.0000,'EXECUTED','2026-06-05 10:00:00','2026-06-05 10:00:00','ACTIVE'),
    (1103,112,'SECURITIES',18, 70.00, 180.9750,'EXECUTED','2026-06-26 10:00:00','2026-06-26 10:00:00','ACTIVE'),
    (1104,112,'DEPOSIT',   20, 30.00,  77.0000,'EXECUTED','2026-06-26 10:00:00','2026-06-26 10:00:00','ACTIVE');

-- CMA: $5000 - ($0+$85+$90) + ($464.60+$257.975) - $123 = $5,424.575
UPDATE financial_accounts SET balance=5424.5750, reserved_balance=363.6000, updated_at=@now WHERE id=18;
UPDATE financial_accounts SET balance= 123.0000, reserved_balance=  0.0000, updated_at=@now WHERE id=20;


-- =====================================================================
-- ③ 리노 (user_id=3, CMA=21, DEP=23)
--    고배정율 (80% 고정), 적극적 / BLZX 현재 보유
--
--    MANE(78)  20주×$17=$340   → 16주(80%) 배정  DEPOSITED
--    EIKN(76)  30주×$18=$540   → 24주(80%) 배정  DEPOSITED
--    SPCX(89)  10주×$135=$1,350→  8주(80%) 배정  DEPOSITED
--    CBRS(98)   5주×$185=$925  →  4주(80%) 배정  DEPOSITED
--    BLZX(202) 25주×$22=$550   → 20주(80%) 배정  COMPLETED
--
--    잔액: CMA $2,507.93  reserved $555.50  DEP $178.00
-- =====================================================================
INSERT INTO ipo_subscriptions (
    id, user_id, ipo_id, securities_account_id,
    requested_shares, offer_price, subscription_amount, currency,
    subscription_status, execution_mode, subscribed_at,
    allocated_shares, allocated_amount, refund_amount, allocation_rate,
    result_status, confirmed_at, created_at, updated_at, status
) VALUES
    (1201,3,78,21, 20,17.0000,  340.0000,'USD','CONFIRMED','MOCK','2026-01-25 10:00:00', 16, 272.0000,  70.0400,0.8000,'DEPOSITED','2026-02-04 21:00:00',@now,@now,'ACTIVE'),
    (1202,3,76,21, 30,18.0000,  540.0000,'USD','CONFIRMED','MOCK','2026-01-26 10:00:00', 24, 432.0000, 111.2400,0.8000,'DEPOSITED','2026-02-05 21:00:00',@now,@now,'ACTIVE'),
    (1203,3,89,21, 10,135.0000,1350.0000,'USD','CONFIRMED','MOCK','2026-06-03 10:00:00',  8,1080.0000, 278.1000,0.8000,'DEPOSITED','2026-06-12 21:00:00',@now,@now,'ACTIVE'),
    (1204,3,98,21,  5,185.0000,  925.0000,'USD','CONFIRMED','MOCK','2026-05-05 10:00:00',  4, 740.0000, 190.5500,0.8000,'DEPOSITED','2026-05-14 21:00:00',@now,@now,'ACTIVE'),
    (1205,3,202,21,25,22.0000,   550.0000,'USD','CONFIRMED','MOCK','2026-06-22 10:00:00', 20, 440.0000, 104.5000,0.8000,'COMPLETED','2026-06-30 12:00:00',@now,@now,'ACTIVE');
-- refund: MANE $343.40-$272-$1.36=$70.04 / EIKN $545.40-$432-$2.16=$111.24
--         SPCX $1363.50-$1080-$5.40=$278.10 / CBRS $934.25-$740-$3.70=$190.55
--         BLZX $555.50-$440-$2.20=$113.30 (COMPLETED)

INSERT INTO balance_holds (account_id,subscription_id,amount,hold_status,released_at,settled_at,created_at,updated_at,status) VALUES
    (21,1201,  343.4000,'RELEASED','2026-02-04 21:30:00',NULL,'2026-01-25 10:00:00','2026-02-04 21:30:00','ACTIVE'),
    (21,1202,  545.4000,'RELEASED','2026-02-05 21:30:00',NULL,'2026-01-26 10:00:00','2026-02-05 21:30:00','ACTIVE'),
    (21,1203, 1363.5000,'RELEASED','2026-06-12 21:30:00',NULL,'2026-06-03 10:00:00','2026-06-12 21:30:00','ACTIVE'),
    (21,1204,  934.2500,'RELEASED','2026-05-14 21:30:00',NULL,'2026-05-05 10:00:00','2026-05-14 21:30:00','ACTIVE'),
    (21,1205,  555.5000,'LOCKED',  NULL,                 NULL,'2026-06-22 10:00:00',@now,                'ACTIVE');

INSERT INTO return_plans (id,user_id,subscription_id,next_ipo_id,total_refund_amount,currency,current_securities_balance,savings_interest_rate,plan_status,executed_at,created_at,updated_at,status) VALUES
    (121,3,1201,NULL, 70.0400,'USD',5000.0000,2.5000,'EXECUTED','2026-02-05 10:00:00','2026-02-04 21:30:00','2026-02-05 10:00:00','ACTIVE'),
    (122,3,1202,NULL,111.2400,'USD',5000.0000,2.5000,'EXECUTED','2026-02-06 10:00:00','2026-02-05 21:30:00','2026-02-06 10:00:00','ACTIVE'),
    (123,3,1203,NULL,278.1000,'USD',5000.0000,2.5000,'EXECUTED','2026-06-13 10:00:00','2026-06-12 21:30:00','2026-06-13 10:00:00','ACTIVE'),
    (124,3,1204,NULL,190.5500,'USD',5000.0000,2.5000,'EXECUTED','2026-05-15 10:00:00','2026-05-14 21:30:00','2026-05-15 10:00:00','ACTIVE');

-- DEP: MANE 20%=$14 / EIKN 30%=$33 / SPCX 20%=$55 / CBRS 40%=$76 → Σ=$178
INSERT INTO return_plan_allocations (id,return_plan_id,destination_type,destination_account_id,allocation_ratio,allocation_amount,allocation_status,created_at,updated_at,status) VALUES
    (1201,121,'SECURITIES',21, 80.00,  56.0400,'EXECUTED','2026-02-05 10:00:00','2026-02-05 10:00:00','ACTIVE'),
    (1202,121,'DEPOSIT',   23, 20.00,  14.0000,'EXECUTED','2026-02-05 10:00:00','2026-02-05 10:00:00','ACTIVE'),
    (1203,122,'SECURITIES',21, 70.00,  78.2400,'EXECUTED','2026-02-06 10:00:00','2026-02-06 10:00:00','ACTIVE'),
    (1204,122,'DEPOSIT',   23, 30.00,  33.0000,'EXECUTED','2026-02-06 10:00:00','2026-02-06 10:00:00','ACTIVE'),
    (1205,123,'SECURITIES',21, 80.00, 223.1000,'EXECUTED','2026-06-13 10:00:00','2026-06-13 10:00:00','ACTIVE'),
    (1206,123,'DEPOSIT',   23, 20.00,  55.0000,'EXECUTED','2026-06-13 10:00:00','2026-06-13 10:00:00','ACTIVE'),
    (1207,124,'SECURITIES',21, 60.00, 114.5500,'EXECUTED','2026-05-15 10:00:00','2026-05-15 10:00:00','ACTIVE'),
    (1208,124,'DEPOSIT',   23, 40.00,  76.0000,'EXECUTED','2026-05-15 10:00:00','2026-05-15 10:00:00','ACTIVE');

-- CMA: $5000 - ($272+$432+$1080+$740+$440) + ($70.04+$111.24+$278.10+$190.55) - $178 = $2,507.93
UPDATE financial_accounts SET balance=2507.9300, reserved_balance=555.5000, updated_at=@now WHERE id=21;
UPDATE financial_accounts SET balance= 178.0000, reserved_balance=  0.0000, updated_at=@now WHERE id=23;


-- =====================================================================
-- ④ 슈 (user_id=4, CMA=24, DEP=26)
--    균형 배정 (0~50%) / NXTN 현재 보유
--
--    LFTO(65)  40주×$23=$920   →  0주(낙첨)       DEPOSITED
--    MANE(78)  50주×$17=$850   → 25주(50%) 배정   DEPOSITED
--    DSC(63)   80주×$17=$1,360 → 30주(37.5%) 배정 DEPOSITED
--    NXTN(203) 15주×$45=$675   →  6주(40%) 배정   COMPLETED
--
--    잔액: CMA $5,000.63  reserved $681.75  DEP $1,016.00
-- =====================================================================
INSERT INTO ipo_subscriptions (
    id, user_id, ipo_id, securities_account_id,
    requested_shares, offer_price, subscription_amount, currency,
    subscription_status, execution_mode, subscribed_at,
    allocated_shares, allocated_amount, refund_amount, allocation_rate,
    result_status, confirmed_at, created_at, updated_at, status
) VALUES
    (1301,4,65,24, 40,23.0000,  920.0000,'USD','CONFIRMED','MOCK','2026-05-26 10:00:00',  0,   0.0000, 929.2000,0.0000,'DEPOSITED','2026-06-04 21:00:00',@now,@now,'ACTIVE'),
    (1302,4,78,24, 50,17.0000,  850.0000,'USD','CONFIRMED','MOCK','2026-01-25 10:00:00', 25, 425.0000, 431.3750,0.5000,'DEPOSITED','2026-02-04 21:00:00',@now,@now,'ACTIVE'),
    (1303,4,63,24, 80,17.0000, 1360.0000,'USD','CONFIRMED','MOCK','2026-06-15 10:00:00', 30, 510.0000, 861.0500,0.3750,'DEPOSITED','2026-06-25 21:00:00',@now,@now,'ACTIVE'),
    (1304,4,203,24,15,45.0000,  675.0000,'USD','CONFIRMED','MOCK','2026-06-22 10:00:00',  6, 270.0000, 410.4000,0.4000,'COMPLETED','2026-06-30 12:00:00',@now,@now,'ACTIVE');
-- refund: LFTO $929.20 / MANE $858.50-$425-$2.125=$431.375 / DSC $1373.60-$510-$2.55=$861.05

INSERT INTO balance_holds (account_id,subscription_id,amount,hold_status,released_at,settled_at,created_at,updated_at,status) VALUES
    (24,1301, 929.2000,'RELEASED','2026-06-04 21:30:00',NULL,'2026-05-26 10:00:00','2026-06-04 21:30:00','ACTIVE'),
    (24,1302, 858.5000,'RELEASED','2026-02-04 21:30:00',NULL,'2026-01-25 10:00:00','2026-02-04 21:30:00','ACTIVE'),
    (24,1303,1373.6000,'RELEASED','2026-06-25 21:30:00',NULL,'2026-06-15 10:00:00','2026-06-25 21:30:00','ACTIVE'),
    (24,1304, 681.7500,'LOCKED',  NULL,                 NULL,'2026-06-22 10:00:00',@now,                'ACTIVE');

INSERT INTO return_plans (id,user_id,subscription_id,next_ipo_id,total_refund_amount,currency,current_securities_balance,savings_interest_rate,plan_status,executed_at,created_at,updated_at,status) VALUES
    (131,4,1301,NULL, 929.2000,'USD',5000.0000,2.5000,'EXECUTED','2026-06-05 10:00:00','2026-06-04 21:30:00','2026-06-05 10:00:00','ACTIVE'),
    (132,4,1302,NULL, 431.3750,'USD',5000.0000,2.5000,'EXECUTED','2026-02-05 10:00:00','2026-02-04 21:30:00','2026-02-05 10:00:00','ACTIVE'),
    (133,4,1303,NULL, 861.0500,'USD',5000.0000,2.5000,'EXECUTED','2026-06-26 10:00:00','2026-06-25 21:30:00','2026-06-26 10:00:00','ACTIVE');

-- DEP: LFTO 40%=$371 / MANE 30%=$129 / DSC 60%=$516 → Σ=$1,016
INSERT INTO return_plan_allocations (id,return_plan_id,destination_type,destination_account_id,allocation_ratio,allocation_amount,allocation_status,created_at,updated_at,status) VALUES
    (1301,131,'SECURITIES',24, 60.00, 558.2000,'EXECUTED','2026-06-05 10:00:00','2026-06-05 10:00:00','ACTIVE'),
    (1302,131,'DEPOSIT',   26, 40.00, 371.0000,'EXECUTED','2026-06-05 10:00:00','2026-06-05 10:00:00','ACTIVE'),
    (1303,132,'SECURITIES',24, 70.00, 302.3750,'EXECUTED','2026-02-05 10:00:00','2026-02-05 10:00:00','ACTIVE'),
    (1304,132,'DEPOSIT',   26, 30.00, 129.0000,'EXECUTED','2026-02-05 10:00:00','2026-02-05 10:00:00','ACTIVE'),
    (1305,133,'SECURITIES',24, 40.00, 345.0500,'EXECUTED','2026-06-26 10:00:00','2026-06-26 10:00:00','ACTIVE'),
    (1306,133,'DEPOSIT',   26, 60.00, 516.0000,'EXECUTED','2026-06-26 10:00:00','2026-06-26 10:00:00','ACTIVE');

-- CMA: $5000 - ($0+$425+$510+$270) + ($929.20+$431.375+$861.05) - $1,016 = $5,000.625
UPDATE financial_accounts SET balance=5000.6250, reserved_balance=681.7500, updated_at=@now WHERE id=24;
UPDATE financial_accounts SET balance=1016.0000, reserved_balance=  0.0000, updated_at=@now WHERE id=26;


-- =====================================================================
-- ⑤ 도레미 (user_id=5, CMA=27, DEP=29)
--    다양한 배정율 (0~60%) / BLZX+NXTN 둘 다 현재 보유
--
--    EIKN(76)  30주×$18=$540   → 10주(33%) 배정   DEPOSITED
--    CBRS(98)   6주×$185=$1,110→  1주(17%) 배정   DEPOSITED
--    SPCX(89)  15주×$135=$2,025→  0주(낙첨)        DEPOSITED
--    MANE(78)  20주×$17=$340   → 12주(60%) 배정   DEPOSITED
--    BLZX(202) 40주×$22=$880   →  8주(20%) 배정   COMPLETED
--    NXTN(203) 12주×$45=$540   →  3주(25%) 배정   COMPLETED
--
--    잔액: CMA $6,608.31  reserved $1,434.20  DEP $995.00
-- =====================================================================
INSERT INTO ipo_subscriptions (
    id, user_id, ipo_id, securities_account_id,
    requested_shares, offer_price, subscription_amount, currency,
    subscription_status, execution_mode, subscribed_at,
    allocated_shares, allocated_amount, refund_amount, allocation_rate,
    result_status, confirmed_at, created_at, updated_at, status
) VALUES
    (1401,5,76,27, 30,18.0000,  540.0000,'USD','CONFIRMED','MOCK','2026-01-26 10:00:00', 10, 180.0000, 364.5000,0.3333,'DEPOSITED','2026-02-05 21:00:00',@now,@now,'ACTIVE'),
    (1402,5,98,27,  6,185.0000,1110.0000,'USD','CONFIRMED','MOCK','2026-05-05 10:00:00',  1, 185.0000, 935.1750,0.1667,'DEPOSITED','2026-05-14 21:00:00',@now,@now,'ACTIVE'),
    (1403,5,89,27, 15,135.0000,2025.0000,'USD','CONFIRMED','MOCK','2026-06-03 10:00:00',  0,   0.0000,2045.2500,0.0000,'DEPOSITED','2026-06-12 21:00:00',@now,@now,'ACTIVE'),
    (1404,5,78,27, 20,17.0000,  340.0000,'USD','CONFIRMED','MOCK','2026-01-25 10:00:00', 12, 204.0000, 138.3800,0.6000,'DEPOSITED','2026-02-04 21:00:00',@now,@now,'ACTIVE'),
    (1405,5,202,27,40,22.0000,  880.0000,'USD','CONFIRMED','MOCK','2026-06-22 10:00:00',  8, 176.0000, 711.9200,0.2000,'COMPLETED','2026-06-30 12:00:00',@now,@now,'ACTIVE'),
    (1406,5,203,27,12,45.0000,  540.0000,'USD','CONFIRMED','MOCK','2026-06-22 10:00:00',  3, 135.0000, 409.7250,0.2500,'COMPLETED','2026-06-30 12:00:00',@now,@now,'ACTIVE');
-- refund: EIKN $545.40-$180-$0.90=$364.50 / CBRS $1121.10-$185-$0.925=$935.175
--         SPCX $2045.25 / MANE $343.40-$204-$1.02=$138.38
--         BLZX $888.80-$176-$0.88=$711.92 / NXTN $545.40-$135-$0.675=$409.725

INSERT INTO balance_holds (account_id,subscription_id,amount,hold_status,released_at,settled_at,created_at,updated_at,status) VALUES
    (27,1401,  545.4000,'RELEASED','2026-02-05 21:30:00',NULL,'2026-01-26 10:00:00','2026-02-05 21:30:00','ACTIVE'),
    (27,1402, 1121.1000,'RELEASED','2026-05-14 21:30:00',NULL,'2026-05-05 10:00:00','2026-05-14 21:30:00','ACTIVE'),
    (27,1403, 2045.2500,'RELEASED','2026-06-12 21:30:00',NULL,'2026-06-03 10:00:00','2026-06-12 21:30:00','ACTIVE'),
    (27,1404,  343.4000,'RELEASED','2026-02-04 21:30:00',NULL,'2026-01-25 10:00:00','2026-02-04 21:30:00','ACTIVE'),
    (27,1405,  888.8000,'LOCKED',  NULL,                 NULL,'2026-06-22 10:00:00',@now,                'ACTIVE'),
    (27,1406,  545.4000,'LOCKED',  NULL,                 NULL,'2026-06-22 10:00:00',@now,                'ACTIVE');

INSERT INTO return_plans (id,user_id,subscription_id,next_ipo_id,total_refund_amount,currency,current_securities_balance,savings_interest_rate,plan_status,executed_at,created_at,updated_at,status) VALUES
    (141,5,1401,NULL, 364.5000,'USD',5000.0000,2.5000,'EXECUTED','2026-02-06 10:00:00','2026-02-05 21:30:00','2026-02-06 10:00:00','ACTIVE'),
    (142,5,1402,NULL, 935.1750,'USD',5000.0000,2.5000,'EXECUTED','2026-05-15 10:00:00','2026-05-14 21:30:00','2026-05-15 10:00:00','ACTIVE'),
    (143,5,1403,NULL,2045.2500,'USD',5000.0000,2.5000,'EXECUTED','2026-06-13 10:00:00','2026-06-12 21:30:00','2026-06-13 10:00:00','ACTIVE'),
    (144,5,1404,NULL, 138.3800,'USD',5000.0000,2.5000,'EXECUTED','2026-02-05 10:00:00','2026-02-04 21:30:00','2026-02-05 10:00:00','ACTIVE');

-- DEP: EIKN 50%=$182 / CBRS 20%=$187 / SPCX 30%=$613 / MANE 10%=$13 → Σ=$995
INSERT INTO return_plan_allocations (id,return_plan_id,destination_type,destination_account_id,allocation_ratio,allocation_amount,allocation_status,created_at,updated_at,status) VALUES
    (1401,141,'SECURITIES',27, 50.00, 182.5000,'EXECUTED','2026-02-06 10:00:00','2026-02-06 10:00:00','ACTIVE'),
    (1402,141,'DEPOSIT',   29, 50.00, 182.0000,'EXECUTED','2026-02-06 10:00:00','2026-02-06 10:00:00','ACTIVE'),
    (1403,142,'SECURITIES',27, 80.00, 748.1750,'EXECUTED','2026-05-15 10:00:00','2026-05-15 10:00:00','ACTIVE'),
    (1404,142,'DEPOSIT',   29, 20.00, 187.0000,'EXECUTED','2026-05-15 10:00:00','2026-05-15 10:00:00','ACTIVE'),
    (1405,143,'SECURITIES',27, 70.00,1432.2500,'EXECUTED','2026-06-13 10:00:00','2026-06-13 10:00:00','ACTIVE'),
    (1406,143,'DEPOSIT',   29, 30.00, 613.0000,'EXECUTED','2026-06-13 10:00:00','2026-06-13 10:00:00','ACTIVE'),
    (1407,144,'SECURITIES',27, 90.00, 125.3800,'EXECUTED','2026-02-05 10:00:00','2026-02-05 10:00:00','ACTIVE'),
    (1408,144,'DEPOSIT',   29, 10.00,  13.0000,'EXECUTED','2026-02-05 10:00:00','2026-02-05 10:00:00','ACTIVE');

-- CMA: $5000 - ($180+$185+$0+$204+$176+$135) + ($364.50+$935.175+$2045.25+$138.38) - $995 = $6,608.305
UPDATE financial_accounts SET balance=6608.3050, reserved_balance=1434.2000, updated_at=@now WHERE id=27;
UPDATE financial_accounts SET balance= 995.0000, reserved_balance=   0.0000, updated_at=@now WHERE id=29;


-- =====================================================================
-- ⑥ 플리 (user_id=6, CMA=30, DEP=32)
--    신규·소극 투자자 / BLZX 현재 보유
--
--    DSC(63)   30주×$17=$510   → 10주(33%) 배정   DEPOSITED
--    SPCX(89)   6주×$135=$810  →  0주(낙첨)        DEPOSITED
--    BLZX(202) 15주×$22=$330   →  5주(33%) 배정   COMPLETED
--
--    잔액: CMA $5,582.35  reserved $333.30  DEP $300.00
-- =====================================================================
INSERT INTO ipo_subscriptions (
    id, user_id, ipo_id, securities_account_id,
    requested_shares, offer_price, subscription_amount, currency,
    subscription_status, execution_mode, subscribed_at,
    allocated_shares, allocated_amount, refund_amount, allocation_rate,
    result_status, confirmed_at, created_at, updated_at, status
) VALUES
    (1501,6,63,30, 30,17.0000,510.0000,'USD','CONFIRMED','MOCK','2026-06-15 10:00:00', 10,170.0000, 344.2500,0.3333,'DEPOSITED','2026-06-25 21:00:00',@now,@now,'ACTIVE'),
    (1502,6,89,30,  6,135.0000,810.0000,'USD','CONFIRMED','MOCK','2026-06-03 10:00:00',  0,  0.0000, 818.1000,0.0000,'DEPOSITED','2026-06-12 21:00:00',@now,@now,'ACTIVE'),
    (1503,6,202,30,15,22.0000, 330.0000,'USD','CONFIRMED','MOCK','2026-06-22 10:00:00',  5,110.0000, 222.7500,0.3333,'COMPLETED','2026-06-30 12:00:00',@now,@now,'ACTIVE');
-- refund: DSC $515.10-$170-$0.85=$344.25 / SPCX $818.10 / BLZX $333.30-$110-$0.55=$222.75

INSERT INTO balance_holds (account_id,subscription_id,amount,hold_status,released_at,settled_at,created_at,updated_at,status) VALUES
    (30,1501,515.1000,'RELEASED','2026-06-25 21:30:00',NULL,'2026-06-15 10:00:00','2026-06-25 21:30:00','ACTIVE'),
    (30,1502,818.1000,'RELEASED','2026-06-12 21:30:00',NULL,'2026-06-03 10:00:00','2026-06-12 21:30:00','ACTIVE'),
    (30,1503,333.3000,'LOCKED',  NULL,                 NULL,'2026-06-22 10:00:00',@now,                'ACTIVE');

INSERT INTO return_plans (id,user_id,subscription_id,next_ipo_id,total_refund_amount,currency,current_securities_balance,savings_interest_rate,plan_status,executed_at,created_at,updated_at,status) VALUES
    (151,6,1501,NULL,344.2500,'USD',5000.0000,2.5000,'EXECUTED','2026-06-26 10:00:00','2026-06-25 21:30:00','2026-06-26 10:00:00','ACTIVE'),
    (152,6,1502,NULL,818.1000,'USD',5000.0000,2.5000,'EXECUTED','2026-06-13 10:00:00','2026-06-12 21:30:00','2026-06-13 10:00:00','ACTIVE');

-- DEP: DSC 40%=$137 / SPCX 20%=$163 → Σ=$300
INSERT INTO return_plan_allocations (id,return_plan_id,destination_type,destination_account_id,allocation_ratio,allocation_amount,allocation_status,created_at,updated_at,status) VALUES
    (1501,151,'SECURITIES',30, 60.00, 207.2500,'EXECUTED','2026-06-26 10:00:00','2026-06-26 10:00:00','ACTIVE'),
    (1502,151,'DEPOSIT',   32, 40.00, 137.0000,'EXECUTED','2026-06-26 10:00:00','2026-06-26 10:00:00','ACTIVE'),
    (1503,152,'SECURITIES',30, 80.00, 655.1000,'EXECUTED','2026-06-13 10:00:00','2026-06-13 10:00:00','ACTIVE'),
    (1504,152,'DEPOSIT',   32, 20.00, 163.0000,'EXECUTED','2026-06-13 10:00:00','2026-06-13 10:00:00','ACTIVE');

-- CMA: $5000 - ($170+$0+$110) + ($344.25+$818.10) - $300 = $5,582.35
UPDATE financial_accounts SET balance=5582.3500, reserved_balance=333.3000, updated_at=@now WHERE id=30;
UPDATE financial_accounts SET balance= 300.0000, reserved_balance=  0.0000, updated_at=@now WHERE id=32;


-- =====================================================================
-- ⑦ 레이 (user_id=7, CMA=33, DEP=35)
--    분산 투자 / BLZX+NXTN 둘 다 현재 보유
--
--    LFTO(65)  60주×$23=$1,380 → 15주(25%) 배정   DEPOSITED
--    MANE(78)  40주×$17=$680   → 30주(75%) 배정   DEPOSITED
--    EIKN(76)  25주×$18=$450   →  5주(20%) 배정   DEPOSITED
--    BLZX(202) 20주×$22=$440   →  4주(20%) 배정   COMPLETED
--    NXTN(203) 10주×$45=$450   →  2주(20%) 배정   COMPLETED
--
--    잔액: CMA $4,604.38  reserved $898.90  DEP $858.00
-- =====================================================================
INSERT INTO ipo_subscriptions (
    id, user_id, ipo_id, securities_account_id,
    requested_shares, offer_price, subscription_amount, currency,
    subscription_status, execution_mode, subscribed_at,
    allocated_shares, allocated_amount, refund_amount, allocation_rate,
    result_status, confirmed_at, created_at, updated_at, status
) VALUES
    (1601,7,65,33, 60,23.0000,1380.0000,'USD','CONFIRMED','MOCK','2026-05-26 10:00:00', 15, 345.0000,1047.0750,0.2500,'DEPOSITED','2026-06-04 21:00:00',@now,@now,'ACTIVE'),
    (1602,7,78,33, 40,17.0000,  680.0000,'USD','CONFIRMED','MOCK','2026-01-25 10:00:00', 30, 510.0000, 174.2500,0.7500,'DEPOSITED','2026-02-04 21:00:00',@now,@now,'ACTIVE'),
    (1603,7,76,33, 25,18.0000,  450.0000,'USD','CONFIRMED','MOCK','2026-01-26 10:00:00',  5,  90.0000, 364.0500,0.2000,'DEPOSITED','2026-02-05 21:00:00',@now,@now,'ACTIVE'),
    (1604,7,202,33,20,22.0000,  440.0000,'USD','CONFIRMED','MOCK','2026-06-22 10:00:00',  4,  88.0000, 355.9600,0.2000,'COMPLETED','2026-06-30 12:00:00',@now,@now,'ACTIVE'),
    (1605,7,203,33,10,45.0000,  450.0000,'USD','CONFIRMED','MOCK','2026-06-22 10:00:00',  2,  90.0000, 364.0500,0.2000,'COMPLETED','2026-06-30 12:00:00',@now,@now,'ACTIVE');
-- refund: LFTO $1393.80-$345-$1.725=$1047.075 / MANE $686.80-$510-$2.55=$174.25
--         EIKN $454.50-$90-$0.45=$364.05 / BLZX $444.40-$88-$0.44=$355.96 / NXTN $454.50-$90-$0.45=$364.05

INSERT INTO balance_holds (account_id,subscription_id,amount,hold_status,released_at,settled_at,created_at,updated_at,status) VALUES
    (33,1601,1393.8000,'RELEASED','2026-06-04 21:30:00',NULL,'2026-05-26 10:00:00','2026-06-04 21:30:00','ACTIVE'),
    (33,1602,  686.8000,'RELEASED','2026-02-04 21:30:00',NULL,'2026-01-25 10:00:00','2026-02-04 21:30:00','ACTIVE'),
    (33,1603,  454.5000,'RELEASED','2026-02-05 21:30:00',NULL,'2026-01-26 10:00:00','2026-02-05 21:30:00','ACTIVE'),
    (33,1604,  444.4000,'LOCKED',  NULL,                 NULL,'2026-06-22 10:00:00',@now,                'ACTIVE'),
    (33,1605,  454.5000,'LOCKED',  NULL,                 NULL,'2026-06-22 10:00:00',@now,                'ACTIVE');

INSERT INTO return_plans (id,user_id,subscription_id,next_ipo_id,total_refund_amount,currency,current_securities_balance,savings_interest_rate,plan_status,executed_at,created_at,updated_at,status) VALUES
    (161,7,1601,NULL,1047.0750,'USD',5000.0000,2.5000,'EXECUTED','2026-06-05 10:00:00','2026-06-04 21:30:00','2026-06-05 10:00:00','ACTIVE'),
    (162,7,1602,NULL, 174.2500,'USD',5000.0000,2.5000,'EXECUTED','2026-02-05 10:00:00','2026-02-04 21:30:00','2026-02-05 10:00:00','ACTIVE'),
    (163,7,1603,NULL, 364.0500,'USD',5000.0000,2.5000,'EXECUTED','2026-02-06 10:00:00','2026-02-05 21:30:00','2026-02-06 10:00:00','ACTIVE');

-- DEP: LFTO 70%=$732 / MANE 10%=$17 / EIKN 30%=$109 → Σ=$858
INSERT INTO return_plan_allocations (id,return_plan_id,destination_type,destination_account_id,allocation_ratio,allocation_amount,allocation_status,created_at,updated_at,status) VALUES
    (1601,161,'SECURITIES',33, 30.00, 315.0750,'EXECUTED','2026-06-05 10:00:00','2026-06-05 10:00:00','ACTIVE'),
    (1602,161,'DEPOSIT',   35, 70.00, 732.0000,'EXECUTED','2026-06-05 10:00:00','2026-06-05 10:00:00','ACTIVE'),
    (1603,162,'SECURITIES',33, 90.00, 157.2500,'EXECUTED','2026-02-05 10:00:00','2026-02-05 10:00:00','ACTIVE'),
    (1604,162,'DEPOSIT',   35, 10.00,  17.0000,'EXECUTED','2026-02-05 10:00:00','2026-02-05 10:00:00','ACTIVE'),
    (1605,163,'SECURITIES',33, 70.00, 255.0500,'EXECUTED','2026-02-06 10:00:00','2026-02-06 10:00:00','ACTIVE'),
    (1606,163,'DEPOSIT',   35, 30.00, 109.0000,'EXECUTED','2026-02-06 10:00:00','2026-02-06 10:00:00','ACTIVE');

-- CMA: $5000 - ($345+$510+$90+$88+$90) + ($1047.075+$174.25+$364.05) - $858 = $4,604.375
UPDATE financial_accounts SET balance=4604.3750, reserved_balance=898.9000, updated_at=@now WHERE id=33;
UPDATE financial_accounts SET balance= 858.0000, reserved_balance=  0.0000, updated_at=@now WHERE id=35;


-- =====================================================================
-- 확인
-- =====================================================================
SELECT '=== 유저별 청약·리턴플랜 현황 ===' AS '';
SELECT u.id, u.name,
       COUNT(DISTINCT s.id)   AS 청약수,
       COUNT(DISTINCT rp.id)  AS 리턴플랜수,
       SUM(CASE WHEN bh.hold_status='LOCKED' THEN 1 ELSE 0 END) AS LOCKED홀드
FROM users u
LEFT JOIN ipo_subscriptions s  ON s.user_id  = u.id
LEFT JOIN return_plans rp      ON rp.user_id = u.id
LEFT JOIN balance_holds bh ON bh.subscription_id = s.id
WHERE u.id BETWEEN 1 AND 7
GROUP BY u.id, u.name ORDER BY u.id;

SELECT '=== 계좌 잔액 요약 ===' AS '';
SELECT fa.user_id, u.name,
       fa.id AS acct_id, fa.account_type, fa.currency,
       fa.balance, fa.reserved_balance
FROM financial_accounts fa
JOIN users u ON u.id = fa.user_id
WHERE fa.user_id BETWEEN 1 AND 7
  AND fa.currency = 'USD'
ORDER BY fa.user_id, fa.account_type;
