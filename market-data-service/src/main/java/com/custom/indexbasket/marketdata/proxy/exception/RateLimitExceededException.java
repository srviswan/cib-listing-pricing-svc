package com.custom.indexbasket.marketdata.proxy.exception;

/**
 * Exception thrown when rate limit is exceeded for a data source.
 */
public class RateLimitExceededException extends RuntimeException {
    
    public RateLimitExceededException(String message) {
        super(message);
    }
    
    public RateLimitExceededException(String message, Throwable cause) {
        super(message, cause);
    }
}
