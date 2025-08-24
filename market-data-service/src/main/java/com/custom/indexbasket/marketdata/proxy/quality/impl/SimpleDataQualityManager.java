package com.custom.indexbasket.marketdata.proxy.quality.impl;

import com.custom.indexbasket.marketdata.dto.MarketDataResponse;
import com.custom.indexbasket.marketdata.proxy.quality.DataQualityManager;
import com.custom.indexbasket.marketdata.proxy.quality.DataValidator;
import com.custom.indexbasket.marketdata.proxy.quality.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Simple implementation of DataQualityManager for development purposes.
 */
@Service
public class SimpleDataQualityManager implements DataQualityManager {
    
    private static final Logger log = LoggerFactory.getLogger(SimpleDataQualityManager.class);
    private final List<DataValidator> validators;
    
    public SimpleDataQualityManager(List<DataValidator> validators) {
        this.validators = validators;
    }
    
    @Override
    public Mono<DataQualityReport> validateData(MarketDataResponse data) {
        log.debug("Validating data for instrument: {}", data.getInstrumentId());
        
        List<ValidationError> allErrors = validators.stream()
            .flatMap(validator -> {
                ValidationResult result = validator.validate(data);
                return result.getErrors().stream();
            })
            .toList();
        
        boolean isValid = allErrors.isEmpty();
        double qualityScore = calculateQualityScore(allErrors);
        
        DataQualityReport report = new DataQualityReport(
            data.getInstrumentId(),
            data.getDataSource(),
            LocalDateTime.now(),
            isValid,
            qualityScore,
            allErrors,
            Map.of("validatorCount", validators.size())
        );
        
        log.debug("Data validation completed for instrument: {} - Valid: {}, Score: {}", 
            data.getInstrumentId(), isValid, qualityScore);
        
        return Mono.just(report);
    }
    
    @Override
    public Mono<DataQualityScore> calculateQualityScore(String instrumentId, String dataSource) {
        // For development, return a mock quality score
        double score = 85.0 + Math.random() * 15.0; // Random score between 85-100
        
        DataQualityScore qualityScore = new DataQualityScore(
            instrumentId,
            dataSource,
            score,
            LocalDateTime.now(),
            Map.of("mock", true, "score", score)
        );
        
        return Mono.just(qualityScore);
    }
    
    @Override
    public Mono<Void> reportQualityIssues(DataQualityIssue issue) {
        log.warn("Data quality issue reported: {} - {}", issue.getIssueType(), issue.getDescription());
        // In production, this would send alerts or notifications
        return Mono.empty();
    }
    
    @Override
    public Mono<DataQualityMetrics> getQualityMetrics() {
        // For development, return mock metrics
        DataQualityMetrics metrics = new DataQualityMetrics(
            LocalDateTime.now(),
            1000L, // totalRecords
            950L,  // validRecords
            50L,   // invalidRecords
            95.0,  // overallQualityScore
            Map.of("mock", true)
        );
        
        return Mono.just(metrics);
    }
    
    @Override
    public Flux<DataQualityTrend> getQualityTrends(Duration timeWindow) {
        // For development, return mock trends
        LocalDateTime now = LocalDateTime.now();
        
        List<DataQualityTrend> trends = List.of(
            new DataQualityTrend(now.minusHours(1), 92.0, 100L, 8L),
            new DataQualityTrend(now.minusMinutes(30), 95.0, 100L, 5L),
            new DataQualityTrend(now, 97.0, 100L, 3L)
        );
        
        return Flux.fromIterable(trends);
    }
    
    private double calculateQualityScore(List<ValidationError> errors) {
        if (errors.isEmpty()) {
            return 100.0;
        }
        
        // Calculate score based on error severity
        double totalPenalty = errors.stream()
            .mapToDouble(error -> {
                return switch (error.getSeverity()) {
                    case "CRITICAL" -> 20.0;
                    case "HIGH" -> 15.0;
                    case "MEDIUM" -> 10.0;
                    case "LOW" -> 5.0;
                    default -> 10.0;
                };
            })
            .sum();
        
        return Math.max(0.0, 100.0 - totalPenalty);
    }
}
