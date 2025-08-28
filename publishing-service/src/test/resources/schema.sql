-- Test Schema for Publishing Service
-- This file is used by H2 database for testing

-- Create publishing_status table if it doesn't exist
CREATE TABLE IF NOT EXISTS publishing_status (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    basket_id VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Create basket_listing table if it doesn't exist
CREATE TABLE IF NOT EXISTS basket_listing (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    basket_id VARCHAR(255) NOT NULL,
    vendor_id VARCHAR(255) NOT NULL,
    listing_status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Create price_publishing table if it doesn't exist
CREATE TABLE IF NOT EXISTS price_publishing (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    basket_id VARCHAR(255) NOT NULL,
    price DECIMAL(19,4) NOT NULL,
    published_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    vendor_id VARCHAR(255) NOT NULL
);
