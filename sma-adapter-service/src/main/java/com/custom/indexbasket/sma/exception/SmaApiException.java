package com.custom.indexbasket.sma.exception;

/**
 * SMA API Exception
 * 
 * Exception thrown when SMA Refinitiv API operations fail.
 */
public class SmaApiException extends RuntimeException {
    
    public SmaApiException(String message) {
        super(message);
    }
    
    public SmaApiException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public SmaApiException(Throwable cause) {
        super(cause);
    }
}
