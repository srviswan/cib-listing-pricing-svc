package com.custom.indexbasket.integration.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Publishing Result Model
 * 
 * Represents the result of a publishing operation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublishingResult {
    
    private String basketId;
    private String symbol;
    private boolean success;
    private String message;
    private String fixMessageId;
    private String errorCode;
    private String errorMessage;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    private LocalDateTime timestamp;
    
    private long processingTimeMs;
    private String sessionId;
    
    /**
     * Create a successful result
     */
    public static PublishingResult success(String basketId, String symbol, String fixMessageId) {
        return PublishingResult.builder()
            .basketId(basketId)
            .symbol(symbol)
            .success(true)
            .message("Price published successfully")
            .fixMessageId(fixMessageId)
            .timestamp(LocalDateTime.now())
            .build();
    }
    
    /**
     * Create a failed result
     */
    public static PublishingResult failure(String basketId, String symbol, String errorCode, String errorMessage) {
        return PublishingResult.builder()
            .basketId(basketId)
            .symbol(symbol)
            .success(false)
            .message("Price publishing failed")
            .errorCode(errorCode)
            .errorMessage(errorMessage)
            .timestamp(LocalDateTime.now())
            .build();
    }
    
    /**
     * Create a builder from current instance
     */
    public PublishingResultBuilder toBuilder() {
        return PublishingResult.builder()
            .basketId(this.basketId)
            .symbol(this.symbol)
            .success(this.success)
            .message(this.message)
            .fixMessageId(this.fixMessageId)
            .errorCode(this.errorCode)
            .errorMessage(this.errorMessage)
            .timestamp(this.timestamp)
            .processingTimeMs(this.processingTimeMs)
            .sessionId(this.sessionId);
    }
}
