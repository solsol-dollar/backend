-- Eclipse DB Schema v1.1.0 (subscription_results 통합)
-- Engine: InnoDB (FK 제약 사용) / Charset: utf8mb4
--
-- [v1.0.2 → v1.1.0 변경 사항]
--  - subscription_results 테이블 제거
--  - ipo_subscriptions 에 배정 결과 필드 통합 (배정 전 NULL)
--  - return_plans.subscription_result_id → subscription_id (FK: ipo_subscriptions)
-- =====================================================================


-- =====================================================================
-- 1. 사용자 / 계좌 / 카드
-- =====================================================================

CREATE TABLE `users` (
	`id`	BIGINT	NOT NULL	AUTO_INCREMENT,
	`name`	VARCHAR(50)	NOT NULL,
	`email`	VARCHAR(100)	NULL	COMMENT 'SSO 연동 시 존재 (현재 NULL 허용 - 정책 확정 후 NOT NULL 검토)',
	`phone_number`	VARCHAR(30)	NULL,
	`onboarding_status`	VARCHAR(30)	NOT NULL	DEFAULT 'REQUIRED',
	`simple_password`	VARCHAR(255)	NULL	COMMENT '간편 비밀번호 BCrypt 해시',
	`created_at`	DATETIME	NOT NULL,
	`updated_at`	DATETIME	NOT NULL	DEFAULT CURRENT_TIMESTAMP,
	`status`	VARCHAR(20)	NOT NULL	DEFAULT 'ACTIVE',
	PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `financial_accounts` (
	`id`	BIGINT	NOT NULL	AUTO_INCREMENT,
	`user_id`	BIGINT	NOT NULL,
	`account_type`	VARCHAR(30)	NOT NULL	COMMENT 'SECURITIES / FX_SAVINGS / FX_ACCOUNT 등',
	`institution_type`	VARCHAR(30)	NOT NULL,
	`institution_name`	VARCHAR(50)	NOT NULL,
	`account_name`	VARCHAR(100)	NULL,
	`account_number_masked`	VARCHAR(50)	NULL,
	`currency`	VARCHAR(10)	NOT NULL	DEFAULT 'USD',
	`balance`	DECIMAL(18,4)	NOT NULL,
	`interest_rate`	DECIMAL(7,4)	NULL,
	`maturity_date`	DATE	NULL,
	`linked`	BOOLEAN	NOT NULL,
	`linked_at`	DATETIME	NULL,
	`created_at`	DATETIME	NOT NULL,
	`updated_at`	DATETIME	NOT NULL	DEFAULT CURRENT_TIMESTAMP,
	`status`	VARCHAR(20)	NOT NULL	DEFAULT 'ACTIVE',
	PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `cards` (
	`id`	BIGINT	NOT NULL	AUTO_INCREMENT,
	`user_id`	BIGINT	NOT NULL,
	`linked_account_id`	BIGINT	NULL,
	`card_type`	VARCHAR(30)	NOT NULL	DEFAULT 'CHECK_CARD',
	`card_status`	VARCHAR(30)	NOT NULL	DEFAULT 'UNISSUED'	COMMENT 'UNISSUED(미발급) / ISSUED(발급·미연동) / LINKED(연동완료)',
	`card_name`	VARCHAR(100)	NULL,
	`card_number_masked`	VARCHAR(50)	NULL,
	`issuer_name`	VARCHAR(50)	NULL,
	`linked`	BOOLEAN	NOT NULL,
	`linked_at`	DATETIME	NULL,
	`created_at`	DATETIME	NOT NULL,
	`updated_at`	DATETIME	NOT NULL	DEFAULT CURRENT_TIMESTAMP,
	`status`	VARCHAR(20)	NOT NULL	DEFAULT 'ACTIVE',
	PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- [신규] 투자성향 진단 (REQ-11) - 이력 보존형 별도 테이블
CREATE TABLE `investment_profiles` (
	`id`	BIGINT	NOT NULL	AUTO_INCREMENT,
	`user_id`	BIGINT	NOT NULL,
	`risk_type`	VARCHAR(30)	NOT NULL	COMMENT 'STABLE / STABLE_SEEKING / NEUTRAL / ACTIVE / AGGRESSIVE (REQ-11-01-02)',
	`total_score`	INT	NOT NULL,
	`investment_experience_score`	INT	NULL	COMMENT '항목별 분석 (REQ-11-01-03)',
	`return_expectation_score`	INT	NULL,
	`loss_tolerance_score`	INT	NULL,
	`recommended_ipo_ratio`	DECIMAL(5,2)	NULL	COMMENT '추천 자산배분 (REQ-11-01-04)',
	`recommended_etf_ratio`	DECIMAL(5,2)	NULL,
	`recommended_savings_ratio`	DECIMAL(5,2)	NULL,
	`recommended_rp_ratio`	DECIMAL(5,2)	NULL,
	`diagnosed_at`	DATETIME	NOT NULL	COMMENT '최신 진단은 diagnosed_at 기준 조회',
	`created_at`	DATETIME	NOT NULL,
	`updated_at`	DATETIME	NOT NULL	DEFAULT CURRENT_TIMESTAMP,
	`status`	VARCHAR(20)	NOT NULL	DEFAULT 'ACTIVE',
	PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- =====================================================================
-- 2. 투자 상품 / IPO
-- =====================================================================

CREATE TABLE `investment_products` (
	`id`	BIGINT	NOT NULL	AUTO_INCREMENT,
	`product_type`	VARCHAR(30)	NOT NULL	COMMENT 'STOCK / ETF / RP 등',
	`ticker`	VARCHAR(20)	NULL,
	`product_name`	VARCHAR(100)	NOT NULL,
	`exchange_name`	VARCHAR(50)	NULL,
	`currency`	VARCHAR(10)	NOT NULL	DEFAULT 'USD',
	`sector`	VARCHAR(100)	NULL,
	`created_at`	DATETIME	NOT NULL,
	`updated_at`	DATETIME	NOT NULL	DEFAULT CURRENT_TIMESTAMP,
	`status`	VARCHAR(20)	NOT NULL	DEFAULT 'ACTIVE',
	PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `ipos` (
	`id`	BIGINT	NOT NULL	AUTO_INCREMENT,
	`product_id`	BIGINT	NULL	COMMENT '상장 후 investment_products 와 연결',
	`ticker`	VARCHAR(20)	NOT NULL,
	`company_name`	VARCHAR(100)	NOT NULL,
	`exchange_name`	VARCHAR(50)	NULL,
	`sector`	VARCHAR(100)	NULL,
	`subscription_start_date`	DATE	NULL,
	`subscription_end_date`	DATE	NULL,
	`listing_date`	DATE	NULL,
	`refund_date`	DATE	NULL,
	`deposit_date`	DATE	NULL,
	`offer_price_min`	DECIMAL(18,4)	NULL,
	`offer_price_max`	DECIMAL(18,4)	NULL,
	`confirmed_offer_price`	DECIMAL(18,4)	NULL,
	`minimum_subscription_amount`	DECIMAL(18,4)	NULL,
	`ipo_status`	VARCHAR(30)	NOT NULL	DEFAULT 'UPCOMING',
	`number_of_shares`	BIGINT	NULL,
	`logo_url`	VARCHAR(500)	NULL,
	`current_price`	DECIMAL(18,4)	NULL,
	`created_at`	DATETIME	NOT NULL,
	`updated_at`	DATETIME	NOT NULL	DEFAULT CURRENT_TIMESTAMP,
	`status`	VARCHAR(20)	NOT NULL	DEFAULT 'ACTIVE',
	PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- [변경] risk_level(VARCHAR) → risk_score(숫자). 등급 문자(A/B/C) 저장 금지.
CREATE TABLE `ipo_risk_scores` (
	`id`	BIGINT	NOT NULL	AUTO_INCREMENT,
	`ipo_id`	BIGINT	NOT NULL,
	`risk_score`	DECIMAL(5,2)	NOT NULL	COMMENT '숫자형 위험 점수 (등급 문자 금지 - 투자자문업 규제 회피, REQ-03-02-01)',
	`summary`	VARCHAR(255)	NULL,
	`reason`	TEXT	NULL,
	`analyzed_at`	DATETIME	NULL,
	`created_at`	DATETIME	NOT NULL,
	`updated_at`	DATETIME	NOT NULL	DEFAULT CURRENT_TIMESTAMP,
	`status`	VARCHAR(20)	NOT NULL	DEFAULT 'ACTIVE',
	PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `ipo_news` (
	`id`	BIGINT	NOT NULL	AUTO_INCREMENT,
	`ipo_id`	BIGINT	NOT NULL,
	`title`	VARCHAR(255)	NOT NULL,
	`source`	VARCHAR(100)	NULL,
	`published_at`	DATETIME	NULL,
	`url`	VARCHAR(500)	NULL,
	`content`	TEXT	NULL	COMMENT 'EODHD 영어 원문',
	`title_ko`	VARCHAR(255)	NULL	COMMENT '한국어 제목',
	`summary`	TEXT	NULL	COMMENT '한국어 요약 (IPO-007 노출)',
	`created_at`	DATETIME	NOT NULL,
	`updated_at`	DATETIME	NOT NULL	DEFAULT CURRENT_TIMESTAMP,
	`status`	VARCHAR(20)	NOT NULL	DEFAULT 'ACTIVE',
	PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `favorite_ipos` (
	`id`	BIGINT	NOT NULL	AUTO_INCREMENT,
	`user_id`	BIGINT	NOT NULL,
	`ipo_id`	BIGINT	NOT NULL,
	`created_at`	DATETIME	NOT NULL,
	`updated_at`	DATETIME	NOT NULL	DEFAULT CURRENT_TIMESTAMP,
	`status`	VARCHAR(20)	NOT NULL	DEFAULT 'ACTIVE',
	PRIMARY KEY (`id`),
	UNIQUE KEY `UQ_favorite_ipos_user_ipo` (`user_id`, `ipo_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- =====================================================================
-- 3. 청약 / 리턴 플랜
--    [v1.1.0] subscription_results 제거 → ipo_subscriptions 통합
-- =====================================================================

-- [변경] 배정 결과 필드 통합 (배정 전 NULL)
CREATE TABLE `ipo_subscriptions` (
	`id`	BIGINT	NOT NULL	AUTO_INCREMENT,
	`user_id`	BIGINT	NOT NULL,
	`ipo_id`	BIGINT	NOT NULL,
	`securities_account_id`	BIGINT	NOT NULL,
	`requested_shares`	INT	NOT NULL,
	`offer_price`	DECIMAL(18,4)	NOT NULL,
	`subscription_amount`	DECIMAL(18,4)	NOT NULL,
	`currency`	VARCHAR(10)	NOT NULL	DEFAULT 'USD',
	`subscription_status`	VARCHAR(30)	NOT NULL	DEFAULT 'REQUESTED',
	`execution_mode`	VARCHAR(20)	NOT NULL	DEFAULT 'MOCK',
	`subscribed_at`	DATETIME	NOT NULL,
	-- 배정 결과 필드 (배정 전 NULL)
	`allocated_shares`    INT NULL COMMENT '배정 전 NULL',
	`allocated_amount`    DECIMAL(18,4) NULL,
	`refund_amount`       DECIMAL(18,4) NULL,
	`allocation_rate`     DECIMAL(7,4) NULL,
	`result_status`       VARCHAR(30) NULL DEFAULT 'PENDING',
	`confirmed_at`        DATETIME NULL,
	`created_at`	DATETIME	NOT NULL,
	`updated_at`	DATETIME	NOT NULL	DEFAULT CURRENT_TIMESTAMP,
	`status`	VARCHAR(20)	NOT NULL	DEFAULT 'ACTIVE',
	PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- [변경] subscription_result_id → subscription_id, FK: ipo_subscriptions
CREATE TABLE `return_plans` (
	`id`	BIGINT	NOT NULL	AUTO_INCREMENT,
	`user_id`	BIGINT	NOT NULL,
	`subscription_id`	BIGINT	NOT NULL,
	`next_ipo_id`	BIGINT	NULL	COMMENT '맥락 정보: 다음 IPO 일정 (REQ-07-00-02)',
	`total_refund_amount`	DECIMAL(18,4)	NOT NULL,
	`currency`	VARCHAR(10)	NOT NULL	DEFAULT 'USD',
	`current_securities_balance`	DECIMAL(18,4)	NULL	COMMENT '리턴 플랜 생성 시점 증권 잔액 스냅샷',
	`savings_interest_rate`	DECIMAL(7,4)	NULL	COMMENT '생성 시점 외화적금 금리 스냅샷',
	`plan_status`	VARCHAR(30)	NOT NULL	DEFAULT 'DRAFT'	COMMENT 'DRAFT / CONFIRMED / EXECUTED',
	`confirmed_at`	DATETIME	NULL,
	`executed_at`	DATETIME	NULL,
	`created_at`	DATETIME	NOT NULL,
	`updated_at`	DATETIME	NOT NULL	DEFAULT CURRENT_TIMESTAMP,
	`status`	VARCHAR(20)	NOT NULL	DEFAULT 'ACTIVE',
	PRIMARY KEY (`id`),
	UNIQUE KEY `UQ_return_plans_subscription` (`subscription_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `return_plan_allocations` (
	`id`	BIGINT	NOT NULL	AUTO_INCREMENT,
	`return_plan_id`	BIGINT	NOT NULL,
	`destination_type`	VARCHAR(30)	NOT NULL	COMMENT 'SECURITIES / FX_SAVINGS / FX_ACCOUNT',
	`destination_account_id`	BIGINT	NULL,
	`allocation_ratio`	DECIMAL(5,2)	NOT NULL,
	`allocation_amount`	DECIMAL(18,4)	NOT NULL,
	`allocation_status`	VARCHAR(30)	NOT NULL	DEFAULT 'PENDING',
	`created_at`	DATETIME	NOT NULL,
	`updated_at`	DATETIME	NOT NULL	DEFAULT CURRENT_TIMESTAMP,
	`status`	VARCHAR(20)	NOT NULL	DEFAULT 'ACTIVE',
	PRIMARY KEY (`id`),
	UNIQUE KEY `UQ_return_plan_allocations_plan_destination` (`return_plan_id`, `destination_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- [신규] 리턴 플랜 프리셋 (REQ-07-04) - DB 관리형
CREATE TABLE `return_plan_presets` (
	`id`	BIGINT	NOT NULL	AUTO_INCREMENT,
	`preset_code`	VARCHAR(30)	NOT NULL	COMMENT 'IPO_FOCUS / STABLE_SAVING / BALANCED',
	`preset_name`	VARCHAR(50)	NOT NULL	COMMENT 'IPO 집중 / 안정 저축 / 균형 배분',
	`securities_ratio`	DECIMAL(5,2)	NOT NULL	COMMENT '외화 증권 계좌 비율',
	`savings_ratio`	DECIMAL(5,2)	NOT NULL	COMMENT '외화 적금 비율',
	`account_ratio`	DECIMAL(5,2)	NOT NULL	COMMENT '외화 통장 비율',
	`description`	VARCHAR(255)	NULL,
	`display_order`	INT	NOT NULL	DEFAULT 0,
	`created_at`	DATETIME	NOT NULL,
	`updated_at`	DATETIME	NOT NULL	DEFAULT CURRENT_TIMESTAMP,
	`status`	VARCHAR(20)	NOT NULL	DEFAULT 'ACTIVE',
	PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- =====================================================================
-- 4. 거래 / 이체 / 환전
-- =====================================================================

CREATE TABLE `transfer_transactions` (
	`id`	BIGINT	NOT NULL	AUTO_INCREMENT,
	`user_id`	BIGINT	NOT NULL,
	`allocation_id`	BIGINT	NULL	COMMENT '리턴 플랜 실행 시 return_plan_allocations 참조',
	`from_account_id`	BIGINT	NULL,
	`to_account_id`	BIGINT	NULL,
	`amount`	DECIMAL(18,4)	NOT NULL,
	`currency`	VARCHAR(10)	NOT NULL	DEFAULT 'USD',
	`transfer_type`	VARCHAR(30)	NOT NULL,
	`transfer_status`	VARCHAR(30)	NOT NULL	DEFAULT 'REQUESTED',
	`execution_mode`	VARCHAR(20)	NOT NULL	DEFAULT 'MOCK',
	`requested_at`	DATETIME	NOT NULL,
	`completed_at`	DATETIME	NULL,
	`failure_reason`	VARCHAR(255)	NULL,
	`created_at`	DATETIME	NOT NULL,
	`updated_at`	DATETIME	NOT NULL	DEFAULT CURRENT_TIMESTAMP,
	`status`	VARCHAR(20)	NOT NULL	DEFAULT 'ACTIVE',
	PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- [신규] 환전 내역 (REQ-05-02-03) - 환율·전후잔액 분리 별도 테이블
CREATE TABLE `fx_exchange_transactions` (
	`id`	BIGINT	NOT NULL	AUTO_INCREMENT,
	`user_id`	BIGINT	NOT NULL,
	`from_account_id`	BIGINT	NULL	COMMENT '원화 출금 계좌 (KRW)',
	`to_account_id`	BIGINT	NULL	COMMENT '외화 입금 계좌 (USD)',
	`from_currency`	VARCHAR(10)	NOT NULL	DEFAULT 'KRW',
	`to_currency`	VARCHAR(10)	NOT NULL	DEFAULT 'USD',
	`exchange_rate`	DECIMAL(18,4)	NOT NULL	COMMENT '적용 환율',
	`source_amount`	DECIMAL(18,4)	NOT NULL	COMMENT '환전 전 금액 (KRW)',
	`target_amount`	DECIMAL(18,4)	NOT NULL	COMMENT '환전 후 금액 (USD)',
	`from_balance_after`	DECIMAL(18,4)	NULL	COMMENT '출금 계좌 잔액 (환전 후)',
	`to_balance_after`	DECIMAL(18,4)	NULL	COMMENT '입금 계좌 잔액 (환전 후)',
	`exchange_status`	VARCHAR(30)	NOT NULL	DEFAULT 'REQUESTED',
	`execution_mode`	VARCHAR(20)	NOT NULL	DEFAULT 'MOCK',
	`requested_at`	DATETIME	NOT NULL,
	`completed_at`	DATETIME	NULL,
	`failure_reason`	VARCHAR(255)	NULL,
	`created_at`	DATETIME	NOT NULL,
	`updated_at`	DATETIME	NOT NULL	DEFAULT CURRENT_TIMESTAMP,
	`status`	VARCHAR(20)	NOT NULL	DEFAULT 'ACTIVE',
	PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `trade_orders` (
	`id`	BIGINT	NOT NULL	AUTO_INCREMENT,
	`user_id`	BIGINT	NOT NULL,
	`product_id`	BIGINT	NOT NULL,
	`account_id`	BIGINT	NOT NULL,
	`order_side`	VARCHAR(20)	NOT NULL,
	`order_type`	VARCHAR(20)	NOT NULL	DEFAULT 'MARKET',
	`quantity`	INT	NOT NULL,
	`requested_price`	DECIMAL(18,4)	NULL,
	`executed_price`	DECIMAL(18,4)	NULL,
	`executed_amount`	DECIMAL(18,4)	NULL,
	`currency`	VARCHAR(10)	NOT NULL	DEFAULT 'USD',
	`order_status`	VARCHAR(30)	NOT NULL	DEFAULT 'COMPLETED',
	`execution_mode`	VARCHAR(20)	NOT NULL	DEFAULT 'MOCK',
	`ordered_at`	DATETIME	NOT NULL,
	`executed_at`	DATETIME	NULL,
	`failure_reason`	VARCHAR(255)	NULL,
	`created_at`	DATETIME	NOT NULL,
	`updated_at`	DATETIME	NOT NULL	DEFAULT CURRENT_TIMESTAMP,
	`status`	VARCHAR(20)	NOT NULL	DEFAULT 'ACTIVE',
	PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `rp_contracts` (
	`id`	BIGINT	NOT NULL	AUTO_INCREMENT,
	`user_id`	BIGINT	NOT NULL,
	`product_id`	BIGINT	NOT NULL,
	`account_id`	BIGINT	NOT NULL,
	`principal_amount`	DECIMAL(18,4)	NOT NULL,
	`interest_rate`	DECIMAL(7,4)	NOT NULL,
	`expected_interest`	DECIMAL(18,4)	NULL,
	`currency`	VARCHAR(10)	NOT NULL	DEFAULT 'USD',
	`started_at`	DATETIME	NOT NULL,
	`maturity_at`	DATETIME	NOT NULL,
	`contract_status`	VARCHAR(30)	NOT NULL	DEFAULT 'ACTIVE',
	`execution_mode`	VARCHAR(20)	NOT NULL	DEFAULT 'MOCK',
	`created_at`	DATETIME	NOT NULL,
	`updated_at`	DATETIME	NOT NULL	DEFAULT CURRENT_TIMESTAMP,
	`status`	VARCHAR(20)	NOT NULL	DEFAULT 'ACTIVE',
	PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- =====================================================================
-- 5. 보유 종목
-- =====================================================================

CREATE TABLE `holdings` (
	`id`	BIGINT	NOT NULL	AUTO_INCREMENT,
	`user_id`	BIGINT	NOT NULL,
	`product_id`	BIGINT	NOT NULL,
	`total_quantity`	INT	NOT NULL,
	`average_price`	DECIMAL(18,4)	NOT NULL,
	`currency`	VARCHAR(10)	NOT NULL	DEFAULT 'USD',
	`created_at`	DATETIME	NOT NULL,
	`updated_at`	DATETIME	NOT NULL	DEFAULT CURRENT_TIMESTAMP,
	`status`	VARCHAR(20)	NOT NULL	DEFAULT 'ACTIVE',
	PRIMARY KEY (`id`),
	UNIQUE KEY `UQ_holdings_user_product` (`user_id`, `product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `holding_lots` (
	`id`	BIGINT	NOT NULL	AUTO_INCREMENT,
	`holding_id`	BIGINT	NOT NULL,
	`user_id`	BIGINT	NOT NULL,
	`product_id`	BIGINT	NOT NULL,
	`source_type`	VARCHAR(30)	NOT NULL	COMMENT 'IPO_ALLOCATION / TRADE_ORDER 등 (폴리모픽)',
	`source_id`	BIGINT	NOT NULL	COMMENT 'source_type 에 따라 참조 대상 상이 - FK 미선언 (폴리모픽)',
	`quantity`	INT	NOT NULL,
	`remaining_quantity`	INT	NOT NULL,
	`acquisition_price`	DECIMAL(18,4)	NOT NULL,
	`acquired_at`	DATETIME	NOT NULL,
	`created_at`	DATETIME	NOT NULL,
	`updated_at`	DATETIME	NOT NULL	DEFAULT CURRENT_TIMESTAMP,
	`status`	VARCHAR(20)	NOT NULL	DEFAULT 'ACTIVE',
	PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- =====================================================================
-- 6. 알림 / 유입(쉬는 달러)
-- =====================================================================

CREATE TABLE `notifications` (
	`id`	BIGINT	NOT NULL	AUTO_INCREMENT,
	`user_id`	BIGINT	NOT NULL,
	`notification_type`	VARCHAR(30)	NOT NULL,
	`title`	VARCHAR(100)	NOT NULL,
	`message`	VARCHAR(500)	NOT NULL,
	`target_type`	VARCHAR(50)	NULL,
	`target_id`	BIGINT	NULL,
	`is_read`	BOOLEAN	NOT NULL,
	`sent_at`	DATETIME	NULL,
	`read_at`	DATETIME	NULL,
	`created_at`	DATETIME	NOT NULL,
	`updated_at`	DATETIME	NOT NULL	DEFAULT CURRENT_TIMESTAMP,
	`status`	VARCHAR(20)	NOT NULL	DEFAULT 'ACTIVE',
	PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- [신규] 쉬는 달러 감지 이력 (REQ-08) - 트리거 발동/억제/무효화 상태 추적
CREATE TABLE `idle_dollar_triggers` (
	`id`	BIGINT	NOT NULL	AUTO_INCREMENT,
	`user_id`	BIGINT	NOT NULL,
	`account_id`	BIGINT	NULL	COMMENT '감지 대상 계좌',
	`idle_balance`	DECIMAL(18,4)	NOT NULL	COMMENT '감지 시점 유휴 잔액',
	`idle_days`	INT	NULL	COMMENT '미사용 경과일',
	`trigger_status`	VARCHAR(30)	NOT NULL	DEFAULT 'TRIGGERED'	COMMENT 'TRIGGERED / SUPPRESSED / INVALIDATED',
	`suppression_reason`	VARCHAR(50)	NULL	COMMENT 'IPO_IMMINENT(관심IPO 14일내) / RECENT_REFUND(환불 7일내)',
	`notification_id`	BIGINT	NULL	COMMENT '발송된 푸시 알림 연결',
	`detected_at`	DATETIME	NOT NULL,
	`notified_at`	DATETIME	NULL,
	`invalidated_at`	DATETIME	NULL	COMMENT '잔액 소진 등으로 무효화된 시점 (REQ-08-91-01)',
	`created_at`	DATETIME	NOT NULL,
	`updated_at`	DATETIME	NOT NULL	DEFAULT CURRENT_TIMESTAMP,
	`status`	VARCHAR(20)	NOT NULL	DEFAULT 'ACTIVE',
	PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- =====================================================================
-- 6-1. 주가 캔들 (차트 데이터)
-- =====================================================================

CREATE TABLE `price_candles` (
    `id`          BIGINT        NOT NULL AUTO_INCREMENT,
    `product_id`  BIGINT        NOT NULL,
    `candle_type` VARCHAR(10)   NOT NULL  COMMENT 'DAY / WEEK / MONTH / YEAR',
    `candle_at`   DATE          NOT NULL  COMMENT '캔들 기준일 (주봉=해당주 마지막 거래일, 월봉=말일)',
    `open_price`  DECIMAL(18,8) NOT NULL,
    `high_price`  DECIMAL(18,8) NOT NULL,
    `low_price`   DECIMAL(18,8) NOT NULL,
    `close_price` DECIMAL(18,8) NOT NULL,
    `volume`      BIGINT,
    `amount`      DECIMAL(24,4),
    `sign`        VARCHAR(1)             COMMENT '등락구분 (2:상승 3:보합 5:하락)',
    `created_at`  DATETIME      NOT NULL,
    `updated_at`  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `status`      VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE',
    PRIMARY KEY (`id`),
    UNIQUE KEY `UQ_candle` (`product_id`, `candle_type`, `candle_at`),
    KEY `IDX_candle_lookup` (`product_id`, `candle_type`, `candle_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- =====================================================================
-- 7. 외래 키 (FK) 제약
--    ON DELETE / ON UPDATE 는 기본(RESTRICT) - 운영 정책 확정 후 조정
-- =====================================================================

-- 계좌 / 카드 / 투자성향
ALTER TABLE `financial_accounts`
	ADD CONSTRAINT `FK_financial_accounts_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`);
ALTER TABLE `cards`
	ADD CONSTRAINT `FK_cards_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
	ADD CONSTRAINT `FK_cards_account` FOREIGN KEY (`linked_account_id`) REFERENCES `financial_accounts` (`id`);
ALTER TABLE `investment_profiles`
	ADD CONSTRAINT `FK_investment_profiles_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`);

-- IPO 관련
ALTER TABLE `ipos`
	ADD CONSTRAINT `FK_ipos_product` FOREIGN KEY (`product_id`) REFERENCES `investment_products` (`id`),
	ADD UNIQUE KEY `uq_ipos_ticker` (`ticker`);
ALTER TABLE `ipo_risk_scores`
	ADD CONSTRAINT `FK_ipo_risk_scores_ipo` FOREIGN KEY (`ipo_id`) REFERENCES `ipos` (`id`);
ALTER TABLE `ipo_news`
	ADD CONSTRAINT `FK_ipo_news_ipo` FOREIGN KEY (`ipo_id`) REFERENCES `ipos` (`id`),
	ADD UNIQUE KEY `uq_ipo_url`  (`ipo_id`, `url`(255)),
	ADD UNIQUE KEY `uq_ipo_news` (`ipo_id`, `title`(191), `published_at`);
ALTER TABLE `favorite_ipos`
	ADD CONSTRAINT `FK_favorite_ipos_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
	ADD CONSTRAINT `FK_favorite_ipos_ipo` FOREIGN KEY (`ipo_id`) REFERENCES `ipos` (`id`);

-- 청약 / 리턴 플랜 (subscription_results 제거됨)
ALTER TABLE `ipo_subscriptions`
	ADD CONSTRAINT `FK_ipo_subscriptions_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
	ADD CONSTRAINT `FK_ipo_subscriptions_ipo` FOREIGN KEY (`ipo_id`) REFERENCES `ipos` (`id`),
	ADD CONSTRAINT `FK_ipo_subscriptions_account` FOREIGN KEY (`securities_account_id`) REFERENCES `financial_accounts` (`id`);
-- [변경] return_plans FK: subscription_results → ipo_subscriptions
ALTER TABLE `return_plans`
	ADD CONSTRAINT `FK_return_plans_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
	ADD CONSTRAINT `FK_return_plans_subscription` FOREIGN KEY (`subscription_id`) REFERENCES `ipo_subscriptions` (`id`),
	ADD CONSTRAINT `FK_return_plans_next_ipo` FOREIGN KEY (`next_ipo_id`) REFERENCES `ipos` (`id`);
ALTER TABLE `return_plan_allocations`
	ADD CONSTRAINT `FK_return_plan_allocations_plan` FOREIGN KEY (`return_plan_id`) REFERENCES `return_plans` (`id`),
	ADD CONSTRAINT `FK_return_plan_allocations_account` FOREIGN KEY (`destination_account_id`) REFERENCES `financial_accounts` (`id`);

-- 거래 / 이체 / 환전
ALTER TABLE `transfer_transactions`
	ADD CONSTRAINT `FK_transfer_transactions_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
	ADD CONSTRAINT `FK_transfer_transactions_allocation` FOREIGN KEY (`allocation_id`) REFERENCES `return_plan_allocations` (`id`),
	ADD CONSTRAINT `FK_transfer_transactions_from` FOREIGN KEY (`from_account_id`) REFERENCES `financial_accounts` (`id`),
	ADD CONSTRAINT `FK_transfer_transactions_to` FOREIGN KEY (`to_account_id`) REFERENCES `financial_accounts` (`id`);
ALTER TABLE `fx_exchange_transactions`
	ADD CONSTRAINT `FK_fx_exchange_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
	ADD CONSTRAINT `FK_fx_exchange_from` FOREIGN KEY (`from_account_id`) REFERENCES `financial_accounts` (`id`),
	ADD CONSTRAINT `FK_fx_exchange_to` FOREIGN KEY (`to_account_id`) REFERENCES `financial_accounts` (`id`);
ALTER TABLE `trade_orders`
	ADD CONSTRAINT `FK_trade_orders_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
	ADD CONSTRAINT `FK_trade_orders_product` FOREIGN KEY (`product_id`) REFERENCES `investment_products` (`id`),
	ADD CONSTRAINT `FK_trade_orders_account` FOREIGN KEY (`account_id`) REFERENCES `financial_accounts` (`id`);
ALTER TABLE `rp_contracts`
	ADD CONSTRAINT `FK_rp_contracts_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
	ADD CONSTRAINT `FK_rp_contracts_product` FOREIGN KEY (`product_id`) REFERENCES `investment_products` (`id`),
	ADD CONSTRAINT `FK_rp_contracts_account` FOREIGN KEY (`account_id`) REFERENCES `financial_accounts` (`id`);

-- 보유 종목
ALTER TABLE `holdings`
	ADD CONSTRAINT `FK_holdings_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
	ADD CONSTRAINT `FK_holdings_product` FOREIGN KEY (`product_id`) REFERENCES `investment_products` (`id`);
ALTER TABLE `holding_lots`
	ADD CONSTRAINT `FK_holding_lots_holding` FOREIGN KEY (`holding_id`) REFERENCES `holdings` (`id`),
	ADD CONSTRAINT `FK_holding_lots_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`);
-- 주의: holding_lots.source_id 는 source_type 에 따라 참조 대상이 달라지는
--       폴리모픽 컬럼이라 FK 를 걸지 않음. 무결성은 애플리케이션 레이어에서 보장.

-- 알림 / 쉬는 달러
ALTER TABLE `notifications`
	ADD CONSTRAINT `FK_notifications_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`);
ALTER TABLE `idle_dollar_triggers`
	ADD CONSTRAINT `FK_idle_dollar_triggers_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
	ADD CONSTRAINT `FK_idle_dollar_triggers_account` FOREIGN KEY (`account_id`) REFERENCES `financial_accounts` (`id`),
	ADD CONSTRAINT `FK_idle_dollar_triggers_notification` FOREIGN KEY (`notification_id`) REFERENCES `notifications` (`id`);
