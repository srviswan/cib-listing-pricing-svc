-- Market Data Service Database Schema
-- This script creates the necessary tables for the Market Data Service

-- Enable TimescaleDB extension if not already enabled
CREATE EXTENSION IF NOT EXISTS timescaledb;

-- Market Data Table - Stores real-time market data for financial instruments
CREATE TABLE IF NOT EXISTS market_data (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    instrument_id VARCHAR(100) NOT NULL,
    instrument_type VARCHAR(50) NOT NULL, -- STOCK, BOND, ETF, INDEX, etc.
    symbol VARCHAR(20) NOT NULL,
    exchange VARCHAR(50) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    
    -- Price Data
    last_price DECIMAL(20,6),
    bid_price DECIMAL(20,6),
    ask_price DECIMAL(20,6),
    open_price DECIMAL(20,6),
    high_price DECIMAL(20,6),
    low_price DECIMAL(20,6),
    previous_close DECIMAL(20,6),
    
    -- Volume and Changes
    volume BIGINT,
    change_amount DECIMAL(20,6),
    change_percentage DECIMAL(10,4),
    
    -- Market Metrics
    market_cap DECIMAL(20,2),
    pe_ratio DECIMAL(10,4),
    dividend_yield DECIMAL(10,4),
    beta DECIMAL(10,4),
    volatility DECIMAL(10,4),
    
    -- Data Quality
    data_source VARCHAR(100) NOT NULL,
    data_quality VARCHAR(20) NOT NULL CHECK (data_quality IN ('HIGH', 'MEDIUM', 'LOW')),
    
    -- Metadata
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    data_timestamp TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    entity_version INTEGER DEFAULT 1,
    
    -- Indexes for performance
    CONSTRAINT uk_market_data_instrument UNIQUE (instrument_id),
    CONSTRAINT uk_market_data_symbol_exchange UNIQUE (symbol, exchange)
);

-- Create indexes for better query performance
CREATE INDEX IF NOT EXISTS idx_market_data_symbol ON market_data(symbol);
CREATE INDEX IF NOT EXISTS idx_market_data_exchange ON market_data(exchange);
CREATE INDEX IF NOT EXISTS idx_market_data_instrument_type ON market_data(instrument_type);
CREATE INDEX IF NOT EXISTS idx_market_data_currency ON market_data(currency);
CREATE INDEX IF NOT EXISTS idx_market_data_data_source ON market_data(data_source);
CREATE INDEX IF NOT EXISTS idx_market_data_data_quality ON market_data(data_quality);
CREATE INDEX IF NOT EXISTS idx_market_data_is_active ON market_data(is_active);
CREATE INDEX IF NOT EXISTS idx_market_data_updated_at ON market_data(updated_at);
CREATE INDEX IF NOT EXISTS idx_market_data_data_timestamp ON market_data(data_timestamp);

-- Convert to hypertable for time-series data
SELECT create_hypertable('market_data', 'data_timestamp', if_not_exists => TRUE);

-- Basket Market Data Table - Links baskets with their market data and calculated metrics
CREATE TABLE IF NOT EXISTS basket_market_data (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    basket_id UUID NOT NULL,
    basket_code VARCHAR(100) NOT NULL,
    basket_name VARCHAR(255) NOT NULL,
    
    -- Market Values
    total_market_value DECIMAL(20,2) DEFAULT 0,
    total_weight DECIMAL(10,4) DEFAULT 0,
    constituent_count INTEGER DEFAULT 0,
    
    -- Rebalancing Dates
    last_rebalance_date TIMESTAMP WITH TIME ZONE,
    next_rebalance_date TIMESTAMP WITH TIME ZONE,
    
    -- Base Configuration
    base_currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    
    -- Market Metrics (Weighted Averages)
    market_cap_total DECIMAL(20,2),
    pe_ratio_weighted DECIMAL(10,4),
    dividend_yield_weighted DECIMAL(10,4),
    beta_weighted DECIMAL(10,4),
    volatility_weighted DECIMAL(10,4),
    
    -- Diversification Scores
    sector_diversification_score DECIMAL(5,4),
    geographic_diversification_score DECIMAL(5,4),
    
    -- Overall Scores
    risk_score DECIMAL(5,4),
    performance_score DECIMAL(5,4),
    data_quality_score DECIMAL(5,4),
    
    -- Metadata
    last_updated TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    entity_version INTEGER DEFAULT 1,
    
    -- Constraints
    CONSTRAINT uk_basket_market_data_basket_id UNIQUE (basket_id),
    CONSTRAINT uk_basket_market_data_basket_code UNIQUE (basket_code),
    CONSTRAINT chk_basket_market_data_scores CHECK (
        sector_diversification_score >= 0 AND sector_diversification_score <= 1 AND
        geographic_diversification_score >= 0 AND geographic_diversification_score <= 1 AND
        risk_score >= 0 AND risk_score <= 1 AND
        performance_score >= 0 AND performance_score <= 1 AND
        data_quality_score >= 0 AND data_quality_score <= 1
    )
);

-- Create indexes for basket market data
CREATE INDEX IF NOT EXISTS idx_basket_market_data_basket_code ON basket_market_data(basket_code);
CREATE INDEX IF NOT EXISTS idx_basket_market_data_base_currency ON basket_market_data(base_currency);
CREATE INDEX IF NOT EXISTS idx_basket_market_data_is_active ON basket_market_data(is_active);
CREATE INDEX IF NOT EXISTS idx_basket_market_data_risk_score ON basket_market_data(risk_score);
CREATE INDEX IF NOT EXISTS idx_basket_market_data_performance_score ON basket_market_data(performance_score);
CREATE INDEX IF NOT EXISTS idx_basket_market_data_data_quality_score ON basket_market_data(data_quality_score);
CREATE INDEX IF NOT EXISTS idx_basket_market_data_constituent_count ON basket_market_data(constituent_count);
CREATE INDEX IF NOT EXISTS idx_basket_market_data_market_cap_total ON basket_market_data(market_cap_total);
CREATE INDEX IF NOT EXISTS idx_basket_market_data_sector_diversification ON basket_market_data(sector_diversification_score);
CREATE INDEX IF NOT EXISTS idx_basket_market_data_geographic_diversification ON basket_market_data(geographic_diversification_score);
CREATE INDEX IF NOT EXISTS idx_basket_market_data_next_rebalance ON basket_market_data(next_rebalance_date);
CREATE INDEX IF NOT EXISTS idx_basket_market_data_updated_at ON basket_market_data(updated_at);

-- Market Data History Table - Stores historical market data for analytics
CREATE TABLE IF NOT EXISTS market_data_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    instrument_id VARCHAR(100) NOT NULL,
    symbol VARCHAR(20) NOT NULL,
    exchange VARCHAR(50) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    
    -- Historical Price Data
    open_price DECIMAL(20,6),
    high_price DECIMAL(20,6),
    low_price DECIMAL(20,6),
    close_price DECIMAL(20,6),
    volume BIGINT,
    
    -- Date and Time
    date DATE NOT NULL,
    timestamp TIMESTAMP WITH TIME ZONE NOT NULL,
    
    -- Data Source
    data_source VARCHAR(100) NOT NULL,
    data_quality VARCHAR(20) NOT NULL,
    
    -- Metadata
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    -- Constraints
    CONSTRAINT uk_market_data_history_unique UNIQUE (instrument_id, date, timestamp)
);

-- Create indexes for market data history
CREATE INDEX IF NOT EXISTS idx_market_data_history_instrument_id ON market_data_history(instrument_id);
CREATE INDEX IF NOT EXISTS idx_market_data_history_symbol ON market_data_history(symbol);
CREATE INDEX IF NOT EXISTS idx_market_data_history_date ON market_data_history(date);
CREATE INDEX IF NOT EXISTS idx_market_data_history_timestamp ON market_data_history(timestamp);
CREATE INDEX IF NOT EXISTS idx_market_data_history_data_source ON market_data_history(data_source);

-- Convert to hypertable for time-series historical data
SELECT create_hypertable('market_data_history', 'timestamp', if_not_exists => TRUE);

-- Market Data Quality Metrics Table - Tracks data quality over time
CREATE TABLE IF NOT EXISTS market_data_quality_metrics (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    data_source VARCHAR(100) NOT NULL,
    instrument_type VARCHAR(50) NOT NULL,
    exchange VARCHAR(50) NOT NULL,
    
    -- Quality Metrics
    data_freshness_minutes INTEGER,
    data_completeness_percentage DECIMAL(5,2),
    data_accuracy_score DECIMAL(5,4),
    latency_milliseconds INTEGER,
    
    -- Counts
    total_instruments INTEGER,
    active_instruments INTEGER,
    failed_updates INTEGER,
    
    -- Timestamps
    measurement_timestamp TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    -- Constraints
    CONSTRAINT uk_market_data_quality_unique UNIQUE (data_source, instrument_type, exchange, measurement_timestamp)
);

-- Create indexes for quality metrics
CREATE INDEX IF NOT EXISTS idx_market_data_quality_data_source ON market_data_quality_metrics(data_source);
CREATE INDEX IF NOT EXISTS idx_market_data_quality_instrument_type ON market_data_quality_metrics(instrument_type);
CREATE INDEX IF NOT EXISTS idx_market_data_quality_exchange ON market_data_quality_metrics(exchange);
CREATE INDEX IF NOT EXISTS idx_market_data_quality_timestamp ON market_data_quality_metrics(measurement_timestamp);

-- Convert to hypertable for time-series quality metrics
SELECT create_hypertable('market_data_quality_metrics', 'measurement_timestamp', if_not_exists => TRUE);

-- Insert sample market data for testing
INSERT INTO market_data (
    instrument_id, instrument_type, symbol, exchange, currency,
    last_price, bid_price, ask_price, open_price, high_price, low_price, previous_close,
    volume, change_amount, change_percentage,
    market_cap, pe_ratio, dividend_yield, beta, volatility,
    data_source, data_quality
) VALUES 
    ('AAPL_US', 'STOCK', 'AAPL', 'NASDAQ', 'USD', 
     150.00, 149.95, 150.05, 149.50, 151.00, 149.00, 149.50,
     50000000, 0.50, 0.33,
     2500000000000, 25.5, 0.65, 1.2, 18.5,
     'BLOOMBERG', 'HIGH'),
     
    ('MSFT_US', 'STOCK', 'MSFT', 'NASDAQ', 'USD',
     300.00, 299.90, 300.10, 298.00, 301.00, 297.50, 298.00,
     30000000, 2.00, 0.67,
     2200000000000, 30.2, 0.85, 1.1, 16.8,
     'BLOOMBERG', 'HIGH'),
     
    ('GOOGL_US', 'STOCK', 'GOOGL', 'NASDAQ', 'USD',
     120.00, 119.95, 120.05, 119.00, 121.00, 118.50, 119.00,
     25000000, 1.00, 0.84,
     1500000000000, 28.5, 0.00, 1.3, 22.1,
     'BLOOMBERG', 'HIGH')
ON CONFLICT (instrument_id) DO NOTHING;

-- Insert sample basket market data for testing
INSERT INTO basket_market_data (
    basket_id, basket_code, basket_name, base_currency,
    total_market_value, total_weight, constituent_count,
    sector_diversification_score, geographic_diversification_score,
    risk_score, performance_score, data_quality_score
) VALUES 
    ('58b9ed65-e0ce-49a6-a628-6354d6648680', 'EVENT_TEST_001', 'Event Publishing Test Basket', 'USD',
     1000000.00, 100.00, 3,
     0.85, 0.90,
     0.35, 0.75, 0.95)
ON CONFLICT (basket_id) DO NOTHING;

-- Create views for common queries
CREATE OR REPLACE VIEW market_data_summary AS
SELECT 
    instrument_type,
    exchange,
    currency,
    COUNT(*) as instrument_count,
    AVG(last_price) as avg_last_price,
    AVG(volume) as avg_volume,
    AVG(change_percentage) as avg_change_percentage,
    AVG(beta) as avg_beta,
    AVG(volatility) as avg_volatility,
    COUNT(CASE WHEN data_quality = 'HIGH' THEN 1 END) as high_quality_count,
    COUNT(CASE WHEN data_quality = 'MEDIUM' THEN 1 END) as medium_quality_count,
    COUNT(CASE WHEN data_quality = 'LOW' THEN 1 END) as low_quality_count
FROM market_data 
WHERE is_active = true
GROUP BY instrument_type, exchange, currency;

CREATE OR REPLACE VIEW basket_market_data_summary AS
SELECT 
    base_currency,
    COUNT(*) as basket_count,
    AVG(total_market_value) as avg_market_value,
    AVG(constituent_count) as avg_constituent_count,
    AVG(sector_diversification_score) as avg_sector_diversification,
    AVG(geographic_diversification_score) as avg_geographic_diversification,
    AVG(risk_score) as avg_risk_score,
    AVG(performance_score) as avg_performance_score,
    AVG(data_quality_score) as avg_data_quality
FROM basket_market_data 
WHERE is_active = true
GROUP BY base_currency;

-- Grant permissions to the application user
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO basket_app_user;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO basket_app_user;
GRANT ALL PRIVILEGES ON ALL FUNCTIONS IN SCHEMA public TO basket_app_user;

-- Grant permissions on future tables
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO basket_app_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO basket_app_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON FUNCTIONS TO basket_app_user;
