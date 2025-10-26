CREATE DATABASE IF NOT EXISTS secondhand_house CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE secondhand_house;

CREATE TABLE IF NOT EXISTS user_accounts (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(100) NOT NULL,
    display_name VARCHAR(100) NOT NULL,
    role ENUM('SELLER', 'BUYER', 'ADMIN', 'LANDLORD') NOT NULL,
    blacklisted TINYINT(1) NOT NULL DEFAULT 0,
    phone_number VARCHAR(30),
    real_name VARCHAR(50),
    id_number VARCHAR(64),
    real_name_verified TINYINT(1) NOT NULL DEFAULT 0,
    real_name_verified_at DATETIME(6),
    reputation_score INT NOT NULL DEFAULT 100,
    reservation_breaches INT NOT NULL DEFAULT 0,
    return_count INT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6)
);

INSERT INTO user_accounts (
    username,
    password,
    display_name,
    role,
    blacklisted,
    phone_number,
    real_name,
    id_number,
    real_name_verified,
    real_name_verified_at,
    reputation_score,
    reservation_breaches,
    return_count
)
VALUES
    ('seller01', 'seller123', '卖家小李', 'SELLER', 0, '13800000001', '李卖家', '110101199001010010', 1, CURRENT_TIMESTAMP(6), 100, 0, 0),
    ('buyer01', 'buyer123', '买家小王', 'BUYER', 0, '13900000002', '王买家', '110101199202023333', 1, CURRENT_TIMESTAMP(6), 100, 0, 0),
    ('admin', 'admin123', '系统管理员', 'ADMIN', 0, NULL, NULL, NULL, 0, NULL, 100, 0, 0)
ON DUPLICATE KEY UPDATE
    password = VALUES(password),
    display_name = VALUES(display_name),
    role = VALUES(role),
    blacklisted = VALUES(blacklisted),
    phone_number = VALUES(phone_number),
    real_name = VALUES(real_name),
    id_number = VALUES(id_number),
    real_name_verified = VALUES(real_name_verified),
    real_name_verified_at = VALUES(real_name_verified_at),
    reputation_score = VALUES(reputation_score),
    reservation_breaches = VALUES(reservation_breaches),
    return_count = VALUES(return_count);

CREATE TABLE IF NOT EXISTS second_hand_houses (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(255) NOT NULL,
    address VARCHAR(255) NOT NULL,
    price DECIMAL(15,2) NOT NULL,
    down_payment DECIMAL(15,2) NOT NULL,
    installment_monthly_payment DECIMAL(15,2) NOT NULL,
    installment_months INT NOT NULL,
    area DECIMAL(10,2) NOT NULL,
    description TEXT,
    seller_username VARCHAR(50) NOT NULL,
    seller_name VARCHAR(100) NOT NULL,
    contact_number VARCHAR(50) NOT NULL,
    listing_date DATE NOT NULL,
    floor INT,
    status ENUM('PENDING_REVIEW', 'APPROVED', 'REJECTED', 'SOLD') NOT NULL DEFAULT 'PENDING_REVIEW',
    reviewed_by VARCHAR(50),
    review_message VARCHAR(255),
    reviewed_at DATETIME(6),
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6)
);

CREATE TABLE IF NOT EXISTS second_hand_house_images (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    house_id BIGINT NOT NULL,
    image_url VARCHAR(500) NOT NULL,
    CONSTRAINT fk_house_image_house FOREIGN KEY (house_id) REFERENCES second_hand_houses(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS second_hand_house_keywords (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    house_id BIGINT NOT NULL,
    keyword VARCHAR(50) NOT NULL,
    CONSTRAINT fk_house_keyword_house FOREIGN KEY (house_id) REFERENCES second_hand_houses(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS user_wallets (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL UNIQUE,
    virtual_port VARCHAR(64) NOT NULL UNIQUE,
    balance DECIMAL(18,2) NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_user_wallet_user FOREIGN KEY (user_id) REFERENCES user_accounts(id)
);

CREATE TABLE IF NOT EXISTS wallet_transactions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    wallet_id BIGINT NOT NULL,
    type ENUM('TOP_UP', 'PAYMENT', 'RECEIVE', 'REFUND') NOT NULL,
    amount DECIMAL(18,2) NOT NULL,
    reference VARCHAR(100),
    description VARCHAR(255),
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_wallet_tx_wallet FOREIGN KEY (wallet_id) REFERENCES user_wallets(id)
);

CREATE TABLE IF NOT EXISTS house_orders (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    house_id BIGINT NOT NULL,
    buyer_id BIGINT NOT NULL,
    seller_id BIGINT NOT NULL,
    amount DECIMAL(18,2) NOT NULL,
    payment_method ENUM('FULL', 'INSTALLMENT') NOT NULL,
    status ENUM('PENDING', 'RESERVED', 'PAID', 'RETURNED', 'CANCELLED') NOT NULL,
    progress_stage ENUM('DEPOSIT_PAID', 'VIEWING_SCHEDULED', 'FEEDBACK_SUBMITTED', 'HANDOVER_COMPLETED', 'FUNDS_RELEASED') NOT NULL DEFAULT 'DEPOSIT_PAID',
    admin_hold_amount DECIMAL(18,2) NOT NULL DEFAULT 0,
    platform_fee DECIMAL(18,2) NOT NULL DEFAULT 0,
    released_amount DECIMAL(18,2) NOT NULL DEFAULT 0,
    funds_released_to ENUM('BUYER', 'SELLER'),
    admin_reviewed TINYINT(1) NOT NULL DEFAULT 0,
    admin_reviewed_by VARCHAR(50),
    admin_reviewed_at DATETIME(6),
    return_reason VARCHAR(255),
    viewing_time DATETIME(6),
    viewing_message VARCHAR(255),
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_house_order_house FOREIGN KEY (house_id) REFERENCES second_hand_houses(id),
    CONSTRAINT fk_house_order_buyer FOREIGN KEY (buyer_id) REFERENCES user_accounts(id),
    CONSTRAINT fk_house_order_seller FOREIGN KEY (seller_id) REFERENCES user_accounts(id)
);

CREATE TABLE IF NOT EXISTS conversations (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    buyer_id BIGINT NOT NULL,
    seller_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_conversation_buyer FOREIGN KEY (buyer_id) REFERENCES user_accounts(id),
    CONSTRAINT fk_conversation_seller FOREIGN KEY (seller_id) REFERENCES user_accounts(id),
    CONSTRAINT uk_conversations_participants UNIQUE (buyer_id, seller_id)
);

CREATE TABLE IF NOT EXISTS conversation_messages (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    conversation_id BIGINT NOT NULL,
    sender_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_conversation_message_conversation FOREIGN KEY (conversation_id) REFERENCES conversations(id) ON DELETE CASCADE,
    CONSTRAINT fk_conversation_message_sender FOREIGN KEY (sender_id) REFERENCES user_accounts(id)
);
