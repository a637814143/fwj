SET @column_exists := (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'second_hand_houses'
      AND COLUMN_NAME = 'property_certificate_url'
);

SET @drop_sql := IF(@column_exists > 0,
    'ALTER TABLE second_hand_houses DROP COLUMN property_certificate_url;',
    'SELECT 1;'
);

PREPARE stmt FROM @drop_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
