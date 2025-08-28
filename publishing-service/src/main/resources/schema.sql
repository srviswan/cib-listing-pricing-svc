-- Publishing Service Database Schema
-- This schema should be executed in the existing indexbasket database

-- Publishing Status Table
CREATE TABLE IF NOT EXISTS publishing_status (
    id BIGSERIAL PRIMARY KEY,
    basket_id VARCHAR(50) NOT NULL,
    vendor_name VARCHAR(100) NOT NULL,
    status VARCHAR(50) NOT NULL,
    published_at TIMESTAMP,
    error_message TEXT,
    retry_count INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    operation_type VARCHAR(50), -- LISTING, PRICE_PUBLISHING
    vendor_reference VARCHAR(100), -- Vendor's internal reference
    response_time_ms BIGINT, -- Response time in milliseconds
    request_data TEXT, -- JSON string of request data
    response_data TEXT, -- JSON string of response data
    UNIQUE(basket_id, vendor_name)
);

-- Vendor Health Table
CREATE TABLE IF NOT EXISTS vendor_health (
    id BIGSERIAL PRIMARY KEY,
    vendor_name VARCHAR(100) UNIQUE NOT NULL,
    status VARCHAR(50) NOT NULL,
    last_heartbeat TIMESTAMP,
    response_time_ms INTEGER,
    error_rate DECIMAL(5,4),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    consecutive_failures INTEGER DEFAULT 0,
    circuit_breaker_state VARCHAR(20), -- CLOSED, OPEN, HALF_OPEN
    last_error_message TEXT,
    total_requests INTEGER DEFAULT 0,
    successful_requests INTEGER DEFAULT 0,
    failed_requests INTEGER DEFAULT 0
);

-- Publishing Metrics Table
CREATE TABLE IF NOT EXISTS publishing_metrics (
    id BIGSERIAL PRIMARY KEY,
    basket_id VARCHAR(50) NOT NULL,
    vendor_name VARCHAR(100) NOT NULL,
    operation_type VARCHAR(50) NOT NULL,
    duration_ms INTEGER,
    success BOOLEAN,
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    error_type VARCHAR(50), -- TIMEOUT, CIRCUIT_BREAKER, VENDOR_ERROR, NETWORK_ERROR
    retry_count INTEGER DEFAULT 0,
    vendor_reference VARCHAR(100),
    request_size_bytes BIGINT,
    response_size_bytes BIGINT
);

-- Create indexes for performance
CREATE INDEX IF NOT EXISTS idx_publishing_status_basket ON publishing_status(basket_id);
CREATE INDEX IF NOT EXISTS idx_publishing_status_vendor ON publishing_status(vendor_name);
CREATE INDEX IF NOT EXISTS idx_publishing_status_status ON publishing_status(status);
CREATE INDEX IF NOT EXISTS idx_publishing_status_operation ON publishing_status(operation_type);

CREATE INDEX IF NOT EXISTS idx_vendor_health_name ON vendor_health(vendor_name);
CREATE INDEX IF NOT EXISTS idx_vendor_health_status ON vendor_health(status);
CREATE INDEX IF NOT EXISTS idx_vendor_health_last_heartbeat ON vendor_health(last_heartbeat);

CREATE INDEX IF NOT EXISTS idx_publishing_metrics_basket ON publishing_metrics(basket_id);
CREATE INDEX IF NOT EXISTS idx_publishing_metrics_vendor ON publishing_metrics(vendor_name);
CREATE INDEX IF NOT EXISTS idx_publishing_metrics_operation ON publishing_metrics(operation_type);
CREATE INDEX IF NOT EXISTS idx_publishing_metrics_timestamp ON publishing_metrics(timestamp);

-- Insert initial vendor health records
INSERT INTO vendor_health (vendor_name, status, last_heartbeat, response_time_ms, error_rate, consecutive_failures, circuit_breaker_state, total_requests, successful_requests, failed_requests)
VALUES 
    ('BLOOMBERG', 'HEALTHY', CURRENT_TIMESTAMP, 150, 0.05, 0, 'CLOSED', 0, 0, 0),
    ('REFINITIV', 'HEALTHY', CURRENT_TIMESTAMP, 200, 0.03, 0, 'CLOSED', 0, 0, 0),
    ('GENERIC', 'HEALTHY', CURRENT_TIMESTAMP, 300, 0.10, 0, 'CLOSED', 0, 0, 0)
ON CONFLICT (vendor_name) DO NOTHING;
