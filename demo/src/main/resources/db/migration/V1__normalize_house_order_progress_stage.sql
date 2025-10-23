DELIMITER $$

CREATE PROCEDURE migrate_house_order_progress_stage()
BEGIN
    DECLARE progress_column_exists INT DEFAULT 0;
    DECLARE legacy_stage_count INT DEFAULT 0;

    SELECT COUNT(*)
    INTO progress_column_exists
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'house_orders'
      AND column_name = 'progress_stage';

    IF progress_column_exists > 0 THEN
        SELECT COUNT(*)
        INTO legacy_stage_count
        FROM house_orders
        WHERE progress_stage IN ('RESERVED', 'VIEWED', 'BALANCE_PAID', 'HANDED_OVER');

        IF legacy_stage_count > 0 THEN
            ALTER TABLE house_orders
                MODIFY COLUMN progress_stage ENUM(
                    'RESERVED','VIEWED','BALANCE_PAID','HANDED_OVER',
                    'DEPOSIT_PAID','VIEWING_SCHEDULED','FEEDBACK_SUBMITTED','HANDOVER_COMPLETED'
                ) NOT NULL DEFAULT 'DEPOSIT_PAID';

            UPDATE house_orders SET progress_stage = 'DEPOSIT_PAID' WHERE progress_stage = 'RESERVED';
            UPDATE house_orders SET progress_stage = 'VIEWING_SCHEDULED' WHERE progress_stage = 'VIEWED';
            UPDATE house_orders SET progress_stage = 'FEEDBACK_SUBMITTED' WHERE progress_stage = 'BALANCE_PAID';
            UPDATE house_orders SET progress_stage = 'HANDOVER_COMPLETED' WHERE progress_stage = 'HANDED_OVER';
        END IF;

        ALTER TABLE house_orders
            MODIFY COLUMN progress_stage ENUM(
                'DEPOSIT_PAID','VIEWING_SCHEDULED','FEEDBACK_SUBMITTED','HANDOVER_COMPLETED'
            ) NOT NULL DEFAULT 'DEPOSIT_PAID';
    END IF;
END$$

DELIMITER ;

CALL migrate_house_order_progress_stage();
DROP PROCEDURE migrate_house_order_progress_stage;
