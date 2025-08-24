package com.custom.indexbasket.marketdata.proxy.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Represents raw market data received from external data sources.
 * This is the unprocessed data that needs to be transformed into MarketDataResponse.
 */
public class RawMarketData {
    
    private String instrumentId;
    private String dataSource;
    private LocalDateTime timestamp;
    private BigDecimal price;
    private BigDecimal bidPrice;
    private BigDecimal askPrice;
    private BigDecimal openPrice;
    private BigDecimal highPrice;
    private BigDecimal lowPrice;
    private BigDecimal volume;
    private String currency;
    private String exchange;
    private Map<String, Object> additionalFields;
    private String rawData; // Original response from data source
    private Map<String, Object> metadata; // Source-specific metadata

    // Default constructor
    public RawMarketData() {}

    // All-args constructor
    public RawMarketData(String instrumentId, String dataSource, LocalDateTime timestamp, BigDecimal price,
                         BigDecimal bidPrice, BigDecimal askPrice, BigDecimal openPrice, BigDecimal highPrice,
                         BigDecimal lowPrice, BigDecimal volume, String currency, String exchange,
                         Map<String, Object> additionalFields, String rawData, Map<String, Object> metadata) {
        this.instrumentId = instrumentId;
        this.dataSource = dataSource;
        this.timestamp = timestamp;
        this.price = price;
        this.bidPrice = bidPrice;
        this.askPrice = askPrice;
        this.openPrice = openPrice;
        this.highPrice = highPrice;
        this.lowPrice = lowPrice;
        this.volume = volume;
        this.currency = currency;
        this.exchange = exchange;
        this.additionalFields = additionalFields;
        this.rawData = rawData;
        this.metadata = metadata;
    }

    // Builder pattern
    public static RawMarketDataBuilder builder() {
        return new RawMarketDataBuilder();
    }

    public static class RawMarketDataBuilder {
        private String instrumentId;
        private String dataSource;
        private LocalDateTime timestamp;
        private BigDecimal price;
        private BigDecimal bidPrice;
        private BigDecimal askPrice;
        private BigDecimal openPrice;
        private BigDecimal highPrice;
        private BigDecimal lowPrice;
        private BigDecimal volume;
        private String currency;
        private String exchange;
        private Map<String, Object> additionalFields;
        private String rawData;
        private Map<String, Object> metadata;

        public RawMarketDataBuilder instrumentId(String instrumentId) { this.instrumentId = instrumentId; return this; }
        public RawMarketDataBuilder dataSource(String dataSource) { this.dataSource = dataSource; return this; }
        public RawMarketDataBuilder timestamp(LocalDateTime timestamp) { this.timestamp = timestamp; return this; }
        public RawMarketDataBuilder price(BigDecimal price) { this.price = price; return this; }
        public RawMarketDataBuilder bidPrice(BigDecimal bidPrice) { this.bidPrice = bidPrice; return this; }
        public RawMarketDataBuilder askPrice(BigDecimal askPrice) { this.askPrice = askPrice; return this; }
        public RawMarketDataBuilder openPrice(BigDecimal openPrice) { this.openPrice = openPrice; return this; }
        public RawMarketDataBuilder highPrice(BigDecimal highPrice) { this.highPrice = highPrice; return this; }
        public RawMarketDataBuilder lowPrice(BigDecimal lowPrice) { this.lowPrice = lowPrice; return this; }
        public RawMarketDataBuilder volume(BigDecimal volume) { this.volume = volume; return this; }
        public RawMarketDataBuilder currency(String currency) { this.currency = currency; return this; }
        public RawMarketDataBuilder exchange(String exchange) { this.exchange = exchange; return this; }
        public RawMarketDataBuilder additionalFields(Map<String, Object> additionalFields) { this.additionalFields = additionalFields; return this; }
        public RawMarketDataBuilder rawData(String rawData) { this.rawData = rawData; return this; }
        public RawMarketDataBuilder metadata(Map<String, Object> metadata) { this.metadata = metadata; return this; }

        public RawMarketData build() {
            return new RawMarketData(instrumentId, dataSource, timestamp, price, bidPrice, askPrice,
                openPrice, highPrice, lowPrice, volume, currency, exchange, additionalFields, rawData, metadata);
        }
    }

    // Getters and Setters
    public String getInstrumentId() { return instrumentId; }
    public void setInstrumentId(String instrumentId) { this.instrumentId = instrumentId; }

    public String getDataSource() { return dataSource; }
    public void setDataSource(String dataSource) { this.dataSource = dataSource; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

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

    public BigDecimal getVolume() { return volume; }
    public void setVolume(BigDecimal volume) { this.volume = volume; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getExchange() { return exchange; }
    public void setExchange(String exchange) { this.exchange = exchange; }

    public Map<String, Object> getAdditionalFields() { return additionalFields; }
    public void setAdditionalFields(Map<String, Object> additionalFields) { this.additionalFields = additionalFields; }

    public String getRawData() { return rawData; }
    public void setRawData(String rawData) { this.rawData = rawData; }

    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
}
