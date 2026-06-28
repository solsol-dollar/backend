CREATE TABLE `ipo_financials` (
    `id`               BIGINT      NOT NULL AUTO_INCREMENT,
    `ipo_id`           BIGINT      NOT NULL,
    `fiscal_year`      SMALLINT    NOT NULL,
    `revenue`          BIGINT      NULL     COMMENT '매출액 (천 단위, 예: 509991 = $509,991 thousand)',
    `operating_income` BIGINT      NULL     COMMENT '영업이익/손실 (음수 = 손실)',
    `net_income`       BIGINT      NULL     COMMENT '순이익/손실 (음수 = 손실)',
    `currency`         VARCHAR(3)  NOT NULL DEFAULT 'USD',
    `status`           VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    `created_at`       DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`       DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uq_ipo_financial_year` (`ipo_id`, `fiscal_year`),
    CONSTRAINT `fk_ipo_financials_ipo` FOREIGN KEY (`ipo_id`) REFERENCES `ipos` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
