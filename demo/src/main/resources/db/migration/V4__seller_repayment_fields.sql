ALTER TABLE house_orders
    ADD COLUMN seller_repay_required BIT NOT NULL DEFAULT 0 AFTER admin_reviewed_at,
    ADD COLUMN seller_repay_amount DECIMAL(18, 2) NOT NULL DEFAULT 0.00 AFTER seller_repay_required,
    ADD COLUMN seller_repay_reference VARCHAR(50) NULL AFTER seller_repay_amount,
    ADD COLUMN seller_repay_description VARCHAR(255) NULL AFTER seller_repay_reference,
    ADD COLUMN seller_repay_settled_at DATETIME NULL AFTER seller_repay_description;
