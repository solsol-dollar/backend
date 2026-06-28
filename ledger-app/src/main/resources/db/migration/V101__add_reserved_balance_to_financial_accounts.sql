ALTER TABLE `financial_accounts`
    ADD COLUMN `reserved_balance` DECIMAL(18,4) NOT NULL DEFAULT 0
        COMMENT '청약 등으로 잠긴 금액(홀딩). available = balance - reserved_balance';
