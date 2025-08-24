package com.custom.indexbasket.marketdata.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Market Data Entity - Stores real-time market data for financial instruments
 * 
 * This entity captures:
 * - Current market prices and volumes
 * - Historical price movements
 * - Market indicators and metrics
 * - Time-series data for analytics
 * 
 * @author Custom Index Basket Team
 * @version 1.0.0
 */
@Table("market_data")
public class MarketDataEntity {

    @Id
    private UUID id;

    @Column("instrument_id")
    private String instrumentId;

    @Column("instrument_type")
    private String instrumentType; // STOCK, BOND, ETF, INDEX, etc.

    @Column("symbol")
    private String symbol;

    @Column("exchange")
    private String exchange;

    @Column("currency")
    private String currency;

    @Column("last_price")
    private BigDecimal lastPrice;

    @Column("bid_price")
    private BigDecimal bidPrice;

    @Column("ask_price")
    private BigDecimal askPrice;

    @Column("volume")
    private Long volume;

    @Column("open_price")
    private BigDecimal openPrice;

    @Column("high_price")
    private BigDecimal highPrice;

    @Column("low_price")
    private BigDecimal lowPrice;

    @Column("previous_close")
    private BigDecimal previousClose;

    @Column("change_amount")
    private BigDecimal changeAmount;

    @Column("change_percentage")
    private BigDecimal changePercentage;

    @Column("market_cap")
    private BigDecimal marketCap;

    @Column("pe_ratio")
    private BigDecimal peRatio;

    @Column("dividend_yield")
    private BigDecimal dividendYield;

    @Column("beta")
    private BigDecimal beta;

    @Column("volatility")
    private BigDecimal volatility;

    @Column("data_source")
    private String dataSource;

    @Column("data_quality")
    private String dataQuality; // HIGH, MEDIUM, LOW

    @Column("is_active")
    private Boolean isActive;

    @Column("created_at")
    private LocalDateTime createdAt;

    @Column("updated_at")
    private LocalDateTime updatedAt;

    @Column("data_timestamp")
    private LocalDateTime dataTimestamp;

    @Column("entity_version")
    private Integer entityVersion;

    // Default constructor
    public MarketDataEntity() {}

    // All-args constructor
    public MarketDataEntity(UUID id, String instrumentId, String instrumentType, String symbol, 
                           String exchange, String currency, BigDecimal lastPrice, BigDecimal bidPrice, 
                           BigDecimal askPrice, Long volume, BigDecimal openPrice, BigDecimal highPrice, 
                           BigDecimal lowPrice, BigDecimal previousClose, BigDecimal changeAmount, 
                           BigDecimal changePercentage, BigDecimal marketCap, BigDecimal peRatio, 
                           BigDecimal dividendYield, BigDecimal beta, BigDecimal volatility, 
                           String dataSource, String dataQuality, Boolean isActive, LocalDateTime createdAt, 
                           LocalDateTime updatedAt, LocalDateTime dataTimestamp, Integer entityVersion) {
        this.id = id;
        this.instrumentId = instrumentId;
        this.instrumentType = instrumentType;
        this.symbol = symbol;
        this.exchange = exchange;
        this.currency = currency;
        this.lastPrice = lastPrice;
        this.bidPrice = bidPrice;
        this.askPrice = askPrice;
        this.volume = volume;
        this.openPrice = openPrice;
        this.highPrice = highPrice;
        this.lowPrice = lowPrice;
        this.previousClose = previousClose;
        this.changeAmount = changeAmount;
        this.changePercentage = changePercentage;
        this.marketCap = marketCap;
        this.peRatio = peRatio;
        this.dividendYield = dividendYield;
        this.beta = beta;
        this.volatility = volatility;
        this.dataSource = dataSource;
        this.dataQuality = dataQuality;
        this.isActive = isActive;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.dataTimestamp = dataTimestamp;
        this.entityVersion = entityVersion;
    }

    // Builder pattern
    public static MarketDataEntityBuilder builder() {
        return new MarketDataEntityBuilder();
    }

    public static class MarketDataEntityBuilder {
        private UUID id;
        private String instrumentId;
        private String instrumentType;
        private String symbol;
        private String exchange;
        private String currency;
        private BigDecimal lastPrice;
        private BigDecimal bidPrice;
        private BigDecimal askPrice;
        private Long volume;
        private BigDecimal openPrice;
        private BigDecimal highPrice;
        private BigDecimal lowPrice;
        private BigDecimal previousClose;
        private BigDecimal changeAmount;
        private BigDecimal changePercentage;
        private BigDecimal marketCap;
        private BigDecimal peRatio;
        private BigDecimal dividendYield;
        private BigDecimal beta;
        private BigDecimal volatility;
        private String dataSource;
        private String dataQuality;
        private Boolean isActive;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private LocalDateTime dataTimestamp;
        private Integer entityVersion;

        public MarketDataEntityBuilder id(UUID id) { this.id = id; return this; }
        public MarketDataEntityBuilder instrumentId(String instrumentId) { this.instrumentId = instrumentId; return this; }
        public MarketDataEntityBuilder instrumentType(String instrumentType) { this.instrumentType = instrumentType; return this; }
        public MarketDataEntityBuilder symbol(String symbol) { this.symbol = symbol; return this; }
        public MarketDataEntityBuilder exchange(String exchange) { this.exchange = exchange; return this; }
        public MarketDataEntityBuilder currency(String currency) { this.currency = currency; return this; }
        public MarketDataEntityBuilder lastPrice(BigDecimal lastPrice) { this.lastPrice = lastPrice; return this; }
        public MarketDataEntityBuilder bidPrice(BigDecimal bidPrice) { this.bidPrice = bidPrice; return this; }
        public MarketDataEntityBuilder askPrice(BigDecimal askPrice) { this.askPrice = askPrice; return this; }
        public MarketDataEntityBuilder volume(Long volume) { this.volume = volume; return this; }
        public MarketDataEntityBuilder openPrice(BigDecimal openPrice) { this.openPrice = openPrice; return this; }
        public MarketDataEntityBuilder highPrice(BigDecimal highPrice) { this.highPrice = highPrice; return this; }
        public MarketDataEntityBuilder lowPrice(BigDecimal lowPrice) { this.lowPrice = lowPrice; return this; }
        public MarketDataEntityBuilder previousClose(BigDecimal previousClose) { this.previousClose = previousClose; return this; }
        public MarketDataEntityBuilder changeAmount(BigDecimal changeAmount) { this.changeAmount = changeAmount; return this; }
        public MarketDataEntityBuilder changePercentage(BigDecimal changePercentage) { this.changePercentage = changePercentage; return this; }
        public MarketDataEntityBuilder marketCap(BigDecimal marketCap) { this.marketCap = marketCap; return this; }
        public MarketDataEntityBuilder peRatio(BigDecimal peRatio) { this.peRatio = peRatio; return this; }
        public MarketDataEntityBuilder dividendYield(BigDecimal dividendYield) { this.dividendYield = dividendYield; return this; }
        public MarketDataEntityBuilder beta(BigDecimal beta) { this.beta = beta; return this; }
        public MarketDataEntityBuilder volatility(BigDecimal volatility) { this.volatility = volatility; return this; }
        public MarketDataEntityBuilder dataSource(String dataSource) { this.dataSource = dataSource; return this; }
        public MarketDataEntityBuilder dataQuality(String dataQuality) { this.dataQuality = dataQuality; return this; }
        public MarketDataEntityBuilder isActive(Boolean isActive) { this.isActive = isActive; return this; }
        public MarketDataEntityBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public MarketDataEntityBuilder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }
        public MarketDataEntityBuilder dataTimestamp(LocalDateTime dataTimestamp) { this.dataTimestamp = dataTimestamp; return this; }
        public MarketDataEntityBuilder entityVersion(Integer entityVersion) { this.entityVersion = entityVersion; return this; }

        public MarketDataEntity build() {
            return new MarketDataEntity(id, instrumentId, instrumentType, symbol, exchange, currency, 
                lastPrice, bidPrice, askPrice, volume, openPrice, highPrice, lowPrice, previousClose, 
                changeAmount, changePercentage, marketCap, peRatio, dividendYield, beta, volatility, 
                dataSource, dataQuality, isActive, createdAt, updatedAt, dataTimestamp, entityVersion);
        }
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getInstrumentId() { return instrumentId; }
    public void setInstrumentId(String instrumentId) { this.instrumentId = instrumentId; }

    public String getInstrumentType() { return instrumentType; }
    public void setInstrumentType(String instrumentType) { this.instrumentType = instrumentType; }

    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }

    public String getExchange() { return exchange; }
    public void setExchange(String exchange) { this.exchange = exchange; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public BigDecimal getLastPrice() { return lastPrice; }
    public void setLastPrice(BigDecimal lastPrice) { this.lastPrice = lastPrice; }

    public BigDecimal getBidPrice() { return bidPrice; }
    public void setBidPrice(BigDecimal bidPrice) { this.bidPrice = bidPrice; }

    public BigDecimal getAskPrice() { return askPrice; }
    public void setAskPrice(BigDecimal askPrice) { this.askPrice = askPrice; }

    public Long getVolume() { return volume; }
    public void setVolume(Long volume) { this.volume = volume; }

    public BigDecimal getOpenPrice() { return openPrice; }
    public void setOpenPrice(BigDecimal openPrice) { this.openPrice = openPrice; }

    public BigDecimal getHighPrice() { return highPrice; }
    public void setHighPrice(BigDecimal highPrice) { this.highPrice = highPrice; }

    public BigDecimal getLowPrice() { return lowPrice; }
    public void setLowPrice(BigDecimal lowPrice) { this.lowPrice = lowPrice; }

    public BigDecimal getPreviousClose() { return previousClose; }
    public void setPreviousClose(BigDecimal previousClose) { this.previousClose = previousClose; }

    public BigDecimal getChangeAmount() { return changeAmount; }
    public void setChangeAmount(BigDecimal changeAmount) { this.changeAmount = changeAmount; }

    public BigDecimal getChangePercentage() { return changePercentage; }
    public void setChangePercentage(BigDecimal changePercentage) { this.changePercentage = changePercentage; }

    public BigDecimal getMarketCap() { return marketCap; }
    public void setMarketCap(BigDecimal marketCap) { this.marketCap = marketCap; }

    public BigDecimal getPeRatio() { return peRatio; }
    public void setPeRatio(BigDecimal peRatio) { this.peRatio = peRatio; }

    public BigDecimal getDividendYield() { return dividendYield; }
    public void setDividendYield(BigDecimal dividendYield) { this.dividendYield = dividendYield; }

    public BigDecimal getBeta() { return beta; }
    public void setBeta(BigDecimal beta) { this.beta = beta; }

    public BigDecimal getVolatility() { return volatility; }
    public void setVolatility(BigDecimal volatility) { this.volatility = volatility; }

    public String getDataSource() { return dataSource; }
    public void setDataSource(String dataSource) { this.dataSource = dataSource; }

    public String getDataQuality() { return dataQuality; }
    public void setDataQuality(String dataQuality) { this.dataQuality = dataQuality; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public LocalDateTime getDataTimestamp() { return dataTimestamp; }
    public void setDataTimestamp(LocalDateTime dataTimestamp) { this.dataTimestamp = dataTimestamp; }

    public Integer getEntityVersion() { return entityVersion; }
    public void setEntityVersion(Integer entityVersion) { this.entityVersion = entityVersion; }
}
