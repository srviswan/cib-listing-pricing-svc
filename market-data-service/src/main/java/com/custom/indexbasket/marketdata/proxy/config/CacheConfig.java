package com.custom.indexbasket.marketdata.proxy.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for caching.
 */
@ConfigurationProperties(prefix = "market-data.cache")
public class CacheConfig {
    
    private String keyPrefix = "market_data";
    private int maxSize = 10000;
    private int getTimeoutMs = 100;
    private int setTimeoutMs = 200;
    
    // TTL Configuration
    private int highQualityTTLMinutes = 5;
    private int mediumQualityTTLMinutes = 2;
    private int lowQualityTTLMinutes = 1;
    private int defaultTTLMinutes = 1;
    
    // Redis Configuration
    private String redisHost = "localhost";
    private int redisPort = 6379;
    private String redisPassword;
    private int redisDatabase = 0;
    private int redisConnectionPoolSize = 10;

    // Getters and Setters
    public String getKeyPrefix() { return keyPrefix; }
    public void setKeyPrefix(String keyPrefix) { this.keyPrefix = keyPrefix; }

    public int getMaxSize() { return maxSize; }
    public void setMaxSize(int maxSize) { this.maxSize = maxSize; }

    public int getGetTimeoutMs() { return getTimeoutMs; }
    public void setGetTimeoutMs(int getTimeoutMs) { this.getTimeoutMs = getTimeoutMs; }

    public int getSetTimeoutMs() { return setTimeoutMs; }
    public void setTimeoutMs(int setTimeoutMs) { this.setTimeoutMs = setTimeoutMs; }

    public int getHighQualityTTLMinutes() { return highQualityTTLMinutes; }
    public void setHighQualityTTLMinutes(int highQualityTTLMinutes) { this.highQualityTTLMinutes = highQualityTTLMinutes; }

    public int getMediumQualityTTLMinutes() { return mediumQualityTTLMinutes; }
    public void setMediumQualityTTLMinutes(int mediumQualityTTLMinutes) { this.mediumQualityTTLMinutes = mediumQualityTTLMinutes; }

    public int getLowQualityTTLMinutes() { return lowQualityTTLMinutes; }
    public void setLowQualityTTLMinutes(int lowQualityTTLMinutes) { this.lowQualityTTLMinutes = lowQualityTTLMinutes; }

    public int getDefaultTTLMinutes() { return defaultTTLMinutes; }
    public void setDefaultTTLMinutes(int defaultTTLMinutes) { this.defaultTTLMinutes = defaultTTLMinutes; }

    public String getRedisHost() { return redisHost; }
    public void setRedisHost(String redisHost) { this.redisHost = redisHost; }

    public int getRedisPort() { return redisPort; }
    public void setRedisPort(int redisPort) { this.redisPort = redisPort; }

    public String getRedisPassword() { return redisPassword; }
    public void setRedisPassword(String redisPassword) { this.redisPassword = redisPassword; }

    public int getRedisDatabase() { return redisDatabase; }
    public void setRedisDatabase(int redisDatabase) { this.redisDatabase = redisDatabase; }

    public int getRedisConnectionPoolSize() { return redisConnectionPoolSize; }
    public void setRedisConnectionPoolSize(int redisConnectionPoolSize) { this.redisConnectionPoolSize = redisConnectionPoolSize; }
}
