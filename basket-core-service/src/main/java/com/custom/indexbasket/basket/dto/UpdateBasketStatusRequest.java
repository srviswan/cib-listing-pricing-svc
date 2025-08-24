package com.custom.indexbasket.basket.dto;

/**
 * Request DTO for updating basket status using state machine workflow
 */
public record UpdateBasketStatusRequest(
    String event,
    String updatedBy
) {}
