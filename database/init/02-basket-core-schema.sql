-- Basket Core Service Database Schema
-- This script creates the correct table structure for our BasketEntity and BasketConstituentEntity classes

-- Enable required extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS pg_stat_statements;

-- Create application schema
CREATE SCHEMA IF NOT EXISTS basket_platform;

-- Set search path
SET search_path TO basket_platform, public;

-- Create custom types
CREATE TYPE basket_status AS ENUM (
    'DRAFT', 'BACKTESTING', 'BACKTESTED', 'BACKTEST_FAILED',
    'PENDING_APPROVAL', 'APPROVED', 'REJECTED',
    'LISTING', 'LISTED', 'LISTING_FAILED',
    'ACTIVE', 'SUSPENDED', 'DELISTED', 'DELETED'
);

-- Create baskets table (matches BasketEntity)
CREATE TABLE baskets (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    basket_code VARCHAR(50) UNIQUE NOT NULL,
    basket_name VARCHAR(255) NOT NULL,
    description TEXT,
    basket_type VARCHAR(50) NOT NULL,
    base_currency CHAR(3) NOT NULL,
    total_weight DECIMAL(5,2) DEFAULT 100.00,
    status basket_status NOT NULL DEFAULT 'DRAFT',
    version VARCHAR(20) DEFAULT 'v1.0',
    previous_version VARCHAR(20),
    created_by VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    approved_by VARCHAR(100),
    approved_at TIMESTAMPTZ,
    listed_at TIMESTAMPTZ,
    activated_at TIMESTAMPTZ,
    
    -- Service-specific fields
    updated_by VARCHAR(100),
    entity_version BIGINT DEFAULT 1,
    is_active BOOLEAN DEFAULT true,
    last_rebalance_date TIMESTAMPTZ,
    next_rebalance_date TIMESTAMPTZ,
    total_market_value DECIMAL(20,4),
    risk_metrics TEXT, -- JSON string
    performance_metrics TEXT, -- JSON string
    backtest_score DECIMAL(5,4),
    constituent_count INTEGER DEFAULT 0,
    
    CONSTRAINT chk_total_weight CHECK (total_weight = 100.00),
    CONSTRAINT chk_basket_code CHECK (basket_code ~ '^[A-Z0-9_]{3,50}$'),
    CONSTRAINT chk_backtest_score CHECK (backtest_score >= 0 AND backtest_score <= 1)
);

-- Create basket_constituents table (matches BasketConstituentEntity)
CREATE TABLE basket_constituents (
    entity_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    basket_id UUID NOT NULL,
    symbol VARCHAR(20) NOT NULL,
    symbol_name VARCHAR(255),
    weight DECIMAL(8,4) NOT NULL,
    shares BIGINT,
    target_weight DECIMAL(8,4),
    current_price DECIMAL(15,4),
    market_value DECIMAL(20,4),
    sector VARCHAR(100),
    industry VARCHAR(100),
    country VARCHAR(100),
    exchange VARCHAR(50),
    currency CHAR(3),
    isin VARCHAR(12),
    cusip VARCHAR(9),
    sedol VARCHAR(7),
    risk_score DECIMAL(5,4),
    performance_score DECIMAL(5,4),
    last_price_update TIMESTAMPTZ,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    
    FOREIGN KEY (basket_id) REFERENCES baskets(id) ON DELETE CASCADE,
    UNIQUE(basket_id, symbol),
    
    CONSTRAINT chk_weight CHECK (weight > 0 AND weight <= 100),
    CONSTRAINT chk_symbol CHECK (symbol ~ '^[A-Z0-9.]{1,20}$'),
    CONSTRAINT chk_risk_score CHECK (risk_score >= 0 AND risk_score <= 1),
    CONSTRAINT chk_performance_score CHECK (performance_score >= 0 AND performance_score <= 1)
);

-- Create indexes for performance
CREATE INDEX idx_baskets_status ON baskets(status);
CREATE INDEX idx_baskets_created_by ON baskets(created_by);
CREATE INDEX idx_baskets_basket_type ON baskets(basket_type);
CREATE INDEX idx_baskets_created_at ON baskets(created_at);
CREATE INDEX idx_baskets_is_active ON baskets(is_active);

CREATE INDEX idx_constituents_basket_id ON basket_constituents(basket_id);
CREATE INDEX idx_constituents_symbol ON basket_constituents(symbol);
CREATE INDEX idx_constituents_sector ON basket_constituents(sector);
CREATE INDEX idx_constituents_country ON basket_constituents(country);
CREATE INDEX idx_constituents_is_active ON basket_constituents(is_active);

-- Create views for common queries
CREATE VIEW active_baskets AS
SELECT * FROM baskets WHERE is_active = true;

CREATE VIEW basket_summary AS
SELECT 
    b.id,
    b.basket_code,
    b.basket_name,
    b.basket_type,
    b.status,
    b.total_weight,
    b.constituent_count,
    b.created_at,
    b.updated_at
FROM baskets b
WHERE b.is_active = true;

-- Insert some test data
INSERT INTO baskets (
    basket_code, 
    basket_name, 
    description, 
    basket_type, 
    base_currency, 
    total_weight, 
    status, 
    version, 
    created_by, 
    is_active,
    constituent_count
) VALUES 
    ('TEST_BASKET_001', 'Test Technology Basket', 'A test basket for technology stocks', 'EQUITY', 'USD', 100.00, 'DRAFT', 'v1.0', 'testuser', true, 0),
    ('TEST_BASKET_002', 'Test Healthcare Basket', 'A test basket for healthcare stocks', 'EQUITY', 'USD', 100.00, 'DRAFT', 'v1.0', 'testuser', true, 0),
    ('TEST_BASKET_003', 'Test Financial Basket', 'A test basket for financial stocks', 'EQUITY', 'USD', 100.00, 'ACTIVE', 'v1.0', 'testuser', true, 0);

-- Insert test constituents
INSERT INTO basket_constituents (
    basket_id,
    symbol,
    symbol_name,
    weight,
    shares,
    sector,
    country,
    currency,
    is_active
) VALUES 
    ((SELECT id FROM baskets WHERE basket_code = 'TEST_BASKET_001'), 'AAPL', 'Apple Inc.', 25.00, 100, 'Technology', 'US', 'USD', true),
    ((SELECT id FROM baskets WHERE basket_code = 'TEST_BASKET_001'), 'MSFT', 'Microsoft Corp.', 25.00, 50, 'Technology', 'US', 'USD', true),
    ((SELECT id FROM baskets WHERE basket_code = 'TEST_BASKET_001'), 'GOOGL', 'Alphabet Inc.', 25.00, 25, 'Technology', 'US', 'USD', true),
    ((SELECT id FROM baskets WHERE basket_code = 'TEST_BASKET_001'), 'AMZN', 'Amazon.com Inc.', 25.00, 75, 'Technology', 'US', 'USD', true),
    
    ((SELECT id FROM baskets WHERE basket_code = 'TEST_BASKET_002'), 'JNJ', 'Johnson & Johnson', 30.00, 150, 'Healthcare', 'US', 'USD', true),
    ((SELECT id FROM baskets WHERE basket_code = 'TEST_BASKET_002'), 'PFE', 'Pfizer Inc.', 30.00, 200, 'Healthcare', 'US', 'USD', true),
    ((SELECT id FROM baskets WHERE basket_code = 'TEST_BASKET_002'), 'UNH', 'UnitedHealth Group', 40.00, 100, 'Healthcare', 'US', 'USD', true),
    
    ((SELECT id FROM baskets WHERE basket_code = 'TEST_BASKET_003'), 'JPM', 'JPMorgan Chase', 40.00, 100, 'Financial', 'US', 'USD', true),
    ((SELECT id FROM baskets WHERE basket_code = 'TEST_BASKET_003'), 'BAC', 'Bank of America', 30.00, 150, 'Financial', 'US', 'USD', true),
    ((SELECT id FROM baskets WHERE basket_code = 'TEST_BASKET_003'), 'WFC', 'Wells Fargo', 30.00, 120, 'Financial', 'US', 'USD', true);

-- Update constituent counts
UPDATE baskets SET constituent_count = 4 WHERE basket_code = 'TEST_BASKET_001';
UPDATE baskets SET constituent_count = 3 WHERE basket_code = 'TEST_BASKET_002';
UPDATE baskets SET constituent_count = 3 WHERE basket_code = 'TEST_BASKET_003';

-- Grant permissions
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA basket_platform TO basket_app_user;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA basket_platform TO basket_app_user;
GRANT USAGE ON SCHEMA basket_platform TO basket_app_user;
