package com.custom.indexbasket.basket.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record BasketAnalyticsResponse(
    String basketCode,
    BigDecimal totalMarketValue,
    BigDecimal dailyReturn,
    BigDecimal cumulativeReturn,
    BigDecimal volatility,
    BigDecimal sharpeRatio,
    BigDecimal maxDrawdown,
    List<SectorExposureResponse> sectorExposure,
    LocalDateTime lastUpdated
) {}
