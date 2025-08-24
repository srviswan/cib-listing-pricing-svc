package com.custom.indexbasket.basket.dto;

import java.math.BigDecimal;

public record SectorExposureResponse(
    String sector,
    BigDecimal weight,
    BigDecimal marketValue,
    BigDecimal returnValue
) {}
