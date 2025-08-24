package com.custom.indexbasket.marketdata.proxy.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for data source proxies.
 */
@ConfigurationProperties(prefix = "market-data.data-sources")
public class DataSourceConfig {
    
    private String dataSourceName;
    private String baseUrl;
    private String apiKey;
    private int rateLimit = 1000;
    private int timeoutSeconds = 30;
    private int maxRetries = 3;
    private int retryDelaySeconds = 1;
    private int concurrencyLimit = 10;
    
    // Bloomberg specific
    private String bloombergAppName;
    private String bloombergServerHost;
    private int bloombergServerPort;
    
    // Reuters specific
    private String reutersUsername;
    private String reutersPassword;
    private String reutersAppId;
    
    // Yahoo Finance specific
    private String yahooFinanceApiKey;
    private int yahooFinanceTimeoutSeconds = 30;
    
    // Getters and Setters
    public String getDataSourceName() { return dataSourceName; }
    public void setDataSourceName(String dataSourceName) { this.dataSourceName = dataSourceName; }
    
    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    
    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    
    public int getRateLimit() { return rateLimit; }
    public void setRateLimit(int rateLimit) { this.rateLimit = rateLimit; }
    
    public int getTimeoutSeconds() { return timeoutSeconds; }
    public void setTimeoutSeconds(int timeoutSeconds) { this.timeoutSeconds = timeoutSeconds; }
    
    public int getMaxRetries() { return maxRetries; }
    public void setMaxRetries(int maxRetries) { this.maxRetries = maxRetries; }
    
    public int getRetryDelaySeconds() { return retryDelaySeconds; }
    public void setRetryDelaySeconds(int retryDelaySeconds) { this.retryDelaySeconds = retryDelaySeconds; }
    
    public int getConcurrencyLimit() { return concurrencyLimit; }
    public void setConcurrencyLimit(int concurrencyLimit) { this.concurrencyLimit = concurrencyLimit; }
    
    public String getBloombergAppName() { return bloombergAppName; }
    public void setBloombergAppName(String bloombergAppName) { this.bloombergAppName = bloombergAppName; }
    
    public String getBloombergServerHost() { return bloombergServerHost; }
    public void setBloombergServerHost(String bloombergServerHost) { this.bloombergServerHost = bloombergServerHost; }
    
    public int getBloombergServerPort() { return bloombergServerPort; }
    public void setBloombergServerPort(int bloombergServerPort) { this.bloombergServerPort = bloombergServerPort; }
    
    public String getReutersUsername() { return reutersUsername; }
    public void setReutersUsername(String reutersUsername) { this.reutersUsername = reutersUsername; }
    
    public String getReutersPassword() { return reutersPassword; }
    public void setReutersPassword(String reutersPassword) { this.reutersPassword = reutersPassword; }
    
    public String getReutersAppId() { return reutersAppId; }
    public void setReutersAppId(String reutersAppId) { this.reutersAppId = reutersAppId; }
    
    public String getYahooFinanceApiKey() { return yahooFinanceApiKey; }
    public void setYahooFinanceApiKey(String yahooFinanceApiKey) { this.yahooFinanceApiKey = yahooFinanceApiKey; }
    
    public int getYahooFinanceTimeoutSeconds() { return yahooFinanceTimeoutSeconds; }
    public void setYahooFinanceTimeoutSeconds(int yahooFinanceTimeoutSeconds) { this.yahooFinanceTimeoutSeconds = yahooFinanceTimeoutSeconds; }
}
