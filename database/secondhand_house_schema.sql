CREATE DATABASE IF NOT EXISTS secondhand_house CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE secondhand_house;

CREATE TABLE IF NOT EXISTS user_accounts (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(100) NOT NULL,
    display_name VARCHAR(100) NOT NULL,
    role VARCHAR(20) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6)
);

INSERT INTO user_accounts (username, password, display_name, role)
VALUES
    ('landlord01', 'owner123', '房东小李', 'LANDLORD'),
    ('buyer01', 'buyer123', '买家小王', 'BUYER'),
    ('admin', 'admin123', '系统管理员', 'ADMIN')
ON DUPLICATE KEY UPDATE
    password = VALUES(password),
    display_name = VALUES(display_name),
    role = VALUES(role);

CREATE TABLE IF NOT EXISTS second_hand_houses (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(255) NOT NULL,
    address VARCHAR(255) NOT NULL,
    price DECIMAL(15,2) NOT NULL,
    area DECIMAL(10,2) NOT NULL,
    description TEXT,
    seller_name VARCHAR(100) NOT NULL,
    contact_number VARCHAR(50) NOT NULL,
    listing_date DATE NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6)
);
