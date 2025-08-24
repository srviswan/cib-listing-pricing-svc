-- Custom Index Basket Management Platform - Database Initialization
-- This script sets up the TimescaleDB database with all necessary extensions and initial configuration

-- Enable required extensions
CREATE EXTENSION IF NOT EXISTS timescaledb;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS pg_stat_statements;

-- Create database user for application
DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'basket_app_user') THEN
        CREATE ROLE basket_app_user WITH LOGIN PASSWORD 'basket_app_password';
    END IF;
END
$$;

-- Grant necessary permissions
GRANT CONNECT ON DATABASE basket_platform TO basket_app_user;
GRANT USAGE ON SCHEMA public TO basket_app_user;
GRANT CREATE ON SCHEMA public TO basket_app_user;

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

CREATE TYPE approval_type AS ENUM ('SINGLE', 'DUAL', 'WORKFLOW');
CREATE TYPE data_source AS ENUM ('BLOOMBERG', 'REFINITIV', 'CENTRAL_BANK', 'MANUAL');

-- Create application tables
CREATE TABLE baskets (
    basket_code VARCHAR(50) PRIMARY KEY,
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
    
    CONSTRAINT chk_total_weight CHECK (total_weight = 100.00),
    CONSTRAINT chk_basket_code CHECK (basket_code ~ '^[A-Z0-9_]{3,50}$')
);

CREATE TABLE basket_constituents (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    basket_code VARCHAR(50) NOT NULL,
    symbol VARCHAR(20) NOT NULL,
    symbol_name VARCHAR(255),
    weight DECIMAL(8,4) NOT NULL,
    shares BIGINT,
    target_allocation DECIMAL(8,4),
    sector VARCHAR(100),
    country VARCHAR(100),
    currency CHAR(3),
    added_at TIMESTAMPTZ DEFAULT NOW(),
    
    FOREIGN KEY (basket_code) REFERENCES baskets(basket_code) ON DELETE CASCADE,
    UNIQUE(basket_code, symbol),
    
    CONSTRAINT chk_weight CHECK (weight > 0 AND weight <= 100),
    CONSTRAINT chk_symbol CHECK (symbol ~ '^[A-Z0-9.]{1,20}$')
);

CREATE TABLE basket_states (
    basket_code VARCHAR(50) PRIMARY KEY,
    current_state basket_status NOT NULL,
    previous_state basket_status,
    last_transition_at TIMESTAMPTZ NOT NULL,
    transition_count INTEGER DEFAULT 0,
    retry_count INTEGER DEFAULT 0,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    
    FOREIGN KEY (basket_code) REFERENCES baskets(basket_code) ON DELETE CASCADE
);

CREATE TABLE approvals (
    approval_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    basket_code VARCHAR(50) NOT NULL,
    submitted_by VARCHAR(100) NOT NULL,
    submitted_at TIMESTAMPTZ DEFAULT NOW(),
    approver VARCHAR(100),
    approved_at TIMESTAMPTZ,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    comments TEXT,
    approval_type approval_type DEFAULT 'SINGLE',
    
    FOREIGN KEY (basket_code) REFERENCES baskets(basket_code) ON DELETE CASCADE
);

CREATE TABLE backtest_results (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    basket_code VARCHAR(50) NOT NULL,
    backtest_name VARCHAR(255),
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    initial_value DECIMAL(15,4),
    final_value DECIMAL(15,4),
    total_return DECIMAL(8,4),
    annualized_return DECIMAL(8,4),
    volatility DECIMAL(8,4),
    sharpe_ratio DECIMAL(8,4),
    max_drawdown DECIMAL(8,4),
    benchmark_results JSONB,
    detailed_results JSONB,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    
    FOREIGN KEY (basket_code) REFERENCES baskets(basket_code) ON DELETE CASCADE,
    CONSTRAINT chk_dates CHECK (end_date > start_date)
);

CREATE TABLE audit_trail (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    entity_type VARCHAR(50) NOT NULL,
    entity_id VARCHAR(50) NOT NULL,
    action VARCHAR(50) NOT NULL,
    old_values JSONB,
    new_values JSONB,
    changed_by VARCHAR(100) NOT NULL,
    changed_at TIMESTAMPTZ DEFAULT NOW(),
    ip_address INET,
    user_agent TEXT
);

-- Create time-series hypertables
CREATE TABLE stock_prices_daily (
    time TIMESTAMPTZ NOT NULL,
    symbol TEXT NOT NULL,
    exchange TEXT,
    currency CHAR(3),
    sector TEXT,
    open DECIMAL(15,4),
    high DECIMAL(15,4),
    low DECIMAL(15,4),
    close DECIMAL(15,4),
    volume BIGINT,
    adjusted_close DECIMAL(15,4),
    dividend DECIMAL(8,4) DEFAULT 0,
    split_ratio DECIMAL(8,4) DEFAULT 1,
    data_source data_source DEFAULT 'BLOOMBERG',
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Convert to hypertable with 1-month chunks
SELECT create_hypertable('stock_prices_daily', 'time', 
    chunk_time_interval => INTERVAL '1 month');

CREATE TABLE stock_prices_intraday (
    time TIMESTAMPTZ NOT NULL,
    symbol TEXT NOT NULL,
    exchange TEXT,
    currency CHAR(3),
    price DECIMAL(15,4),
    volume INTEGER,
    bid DECIMAL(15,4),
    ask DECIMAL(15,4),
    last_trade_time TIMESTAMPTZ,
    data_source data_source DEFAULT 'BLOOMBERG'
);

-- Convert to hypertable with 1-day chunks for high frequency data
SELECT create_hypertable('stock_prices_intraday', 'time', 
    chunk_time_interval => INTERVAL '1 day');

CREATE TABLE index_values (
    time TIMESTAMPTZ NOT NULL,
    index_code TEXT NOT NULL,
    index_name TEXT,
    currency CHAR(3),
    value DECIMAL(15,4),
    volume BIGINT,
    market_cap DECIMAL(20,2),
    constituent_count INTEGER,
    data_source data_source DEFAULT 'BLOOMBERG'
);

SELECT create_hypertable('index_values', 'time', 
    chunk_time_interval => INTERVAL '1 month');

CREATE TABLE fx_rates (
    time TIMESTAMPTZ NOT NULL,
    base_currency CHAR(3) NOT NULL,
    quote_currency CHAR(3) NOT NULL,
    rate DECIMAL(15,8),
    bid DECIMAL(15,8),
    ask DECIMAL(15,8),
    volatility DECIMAL(8,4),
    data_source data_source DEFAULT 'CENTRAL_BANK'
);

SELECT create_hypertable('fx_rates', 'time', 
    chunk_time_interval => INTERVAL '1 week');

CREATE TABLE basket_valuations (
    time TIMESTAMPTZ NOT NULL,
    basket_code TEXT NOT NULL,
    total_value DECIMAL(15,4),
    base_currency CHAR(3),
    constituent_count INTEGER,
    last_updated TIMESTAMPTZ,
    calculation_source TEXT DEFAULT 'REAL_TIME'
);

SELECT create_hypertable('basket_valuations', 'time', 
    chunk_time_interval => INTERVAL '1 day');

CREATE TABLE backtest_performance (
    time TIMESTAMPTZ NOT NULL,
    backtest_id UUID NOT NULL,
    basket_code TEXT NOT NULL,
    portfolio_value DECIMAL(15,4),
    daily_return DECIMAL(8,6),
    cumulative_return DECIMAL(8,4),
    benchmark_value DECIMAL(15,4),
    benchmark_return DECIMAL(8,6)
);

SELECT create_hypertable('backtest_performance', 'time', 
    chunk_time_interval => INTERVAL '1 month');

-- Create indexes for performance
CREATE INDEX idx_stock_daily_symbol_time ON stock_prices_daily (symbol, time DESC);
CREATE INDEX idx_stock_daily_sector_time ON stock_prices_daily (sector, time DESC) 
    WHERE sector IS NOT NULL;
CREATE INDEX idx_stock_daily_exchange_time ON stock_prices_daily (exchange, time DESC)
    WHERE exchange IS NOT NULL;

CREATE INDEX idx_stock_intraday_symbol_time ON stock_prices_intraday (symbol, time DESC);
CREATE INDEX idx_stock_intraday_price_volume ON stock_prices_intraday (price, volume)
    WHERE volume > 0;

CREATE INDEX idx_index_code_time ON index_values (index_code, time DESC);
CREATE INDEX idx_index_value_time ON index_values (value, time DESC);

CREATE INDEX idx_fx_currency_pair_time ON fx_rates (base_currency, quote_currency, time DESC);

CREATE INDEX idx_basket_valuations_code_time ON basket_valuations (basket_code, time DESC);
CREATE INDEX idx_basket_valuations_value_time ON basket_valuations (total_value, time DESC);

CREATE INDEX idx_backtest_performance_id_time ON backtest_performance (backtest_id, time);
CREATE INDEX idx_backtest_performance_basket_time ON backtest_performance (basket_code, time);

-- Application table indexes
CREATE INDEX idx_baskets_status ON baskets(status);
CREATE INDEX idx_baskets_created_by ON baskets(created_by);
CREATE INDEX idx_baskets_created_at ON baskets(created_at);
CREATE INDEX idx_constituents_symbol ON basket_constituents(symbol);
CREATE INDEX idx_constituents_sector ON basket_constituents(sector);
CREATE INDEX idx_states_current_state ON basket_states(current_state);
CREATE INDEX idx_approvals_status ON approvals(status);
CREATE INDEX idx_approvals_submitted_by ON approvals(submitted_by);
CREATE INDEX idx_backtest_basket_code ON backtest_results(basket_code);
CREATE INDEX idx_audit_entity ON audit_trail(entity_type, entity_id);
CREATE INDEX idx_audit_changed_by ON audit_trail(changed_by);
CREATE INDEX idx_audit_changed_at ON audit_trail(changed_at);

-- Create continuous aggregates for performance
CREATE MATERIALIZED VIEW stock_daily_agg
WITH (timescaledb.continuous) AS
SELECT 
    time_bucket('1 day', time) AS day,
    symbol,
    exchange,
    currency,
    FIRST(price, time) AS open,
    MAX(price) AS high,
    MIN(price) AS low,
    LAST(price, time) AS close,
    SUM(volume) AS volume,
    COUNT(*) AS tick_count,
    AVG(price) AS avg_price,
    STDDEV(price) AS price_volatility
FROM stock_prices_intraday
GROUP BY day, symbol, exchange, currency;

CREATE MATERIALIZED VIEW basket_weekly_performance
WITH (timescaledb.continuous) AS
SELECT 
    time_bucket('1 week', time) AS week,
    basket_code,
    FIRST(total_value, time) AS week_open,
    MAX(total_value) AS week_high,
    MIN(total_value) AS week_low,
    LAST(total_value, time) AS week_close,
    (LAST(total_value, time) - FIRST(total_value, time)) / FIRST(total_value, time) * 100 AS weekly_return,
    AVG(total_value) AS avg_value,
    STDDEV(total_value) AS volatility
FROM basket_valuations
GROUP BY week, basket_code;

CREATE MATERIALIZED VIEW index_monthly_performance
WITH (timescaledb.continuous) AS
SELECT 
    time_bucket('1 month', time) AS month,
    index_code,
    FIRST(value, time) AS month_open,
    MAX(value) AS month_high,
    MIN(value) AS month_low,
    LAST(value, time) AS month_close,
    (LAST(value, time) - FIRST(value, time)) / FIRST(value, time) * 100 AS monthly_return,
    AVG(value) AS avg_value,
    STDDEV(value) AS volatility
FROM index_values
GROUP BY month, index_code;

CREATE MATERIALIZED VIEW fx_hourly_agg
WITH (timescaledb.continuous) AS
SELECT 
    time_bucket('1 hour', time) AS hour,
    base_currency,
    quote_currency,
    FIRST(rate, time) AS hour_open,
    MAX(rate) AS hour_high,
    MIN(rate) AS hour_low,
    LAST(rate, time) AS hour_close,
    AVG(rate) AS avg_rate,
    STDDEV(rate) AS rate_volatility
FROM fx_rates
GROUP BY hour, base_currency, quote_currency;

-- Enable real-time aggregation policies
SELECT add_continuous_aggregate_policy('stock_daily_agg',
    start_offset => INTERVAL '3 days',
    end_offset => INTERVAL '1 hour',
    schedule_interval => INTERVAL '1 hour');

SELECT add_continuous_aggregate_policy('basket_weekly_performance',
    start_offset => INTERVAL '2 weeks',
    end_offset => INTERVAL '1 day',
    schedule_interval => INTERVAL '1 day');

SELECT add_continuous_aggregate_policy('index_monthly_performance',
    start_offset => INTERVAL '2 months',
    end_offset => INTERVAL '1 week',
    schedule_interval => INTERVAL '1 week');

SELECT add_continuous_aggregate_policy('fx_hourly_agg',
    start_offset => INTERVAL '2 days',
    end_offset => INTERVAL '10 minutes',
    schedule_interval => INTERVAL '10 minutes');

-- Compression policies
SELECT add_compression_policy('stock_prices_daily', INTERVAL '7 days');
SELECT add_compression_policy('stock_prices_intraday', INTERVAL '1 day');
SELECT add_compression_policy('index_values', INTERVAL '7 days');
SELECT add_compression_policy('fx_rates', INTERVAL '1 day');
SELECT add_compression_policy('basket_valuations', INTERVAL '1 day');
SELECT add_compression_policy('backtest_performance', INTERVAL '30 days');

-- Retention policies
SELECT add_retention_policy('stock_prices_intraday', INTERVAL '2 years');
SELECT add_retention_policy('basket_valuations', INTERVAL '5 years');
SELECT add_retention_policy('fx_rates', INTERVAL '10 years');
SELECT add_retention_policy('backtest_performance', INTERVAL '1 year');

-- Grant permissions to application user
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA basket_platform TO basket_app_user;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA basket_platform TO basket_app_user;
GRANT ALL PRIVILEGES ON ALL FUNCTIONS IN SCHEMA basket_platform TO basket_app_user;

-- Create sample data for testing
INSERT INTO baskets (basket_code, basket_name, description, basket_type, base_currency, created_by) VALUES
('TECH_LEADERS', 'Technology Leaders', 'Top technology companies by market cap', 'EQUITY', 'USD', 'admin'),
('ESG_FOCUS', 'ESG Focus', 'Environmental, Social, and Governance focused companies', 'EQUITY', 'USD', 'admin'),
('EMERGING_MARKETS', 'Emerging Markets', 'High-growth emerging market equities', 'EQUITY', 'USD', 'admin');

INSERT INTO basket_constituents (basket_code, symbol, symbol_name, weight, sector, country, currency) VALUES
('TECH_LEADERS', 'AAPL', 'Apple Inc.', 25.00, 'Technology', 'USA', 'USD'),
('TECH_LEADERS', 'MSFT', 'Microsoft Corporation', 25.00, 'Technology', 'USA', 'USD'),
('TECH_LEADERS', 'GOOGL', 'Alphabet Inc.', 20.00, 'Technology', 'USA', 'USD'),
('TECH_LEADERS', 'AMZN', 'Amazon.com Inc.', 20.00, 'Consumer Discretionary', 'USA', 'USD'),
('TECH_LEADERS', 'NVDA', 'NVIDIA Corporation', 10.00, 'Technology', 'USA', 'USD');

INSERT INTO basket_states (basket_code, current_state, last_transition_at) VALUES
('TECH_LEADERS', 'DRAFT', NOW()),
('ESG_FOCUS', 'DRAFT', NOW()),
('EMERGING_MARKETS', 'DRAFT', NOW());

-- Create functions for common operations
CREATE OR REPLACE FUNCTION update_basket_state(
    p_basket_code VARCHAR(50),
    p_new_state basket_status,
    p_triggered_by VARCHAR(100)
) RETURNS VOID AS $$
BEGIN
    -- Update basket state
    INSERT INTO basket_states (basket_code, current_state, previous_state, last_transition_at, transition_count)
    VALUES (p_basket_code, p_new_state, 
            (SELECT current_state FROM basket_states WHERE basket_code = p_basket_code),
            NOW(), 1)
    ON CONFLICT (basket_code) DO UPDATE SET
        previous_state = EXCLUDED.previous_state,
        current_state = EXCLUDED.current_state,
        last_transition_at = EXCLUDED.last_transition_at,
        transition_count = basket_states.transition_count + 1,
        updated_at = NOW();
    
    -- Update basket status
    UPDATE baskets SET status = p_new_state, updated_at = NOW() WHERE basket_code = p_basket_code;
    
    -- Audit trail
    INSERT INTO audit_trail (entity_type, entity_id, action, new_values, changed_by)
    VALUES ('BASKET', p_basket_code, 'STATE_CHANGE', 
            jsonb_build_object('new_state', p_new_state, 'triggered_by', p_triggered_by),
            p_triggered_by);
END;
$$ LANGUAGE plpgsql;

-- Grant execute permission
GRANT EXECUTE ON FUNCTION update_basket_state TO basket_app_user;

-- Create view for basket summary
CREATE VIEW basket_summary AS
SELECT 
    b.basket_code,
    b.basket_name,
    b.status,
    b.base_currency,
    b.created_by,
    b.created_at,
    bs.current_state,
    bs.last_transition_at,
    COUNT(bc.symbol) as constituent_count,
    SUM(bc.weight) as total_weight
FROM baskets b
LEFT JOIN basket_states bs ON b.basket_code = bs.basket_code
LEFT JOIN basket_constituents bc ON b.basket_code = bc.basket_code
GROUP BY b.basket_code, b.basket_name, b.status, b.base_currency, b.created_by, b.created_at, bs.current_state, bs.last_transition_at;

GRANT SELECT ON basket_summary TO basket_app_user;

-- Log completion
DO $$
BEGIN
    RAISE NOTICE 'Database initialization completed successfully!';
    RAISE NOTICE 'TimescaleDB extension enabled';
    RAISE NOTICE 'Hypertables created for time-series data';
    RAISE NOTICE 'Continuous aggregates configured';
    RAISE NOTICE 'Sample data inserted';
    RAISE NOTICE 'Application user created: basket_app_user';
END $$;
