package com.custom.indexbasket.marketdata.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Market Data Request DTO - Request for market data
 * 
 * @author Custom Index Basket Team
 * @version 1.0.0
 */
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarketDataRequest {

    @NotNull(message = "Instrument IDs cannot be null")
    @Size(min = 1, max = 100, message = "Must request 1-100 instruments")
    private List<String> instrumentIds;

    @NotBlank(message = "Data source cannot be blank")
    @Pattern(regexp = "^(BLOOMBERG|REUTERS|YAHOO|ALPHA_VANTAGE|QUANDL|CUSTOM)$", 
            message = "Data source must be BLOOMBERG, REUTERS, YAHOO, ALPHA_VANTAGE, QUANDL, or CUSTOM")
    private String dataSource;

    @Pattern(regexp = "^(NYSE|NASDAQ|LSE|TSE|ASX|NSE|BSE|CUSTOM)$", 
            message = "Exchange must be a valid exchange code or CUSTOM")
    private String exchange;
    
    @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be a 3-letter currency code")
    private String currency;
    
    private Boolean includeHistorical;
    private Boolean includeAnalytics;

    // Manual getters since Lombok @Data is not working
    public List<String> getInstrumentIds() {
        return instrumentIds;
    }

    public void setInstrumentIds(List<String> instrumentIds) {
        this.instrumentIds = instrumentIds;
    }

    public String getDataSource() {
        return dataSource;
    }

    public void setDataSource(String dataSource) {
        this.dataSource = dataSource;
    }

    public String getExchange() {
        return exchange;
    }

    public void setExchange(String exchange) {
        this.exchange = exchange;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public Boolean getIncludeHistorical() {
        return includeHistorical;
    }

    public void setIncludeHistorical(Boolean includeHistorical) {
        this.includeHistorical = includeHistorical;
    }

    public Boolean getIncludeAnalytics() {
        return includeAnalytics;
    }

    public void setIncludeAnalytics(Boolean includeAnalytics) {
        this.includeAnalytics = includeAnalytics;
    }
}
