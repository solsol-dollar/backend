ALTER TABLE `ipos`
    ADD COLUMN `total_allocable_shares` INT NULL COMMENT '중개사가 주관사로부터 배정받은 총 공모주식수 (운영자 입력값)',
    ADD CONSTRAINT `CK_ipos_total_allocable_shares` CHECK (`total_allocable_shares` IS NULL OR `total_allocable_shares` >= 0);
