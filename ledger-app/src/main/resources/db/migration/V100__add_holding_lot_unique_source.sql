ALTER TABLE `holding_lots`
    ADD CONSTRAINT `UQ_holding_lots_source` UNIQUE (`source_type`, `source_id`);
