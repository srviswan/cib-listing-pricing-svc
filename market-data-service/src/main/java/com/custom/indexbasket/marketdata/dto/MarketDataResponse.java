package com.custom.indexbasket.marketdata.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Market Data Response DTO - Response containing market data
 * 
 * @author Custom Index Basket Team
 * @version 1.0.0
 */
public class MarketDataResponse {

    private String instrumentId;
    private String symbol;
    private String exchange;
    private String currency;
    private BigDecimal lastPrice;
    private BigDecimal bidPrice;
    private BigDecimal askPrice;
    private BigDecimal openPrice;
    private BigDecimal highPrice;
    private BigDecimal lowPrice;
    private Long volume;
    private BigDecimal changeAmount;
    private BigDecimal changePercentage;
    private LocalDateTime dataTimestamp;
    private String dataSource;
    private String dataQuality;
    
    // Analytics
    private BigDecimal marketCap;
    private BigDecimal peRatio;
    private BigDecimal dividendYield;
    private BigDecimal beta;
    private BigDecimal volatility;
    
    // Historical data
    private List<HistoricalPrice> historicalPrices;

    // Default constructor
    public MarketDataResponse() {}

    // All-args constructor
    public MarketDataResponse(String instrumentId, String symbol, String exchange, String currency,
                             BigDecimal lastPrice, BigDecimal bidPrice, BigDecimal askPrice, 
                             BigDecimal openPrice, BigDecimal highPrice, BigDecimal lowPrice,
                             Long volume, BigDecimal changeAmount, BigDecimal changePercentage, 
                             LocalDateTime dataTimestamp, String dataSource, String dataQuality, 
                             BigDecimal marketCap, BigDecimal peRatio, BigDecimal dividendYield, 
                             BigDecimal beta, BigDecimal volatility, List<HistoricalPrice> historicalPrices) {
        this.instrumentId = instrumentId;
        this.symbol = symbol;
        this.exchange = exchange;
        this.currency = currency;
        this.lastPrice = lastPrice;
        this.bidPrice = bidPrice;
        this.askPrice = askPrice;
        this.openPrice = openPrice;
        this.highPrice = highPrice;
        this.lowPrice = lowPrice;
        this.volume = volume;
        this.changeAmount = changeAmount;
        this.changePercentage = changePercentage;
        this.dataTimestamp = dataTimestamp;
        this.dataSource = dataSource;
        this.dataQuality = dataQuality;
        this.marketCap = marketCap;
        this.peRatio = peRatio;
        this.dividendYield = dividendYield;
        this.beta = beta;
        this.volatility = volatility;
        this.historicalPrices = historicalPrices;
    }

    // Builder pattern
    public static MarketDataResponseBuilder builder() {
        return new MarketDataResponseBuilder();
    }

    public static class MarketDataResponseBuilder {
        private String instrumentId;
        private String symbol;
        private String exchange;
        private String currency;
        private BigDecimal lastPrice;
        private BigDecimal bidPrice;
        private BigDecimal askPrice;
        private BigDecimal openPrice;
        private BigDecimal highPrice;
        private BigDecimal lowPrice;
        private Long volume;
        private BigDecimal changeAmount;
        private BigDecimal changePercentage;
        private LocalDateTime dataTimestamp;
        private String dataSource;
        private String dataQuality;
        private BigDecimal marketCap;
        private BigDecimal peRatio;
        private BigDecimal dividendYield;
        private BigDecimal beta;
        private BigDecimal volatility;
        private List<HistoricalPrice> historicalPrices;

        public MarketDataResponseBuilder instrumentId(String instrumentId) { this.instrumentId = instrumentId; return this; }
        public MarketDataResponseBuilder symbol(String symbol) { this.symbol = symbol; return this; }
        public MarketDataResponseBuilder exchange(String exchange) { this.exchange = exchange; return this; }
        public MarketDataResponseBuilder currency(String currency) { this.currency = currency; return this; }
        public MarketDataResponseBuilder lastPrice(BigDecimal lastPrice) { this.lastPrice = lastPrice; return this; }
        public MarketDataResponseBuilder bidPrice(BigDecimal bidPrice) { this.bidPrice = bidPrice; return this; }
        public MarketDataResponseBuilder askPrice(BigDecimal askPrice) { this.askPrice = askPrice; return this; }
        public MarketDataResponseBuilder openPrice(BigDecimal openPrice) { this.openPrice = openPrice; return this; }
        public MarketDataResponseBuilder highPrice(BigDecimal highPrice) { this.highPrice = highPrice; return this; }
        public MarketDataResponseBuilder lowPrice(BigDecimal lowPrice) { this.lowPrice = lowPrice; return this; }
        public MarketDataResponseBuilder volume(Long volume) { this.volume = volume; return this; }
        public MarketDataResponseBuilder changeAmount(BigDecimal changeAmount) { this.changeAmount = changeAmount; return this; }
        public MarketDataResponseBuilder changePercentage(BigDecimal changePercentage) { this.changePercentage = changePercentage; return this; }
        public MarketDataResponseBuilder dataTimestamp(LocalDateTime dataTimestamp) { this.dataTimestamp = dataTimestamp; return this; }
        public MarketDataResponseBuilder dataSource(String dataSource) { this.dataSource = dataSource; return this; }
        public MarketDataResponseBuilder dataQuality(String dataQuality) { this.dataQuality = dataQuality; return this; }
        public MarketDataResponseBuilder marketCap(BigDecimal marketCap) { this.marketCap = marketCap; return this; }
        public MarketDataResponseBuilder peRatio(BigDecimal peRatio) { this.peRatio = peRatio; return this; }
        public MarketDataResponseBuilder dividendYield(BigDecimal dividendYield) { this.dividendYield = dividendYield; return this; }
        public MarketDataResponseBuilder beta(BigDecimal beta) { this.beta = beta; return this; }
        public MarketDataResponseBuilder volatility(BigDecimal volatility) { this.volatility = volatility; return this; }
        public MarketDataResponseBuilder historicalPrices(List<HistoricalPrice> historicalPrices) { this.historicalPrices = historicalPrices; return this; }

        public MarketDataResponse build() {
            return new MarketDataResponse(instrumentId, symbol, exchange, currency, lastPrice, bidPrice,
                askPrice, openPrice, highPrice, lowPrice, volume, changeAmount, changePercentage, dataTimestamp, dataSource, dataQuality,
                marketCap, peRatio, dividendYield, beta, volatility, historicalPrices);
        }
    }

    // Getters and Setters
    public String getInstrumentId() { return instrumentId; }
    public void setInstrumentId(String instrumentId) { this.instrumentId = instrumentId; }

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

    public BigDecimal getOpenPrice() { return openPrice; }
    public void setOpenPrice(BigDecimal openPrice) { this.openPrice = openPrice; }

    public BigDecimal getHighPrice() { return highPrice; }
    public void setHighPrice(BigDecimal highPrice) { this.highPrice = highPrice; }

    public BigDecimal getLowPrice() { return lowPrice; }
    public void setLowPrice(BigDecimal lowPrice) { this.lowPrice = lowPrice; }

    public Long getVolume() { return volume; }
    public void setVolume(Long volume) { this.volume = volume; }

    public BigDecimal getChangeAmount() { return changeAmount; }
    public void setChangeAmount(BigDecimal changeAmount) { this.changeAmount = changeAmount; }

    public BigDecimal getChangePercentage() { return changePercentage; }
    public void setChangePercentage(BigDecimal changePercentage) { this.changePercentage = changePercentage; }

    public LocalDateTime getDataTimestamp() { return dataTimestamp; }
    public void setDataTimestamp(LocalDateTime dataTimestamp) { this.dataTimestamp = dataTimestamp; }

    public String getDataSource() { return dataSource; }
    public void setDataSource(String dataSource) { this.dataSource = dataSource; }

    public String getDataQuality() { return dataQuality; }
    public void setDataQuality(String dataQuality) { this.dataQuality = dataQuality; }

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

    public List<HistoricalPrice> getHistoricalPrices() { return historicalPrices; }
    public void setHistoricalPrices(List<HistoricalPrice> historicalPrices) { this.historicalPrices = historicalPrices; }

    // HistoricalPrice inner class
    public static class HistoricalPrice {
        private LocalDateTime timestamp;
        private BigDecimal price;
        private Long volume;

        // Default constructor
        public HistoricalPrice() {}

        // All-args constructor
        public HistoricalPrice(LocalDateTime timestamp, BigDecimal price, Long volume) {
            this.timestamp = timestamp;
            this.price = price;
            this.volume = volume;
        }

        // Builder pattern
        public static HistoricalPriceBuilder builder() {
            return new HistoricalPriceBuilder();
        }

        public static class HistoricalPriceBuilder {
            private LocalDateTime timestamp;
            private BigDecimal price;
            private Long volume;

            public HistoricalPriceBuilder timestamp(LocalDateTime timestamp) { this.timestamp = timestamp; return this; }
            public HistoricalPriceBuilder price(BigDecimal price) { this.price = price; return this; }
            public HistoricalPriceBuilder volume(Long volume) { this.volume = volume; return this; }

            public HistoricalPrice build() {
                return new HistoricalPrice(timestamp, price, volume);
            }
        }

        // Getters and Setters
        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

        public BigDecimal getPrice() { return price; }
        public void setPrice(BigDecimal price) { this.price = price; }

        public Long getVolume() { return volume; }
        public void setVolume(Long volume) { this.volume = volume; }
    }
}
