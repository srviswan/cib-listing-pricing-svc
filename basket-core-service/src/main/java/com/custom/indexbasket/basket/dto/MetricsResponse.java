package com.custom.indexbasket.basket.dto;

import java.time.LocalDateTime;
import java.util.Map;

public record MetricsResponse(
    LocalDateTime timestamp,
    Map<String, Object> metrics
) {}
