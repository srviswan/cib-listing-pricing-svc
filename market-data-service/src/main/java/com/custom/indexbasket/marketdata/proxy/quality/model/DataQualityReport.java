package com.custom.indexbasket.marketdata.proxy.quality.model;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Report containing data quality validation results.
 */
public record DataQualityReport(
    String instrumentId,
    String dataSource,
    LocalDateTime validationTime,
    boolean isValid,
    double qualityScore,
    List<ValidationError> errors,
    Map<String, Object> metadata
) {}
