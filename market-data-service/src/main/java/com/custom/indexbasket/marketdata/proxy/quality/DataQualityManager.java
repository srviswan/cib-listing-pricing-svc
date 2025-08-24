package com.custom.indexbasket.marketdata.proxy.quality;

import com.custom.indexbasket.marketdata.dto.MarketDataResponse;
import com.custom.indexbasket.marketdata.proxy.quality.model.DataQualityReport;
import com.custom.indexbasket.marketdata.proxy.quality.model.DataQualityScore;
import com.custom.indexbasket.marketdata.proxy.quality.model.DataQualityIssue;
import com.custom.indexbasket.marketdata.proxy.quality.model.DataQualityMetrics;
import com.custom.indexbasket.marketdata.proxy.quality.model.DataQualityTrend;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * Interface for managing data quality operations.
 */
public interface DataQualityManager {
    
    /**
     * Validate market data and return quality report
     */
    Mono<DataQualityReport> validateData(MarketDataResponse data);
    
    /**
     * Calculate quality score for an instrument from a specific data source
     */
    Mono<DataQualityScore> calculateQualityScore(String instrumentId, String dataSource);
    
    /**
     * Report data quality issues for investigation
     */
    Mono<Void> reportQualityIssues(DataQualityIssue issue);
    
    /**
     * Get overall data quality metrics
     */
    Mono<DataQualityMetrics> getQualityMetrics();
    
    /**
     * Get quality trends over time
     */
    Flux<DataQualityTrend> getQualityTrends(Duration timeWindow);
}
