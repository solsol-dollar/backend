SET @idx_exists = (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'holding_lots'
      AND index_name = 'UQ_holding_lots_source'
);

SET @sql = IF(@idx_exists = 0,
    'ALTER TABLE `holding_lots` ADD CONSTRAINT `UQ_holding_lots_source` UNIQUE (`source_type`, `source_id`)',
    'SELECT 1'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
