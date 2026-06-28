CREATE TABLE `merchants` (
    `merchant_name` VARCHAR(100)  NOT NULL COMMENT '가맹점명 (card_transactions.merchant_name 과 매핑)',
    `image_url`     VARCHAR(500)  NULL     COMMENT '가맹점 로고 이미지 URL',
    `created_at`    DATETIME      NOT NULL,
    `updated_at`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`merchant_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `card_transactions` (
    `id`                BIGINT        NOT NULL AUTO_INCREMENT,
    `user_id`           BIGINT        NOT NULL,
    `card_id`           BIGINT        NOT NULL,
    `merchant_name`     VARCHAR(100)  NOT NULL,
    `category`          VARCHAR(50)   NOT NULL,
    `amount`            DECIMAL(18,4) NOT NULL,
    `currency`          VARCHAR(10)   NOT NULL DEFAULT 'USD',
    `transacted_at`     DATETIME      NOT NULL,
    `base_rate_at_time` DECIMAL(18,4) NULL,
    `tts_at_time`       DECIMAL(18,4) NULL,
    `created_at`        DATETIME      NOT NULL,
    `updated_at`        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `status`            VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

ALTER TABLE `card_transactions`
    ADD CONSTRAINT `FK_card_transactions_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
    ADD CONSTRAINT `FK_card_transactions_card` FOREIGN KEY (`card_id`) REFERENCES `cards` (`id`);
