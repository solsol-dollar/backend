ALTER TABLE `financial_accounts`
    CHANGE COLUMN `account_number_masked` `account_number` VARCHAR(50) NULL,
    ADD COLUMN `virtual_account_number` VARCHAR(50) NULL
        COMMENT '증권 외화(USD) 계좌에만 사용. 원화 계좌 및 기타 계좌는 NULL';
