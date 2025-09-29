package com.custom.indexbasket.fix.service;

import com.custom.indexbasket.fix.model.PublishingResult;
import com.custom.indexbasket.fix.model.PricePublishingRequest;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import quickfix.*;
import quickfix.field.*;
import quickfix.fix44.*;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * FIX Message Handler
 * 
 * Handles FIX message construction, parsing, and processing.
 */
@Service
@Slf4j
public class FixMessageHandler {
    
    @Autowired
    private FixSessionManager sessionManager;
    
    @Autowired
    private MeterRegistry meterRegistry;
    
    // Track message responses
    private final ConcurrentHashMap<String, String> pendingMessages = new ConcurrentHashMap<>();
    
    /**
     * Publish market data via FIX
     */
    public Mono<PublishingResult> publishMarketData(PricePublishingRequest request) {
        return Mono.fromCallable(() -> {
            long startTime = System.currentTimeMillis();
            
            if (!request.isValid()) {
                return PublishingResult.failure(
                    request.getBasketId(), 
                    request.getSymbol(), 
                    "INVALID_REQUEST", 
                    "Request validation failed"
                );
            }
            
            try {
                // Construct FIX market data message
                quickfix.Message message = constructMarketDataMessage(request);
                
                // Send message
                boolean sent = sessionManager.sendMessage(message).block();
                
                if (sent) {
                    long processingTime = System.currentTimeMillis() - startTime;
                    String fixMessageId = generateMessageId();
                    
                    // Track the message
                    pendingMessages.put(fixMessageId, request.getBasketId());
                    
                    // Update metrics
                    meterRegistry.counter("fix.market.data.published").increment();
                    meterRegistry.timer("fix.message.processing.time").record(Duration.ofMillis(processingTime));
                    
                    log.info("Market data published successfully for basket: {} symbol: {}", 
                        request.getBasketId(), request.getSymbol());
                    
                    return PublishingResult.success(request.getBasketId(), request.getSymbol(), fixMessageId)
                        .toBuilder()
                        .processingTimeMs(processingTime)
                        .build();
                        
                } else {
                    return PublishingResult.failure(
                        request.getBasketId(), 
                        request.getSymbol(), 
                        "SEND_FAILED", 
                        "Failed to send FIX message"
                    );
                }
                
            } catch (Exception e) {
                log.error("Error publishing market data for basket {}: {}", request.getBasketId(), e.getMessage(), e);
                
                return PublishingResult.failure(
                    request.getBasketId(), 
                    request.getSymbol(), 
                    "PUBLISH_ERROR", 
                    e.getMessage()
                );
            }
        })
        .subscribeOn(Schedulers.boundedElastic())
        .timeout(Duration.ofSeconds(10));
    }
    
    /**
     * Construct FIX market data message
     */
    private quickfix.Message constructMarketDataMessage(PricePublishingRequest request) throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue {
        quickfix.Message message = new quickfix.Message();
        
        // Header fields
        message.getHeader().setString(MsgType.FIELD, MsgType.MARKET_DATA_INCREMENTAL_REFRESH);
        message.getHeader().setString(MsgSeqNum.FIELD, String.valueOf(getNextSequenceNumber()));
        message.getHeader().setUtcTimeStamp(SendingTime.FIELD, java.time.LocalDateTime.now());
        message.getHeader().setString(SenderCompID.FIELD, "YOUR_COMP_ID");
        message.getHeader().setString(TargetCompID.FIELD, "BLOOMBERG");
        
        // Market data fields
        message.setString(Symbol.FIELD, request.getFormattedSymbol());
        message.setString(SecurityID.FIELD, request.getSymbol());
        message.setString(SecurityIDSource.FIELD, "8"); // RIC
        message.setString(MDEntryType.FIELD, String.valueOf(MDEntryType.BID));
        message.setDouble(MDEntryPx.FIELD, request.getPrice().doubleValue());
        message.setDouble(MDEntrySize.FIELD, request.getVolume() != null ? request.getVolume().doubleValue() : 1000.0);
        message.setString(MDEntryTime.FIELD, request.getTimestamp().toString());
        message.setString(Currency.FIELD, request.getCurrency());
        
        // Additional fields
        message.setString(MDUpdateAction.FIELD, "0"); // New
        message.setString(NoMDEntries.FIELD, "1");
        
        return message;
    }
    
    /**
     * Handle application messages
     */
    public void handleApplicationMessage(quickfix.Message message, SessionID sessionId) throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, UnsupportedMessageType {
        try {
            String msgType = message.getHeader().getString(MsgType.FIELD);
            
            switch (msgType) {
                case MsgType.MARKET_DATA_REQUEST_REJECT:
                    handleMarketDataReject(message, sessionId);
                    break;
                    
                case MsgType.MARKET_DATA_SNAPSHOT_FULL_REFRESH:
                    handleMarketDataSnapshot(message, sessionId);
                    break;
                    
                default:
                    log.warn("Unknown application message type received: {}", msgType);
            }
            
        } catch (Exception e) {
            log.error("Error handling application message: {}", e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * Handle heartbeat
     */
    public void onHeartbeat(quickfix.Message message, SessionID sessionId) {
        log.debug("Received heartbeat from session: {}", sessionId);
        sessionManager.onHeartbeat();
    }
    
    /**
     * Handle test request
     */
    public void onTestRequest(quickfix.Message message, SessionID sessionId) {
        log.debug("Received test request from session: {}", sessionId);
        // Send heartbeat response
        try {
            quickfix.Message heartbeat = new quickfix.Message();
            heartbeat.getHeader().setString(MsgType.FIELD, MsgType.HEARTBEAT);
            sessionManager.sendMessage(heartbeat).subscribe();
        } catch (Exception e) {
            log.error("Error sending heartbeat response: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Handle logon
     */
    public void onLogon(quickfix.Message message, SessionID sessionId) {
        log.info("Received logon message from session: {}", sessionId);
    }
    
    /**
     * Handle logout
     */
    public void onLogout(quickfix.Message message, SessionID sessionId) {
        log.info("Received logout message from session: {}", sessionId);
    }
    
    /**
     * Handle session logon
     */
    public void onSessionLogon(SessionID sessionId) {
        log.info("Session logged on: {}", sessionId);
        meterRegistry.gauge("fix.sessions.logged_on", 1);
    }
    
    /**
     * Handle session logout
     */
    public void onSessionLogout(SessionID sessionId) {
        log.info("Session logged out: {}", sessionId);
        meterRegistry.gauge("fix.sessions.logged_on", 0);
    }
    
    /**
     * Handle market data reject
     */
    private void handleMarketDataReject(quickfix.Message message, SessionID sessionId) throws FieldNotFound {
        String rejectReason = message.getString(MDReqRejReason.FIELD);
        log.warn("Market data request rejected for session {}: {}", sessionId, rejectReason);
        
        meterRegistry.counter("fix.market.data.rejected").increment();
    }
    
    /**
     * Handle market data snapshot
     */
    private void handleMarketDataSnapshot(quickfix.Message message, SessionID sessionId) throws FieldNotFound {
        log.debug("Received market data snapshot from session: {}", sessionId);
        
        meterRegistry.counter("fix.market.data.received").increment();
    }
    
    /**
     * Generate unique message ID
     */
    private String generateMessageId() {
        return UUID.randomUUID().toString();
    }
    
    /**
     * Get next sequence number
     */
    private int getNextSequenceNumber() {
        // In a real implementation, this would get the next sequence number from the session
        return (int) System.currentTimeMillis() % 1000000;
    }
}
