package com.custom.indexbasket.marketdata.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Basket Market Data Response DTO - Response containing basket market data and analytics
 * 
 * @author Custom Index Basket Team
 * @version 1.0.0
 */
public class BasketMarketDataResponse {

    private String basketId;
    private String basketCode;
    private String basketName;
    private BigDecimal totalMarketValue;
    private BigDecimal totalWeight;
    private Integer constituentCount;
    private String baseCurrency;
    private LocalDateTime lastRebalanceDate;
    private LocalDateTime nextRebalanceDate;
    
    // Market metrics
    private BigDecimal marketCapTotal;
    private BigDecimal peRatioWeighted;
    private BigDecimal dividendYieldWeighted;
    private BigDecimal betaWeighted;
    private BigDecimal volatilityWeighted;
    
    // Diversification scores
    private BigDecimal sectorDiversificationScore;
    private BigDecimal geographicDiversificationScore;
    
    // Overall scores
    private BigDecimal riskScore;
    private BigDecimal performanceScore;
    private BigDecimal dataQualityScore;
    
    // Constituent data
    private List<ConstituentMarketData> constituents;
    
    // Performance metrics
    private List<PerformanceMetric> performanceMetrics;

    // Default constructor
    public BasketMarketDataResponse() {}

    // All-args constructor
    public BasketMarketDataResponse(String basketId, String basketCode, String basketName,
                                   BigDecimal totalMarketValue, BigDecimal totalWeight, Integer constituentCount,
                                   String baseCurrency, LocalDateTime lastRebalanceDate, LocalDateTime nextRebalanceDate,
                                   BigDecimal marketCapTotal, BigDecimal peRatioWeighted, BigDecimal dividendYieldWeighted,
                                   BigDecimal betaWeighted, BigDecimal volatilityWeighted,
                                   BigDecimal sectorDiversificationScore, BigDecimal geographicDiversificationScore,
                                   BigDecimal riskScore, BigDecimal performanceScore, BigDecimal dataQualityScore,
                                   List<ConstituentMarketData> constituents, List<PerformanceMetric> performanceMetrics) {
        this.basketId = basketId;
        this.basketCode = basketCode;
        this.basketName = basketName;
        this.totalMarketValue = totalMarketValue;
        this.totalWeight = totalWeight;
        this.constituentCount = constituentCount;
        this.baseCurrency = baseCurrency;
        this.lastRebalanceDate = lastRebalanceDate;
        this.nextRebalanceDate = nextRebalanceDate;
        this.marketCapTotal = marketCapTotal;
        this.peRatioWeighted = peRatioWeighted;
        this.dividendYieldWeighted = dividendYieldWeighted;
        this.betaWeighted = betaWeighted;
        this.volatilityWeighted = volatilityWeighted;
        this.sectorDiversificationScore = sectorDiversificationScore;
        this.geographicDiversificationScore = geographicDiversificationScore;
        this.riskScore = riskScore;
        this.performanceScore = performanceScore;
        this.dataQualityScore = dataQualityScore;
        this.constituents = constituents;
        this.performanceMetrics = performanceMetrics;
    }

    // Builder pattern
    public static BasketMarketDataResponseBuilder builder() {
        return new BasketMarketDataResponseBuilder();
    }

    public static class BasketMarketDataResponseBuilder {
        private String basketId;
        private String basketCode;
        private String basketName;
        private BigDecimal totalMarketValue;
        private BigDecimal totalWeight;
        private Integer constituentCount;
        private String baseCurrency;
        private LocalDateTime lastRebalanceDate;
        private LocalDateTime nextRebalanceDate;
        private BigDecimal marketCapTotal;
        private BigDecimal peRatioWeighted;
        private BigDecimal dividendYieldWeighted;
        private BigDecimal betaWeighted;
        private BigDecimal volatilityWeighted;
        private BigDecimal sectorDiversificationScore;
        private BigDecimal geographicDiversificationScore;
        private BigDecimal riskScore;
        private BigDecimal performanceScore;
        private BigDecimal dataQualityScore;
        private List<ConstituentMarketData> constituents;
        private List<PerformanceMetric> performanceMetrics;

        public BasketMarketDataResponseBuilder basketId(String basketId) { this.basketId = basketId; return this; }
        public BasketMarketDataResponseBuilder basketCode(String basketCode) { this.basketCode = basketCode; return this; }
        public BasketMarketDataResponseBuilder basketName(String basketName) { this.basketName = basketName; return this; }
        public BasketMarketDataResponseBuilder totalMarketValue(BigDecimal totalMarketValue) { this.totalMarketValue = totalMarketValue; return this; }
        public BasketMarketDataResponseBuilder totalWeight(BigDecimal totalWeight) { this.totalWeight = totalWeight; return this; }
        public BasketMarketDataResponseBuilder constituentCount(Integer constituentCount) { this.constituentCount = constituentCount; return this; }
        public BasketMarketDataResponseBuilder baseCurrency(String baseCurrency) { this.baseCurrency = baseCurrency; return this; }
        public BasketMarketDataResponseBuilder lastRebalanceDate(LocalDateTime lastRebalanceDate) { this.lastRebalanceDate = lastRebalanceDate; return this; }
        public BasketMarketDataResponseBuilder nextRebalanceDate(LocalDateTime nextRebalanceDate) { this.nextRebalanceDate = nextRebalanceDate; return this; }
        public BasketMarketDataResponseBuilder marketCapTotal(BigDecimal marketCapTotal) { this.marketCapTotal = marketCapTotal; return this; }
        public BasketMarketDataResponseBuilder peRatioWeighted(BigDecimal peRatioWeighted) { this.peRatioWeighted = peRatioWeighted; return this; }
        public BasketMarketDataResponseBuilder dividendYieldWeighted(BigDecimal dividendYieldWeighted) { this.dividendYieldWeighted = dividendYieldWeighted; return this; }
        public BasketMarketDataResponseBuilder betaWeighted(BigDecimal betaWeighted) { this.betaWeighted = betaWeighted; return this; }
        public BasketMarketDataResponseBuilder volatilityWeighted(BigDecimal volatilityWeighted) { this.volatilityWeighted = volatilityWeighted; return this; }
        public BasketMarketDataResponseBuilder sectorDiversificationScore(BigDecimal sectorDiversificationScore) { this.sectorDiversificationScore = sectorDiversificationScore; return this; }
        public BasketMarketDataResponseBuilder geographicDiversificationScore(BigDecimal geographicDiversificationScore) { this.geographicDiversificationScore = geographicDiversificationScore; return this; }
        public BasketMarketDataResponseBuilder riskScore(BigDecimal riskScore) { this.riskScore = riskScore; return this; }
        public BasketMarketDataResponseBuilder performanceScore(BigDecimal performanceScore) { this.performanceScore = performanceScore; return this; }
        public BasketMarketDataResponseBuilder dataQualityScore(BigDecimal dataQualityScore) { this.dataQualityScore = dataQualityScore; return this; }
        public BasketMarketDataResponseBuilder constituents(List<ConstituentMarketData> constituents) { this.constituents = constituents; return this; }
        public BasketMarketDataResponseBuilder performanceMetrics(List<PerformanceMetric> performanceMetrics) { this.performanceMetrics = performanceMetrics; return this; }

        public BasketMarketDataResponse build() {
            return new BasketMarketDataResponse(basketId, basketCode, basketName, totalMarketValue, totalWeight,
                constituentCount, baseCurrency, lastRebalanceDate, nextRebalanceDate, marketCapTotal,
                peRatioWeighted, dividendYieldWeighted, betaWeighted, volatilityWeighted,
                sectorDiversificationScore, geographicDiversificationScore, riskScore, performanceScore,
                dataQualityScore, constituents, performanceMetrics);
        }
    }

    // Getters and Setters
    public String getBasketId() { return basketId; }
    public void setBasketId(String basketId) { this.basketId = basketId; }

    public String getBasketCode() { return basketCode; }
    public void setBasketCode(String basketCode) { this.basketCode = basketCode; }

    public String getBasketName() { return basketName; }
    public void setBasketName(String basketName) { this.basketName = basketName; }

    public BigDecimal getTotalMarketValue() { return totalMarketValue; }
    public void setTotalMarketValue(BigDecimal totalMarketValue) { this.totalMarketValue = totalMarketValue; }

    public BigDecimal getTotalWeight() { return totalWeight; }
    public void setTotalWeight(BigDecimal totalWeight) { this.totalWeight = totalWeight; }

    public Integer getConstituentCount() { return constituentCount; }
    public void setConstituentCount(Integer constituentCount) { this.constituentCount = constituentCount; }

    public String getBaseCurrency() { return baseCurrency; }
    public void setBaseCurrency(String baseCurrency) { this.baseCurrency = baseCurrency; }

    public LocalDateTime getLastRebalanceDate() { return lastRebalanceDate; }
    public void setLastRebalanceDate(LocalDateTime lastRebalanceDate) { this.lastRebalanceDate = lastRebalanceDate; }

    public LocalDateTime getNextRebalanceDate() { return nextRebalanceDate; }
    public void setNextRebalanceDate(LocalDateTime nextRebalanceDate) { this.nextRebalanceDate = nextRebalanceDate; }

    public BigDecimal getMarketCapTotal() { return marketCapTotal; }
    public void setMarketCapTotal(BigDecimal marketCapTotal) { this.marketCapTotal = marketCapTotal; }

    public BigDecimal getPeRatioWeighted() { return peRatioWeighted; }
    public void setPeRatioWeighted(BigDecimal peRatioWeighted) { this.peRatioWeighted = peRatioWeighted; }

    public BigDecimal getDividendYieldWeighted() { return dividendYieldWeighted; }
    public void setDividendYieldWeighted(BigDecimal dividendYieldWeighted) { this.dividendYieldWeighted = dividendYieldWeighted; }

    public BigDecimal getBetaWeighted() { return betaWeighted; }
    public void setBetaWeighted(BigDecimal betaWeighted) { this.betaWeighted = betaWeighted; }

    public BigDecimal getVolatilityWeighted() { return volatilityWeighted; }
    public void setVolatilityWeighted(BigDecimal volatilityWeighted) { this.volatilityWeighted = volatilityWeighted; }

    public BigDecimal getSectorDiversificationScore() { return sectorDiversificationScore; }
    public void setSectorDiversificationScore(BigDecimal sectorDiversificationScore) { this.sectorDiversificationScore = sectorDiversificationScore; }

    public BigDecimal getGeographicDiversificationScore() { return geographicDiversificationScore; }
    public void setGeographicDiversificationScore(BigDecimal geographicDiversificationScore) { this.geographicDiversificationScore = geographicDiversificationScore; }

    public BigDecimal getRiskScore() { return riskScore; }
    public void setRiskScore(BigDecimal riskScore) { this.riskScore = riskScore; }

    public BigDecimal getPerformanceScore() { return performanceScore; }
    public void setPerformanceScore(BigDecimal performanceScore) { this.performanceScore = performanceScore; }

    public BigDecimal getDataQualityScore() { return dataQualityScore; }
    public void setDataQualityScore(BigDecimal dataQualityScore) { this.dataQualityScore = dataQualityScore; }

    public List<ConstituentMarketData> getConstituents() { return constituents; }
    public void setConstituents(List<ConstituentMarketData> constituents) { this.constituents = constituents; }

    public List<PerformanceMetric> getPerformanceMetrics() { return performanceMetrics; }
    public void setPerformanceMetrics(List<PerformanceMetric> performanceMetrics) { this.performanceMetrics = performanceMetrics; }

    // ConstituentMarketData inner class
    public static class ConstituentMarketData {
        private String instrumentId;
        private String symbol;
        private BigDecimal weight;
        private BigDecimal marketValue;
        private BigDecimal lastPrice;
        private BigDecimal changePercentage;
        private BigDecimal beta;
        private BigDecimal volatility;

        // Default constructor
        public ConstituentMarketData() {}

        // All-args constructor
        public ConstituentMarketData(String instrumentId, String symbol, BigDecimal weight,
                                    BigDecimal marketValue, BigDecimal lastPrice, BigDecimal changePercentage,
                                    BigDecimal beta, BigDecimal volatility) {
            this.instrumentId = instrumentId;
            this.symbol = symbol;
            this.weight = weight;
            this.marketValue = marketValue;
            this.lastPrice = lastPrice;
            this.changePercentage = changePercentage;
            this.beta = beta;
            this.volatility = volatility;
        }

        // Builder pattern
        public static ConstituentMarketDataBuilder builder() {
            return new ConstituentMarketDataBuilder();
        }

        public static class ConstituentMarketDataBuilder {
            private String instrumentId;
            private String symbol;
            private BigDecimal weight;
            private BigDecimal marketValue;
            private BigDecimal lastPrice;
            private BigDecimal changePercentage;
            private BigDecimal beta;
            private BigDecimal volatility;

            public ConstituentMarketDataBuilder instrumentId(String instrumentId) { this.instrumentId = instrumentId; return this; }
            public ConstituentMarketDataBuilder symbol(String symbol) { this.symbol = symbol; return this; }
            public ConstituentMarketDataBuilder weight(BigDecimal weight) { this.weight = weight; return this; }
            public ConstituentMarketDataBuilder marketValue(BigDecimal marketValue) { this.marketValue = marketValue; return this; }
            public ConstituentMarketDataBuilder lastPrice(BigDecimal lastPrice) { this.lastPrice = lastPrice; return this; }
            public ConstituentMarketDataBuilder changePercentage(BigDecimal changePercentage) { this.changePercentage = changePercentage; return this; }
            public ConstituentMarketDataBuilder beta(BigDecimal beta) { this.beta = beta; return this; }
            public ConstituentMarketDataBuilder volatility(BigDecimal volatility) { this.volatility = volatility; return this; }

            public ConstituentMarketData build() {
                return new ConstituentMarketData(instrumentId, symbol, weight, marketValue, lastPrice, changePercentage, beta, volatility);
            }
        }

        // Getters and Setters
        public String getInstrumentId() { return instrumentId; }
        public void setInstrumentId(String instrumentId) { this.instrumentId = instrumentId; }

        public String getSymbol() { return symbol; }
        public void setSymbol(String symbol) { this.symbol = symbol; }

        public BigDecimal getWeight() { return weight; }
        public void setWeight(BigDecimal weight) { this.weight = weight; }

        public BigDecimal getMarketValue() { return marketValue; }
        public void setMarketValue(BigDecimal marketValue) { this.marketValue = marketValue; }

        public BigDecimal getLastPrice() { return lastPrice; }
        public void setLastPrice(BigDecimal lastPrice) { this.lastPrice = lastPrice; }

        public BigDecimal getChangePercentage() { return changePercentage; }
        public void setChangePercentage(BigDecimal changePercentage) { this.changePercentage = changePercentage; }

        public BigDecimal getBeta() { return beta; }
        public void setBeta(BigDecimal beta) { this.beta = beta; }

        public BigDecimal getVolatility() { return volatility; }
        public void setVolatility(BigDecimal volatility) { this.volatility = volatility; }
    }

    // PerformanceMetric inner class
    public static class PerformanceMetric {
        private LocalDateTime timestamp;
        private BigDecimal marketValue;
        private BigDecimal returnPercentage;
        private BigDecimal riskMetric;

        // Default constructor
        public PerformanceMetric() {}

        // All-args constructor
        public PerformanceMetric(LocalDateTime timestamp, BigDecimal marketValue,
                               BigDecimal returnPercentage, BigDecimal riskMetric) {
            this.timestamp = timestamp;
            this.marketValue = marketValue;
            this.returnPercentage = returnPercentage;
            this.riskMetric = riskMetric;
        }

        // Builder pattern
        public static PerformanceMetricBuilder builder() {
            return new PerformanceMetricBuilder();
        }

        public static class PerformanceMetricBuilder {
            private LocalDateTime timestamp;
            private BigDecimal marketValue;
            private BigDecimal returnPercentage;
            private BigDecimal riskMetric;

            public PerformanceMetricBuilder timestamp(LocalDateTime timestamp) { this.timestamp = timestamp; return this; }
            public PerformanceMetricBuilder marketValue(BigDecimal marketValue) { this.marketValue = marketValue; return this; }
            public PerformanceMetricBuilder returnPercentage(BigDecimal returnPercentage) { this.returnPercentage = returnPercentage; return this; }
            public PerformanceMetricBuilder riskMetric(BigDecimal riskMetric) { this.riskMetric = riskMetric; return this; }

            public PerformanceMetric build() {
                return new PerformanceMetric(timestamp, marketValue, returnPercentage, riskMetric);
            }
        }

        // Getters and Setters
        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

        public BigDecimal getMarketValue() { return marketValue; }
        public void setMarketValue(BigDecimal marketValue) { this.marketValue = marketValue; }

        public BigDecimal getReturnPercentage() { return returnPercentage; }
        public void setReturnPercentage(BigDecimal returnPercentage) { this.returnPercentage = returnPercentage; }

        public BigDecimal getRiskMetric() { return riskMetric; }
        public void setRiskMetric(BigDecimal riskMetric) { this.riskMetric = riskMetric; }
    }
}
