package com.custom.indexbasket.basket.dto;

import java.time.LocalDateTime;
import java.util.Map;

public record HealthResponse(
    String status,
    LocalDateTime timestamp,
    Map<String, Object> details
) {}
