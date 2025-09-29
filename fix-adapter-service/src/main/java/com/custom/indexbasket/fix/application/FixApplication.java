package com.custom.indexbasket.fix.application;

import com.custom.indexbasket.fix.service.FixMessageHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import quickfix.*;
import quickfix.field.*;
import quickfix.fix44.*;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * FIX Application
 * 
 * Handles FIX protocol messages and session events.
 */
@Component
@Slf4j
public class FixApplication implements Application {
    
    @Autowired
    private FixMessageHandler messageHandler;
    
    private final AtomicInteger messageCount = new AtomicInteger(0);
    private final AtomicInteger heartbeatCount = new AtomicInteger(0);
    
    @Override
    public void onCreate(SessionID sessionId) {
        log.info("FIX application created for session: {}", sessionId);
    }
    
    @Override
    public void onLogon(SessionID sessionId) {
        log.info("FIX session logged on: {}", sessionId);
        messageHandler.onSessionLogon(sessionId);
    }
    
    @Override
    public void onLogout(SessionID sessionId) {
        log.info("FIX session logged out: {}", sessionId);
        messageHandler.onSessionLogout(sessionId);
    }
    
    @Override
    public void toAdmin(quickfix.Message message, SessionID sessionId) {
        log.debug("Sending admin message: {}", message);
    }
    
    @Override
    public void fromAdmin(quickfix.Message message, SessionID sessionId) 
            throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, RejectLogon {
        
        try {
            String msgType = message.getHeader().getString(MsgType.FIELD);
            
            switch (msgType) {
                case MsgType.HEARTBEAT:
                    heartbeatCount.incrementAndGet();
                    log.debug("Received heartbeat from session: {}", sessionId);
                    messageHandler.onHeartbeat(message, sessionId);
                    break;
                    
                case MsgType.TEST_REQUEST:
                    log.debug("Received test request from session: {}", sessionId);
                    messageHandler.onTestRequest(message, sessionId);
                    break;
                    
                case MsgType.LOGON:
                    log.info("Received logon message from session: {}", sessionId);
                    messageHandler.onLogon(message, sessionId);
                    break;
                    
                case MsgType.LOGOUT:
                    log.info("Received logout message from session: {}", sessionId);
                    messageHandler.onLogout(message, sessionId);
                    break;
                    
                default:
                    log.warn("Unknown admin message type received: {}", msgType);
            }
        } catch (Exception e) {
            log.error("Error processing admin message: {}", e.getMessage(), e);
        }
    }
    
    @Override
    public void toApp(quickfix.Message message, SessionID sessionId) throws DoNotSend {
        log.debug("Sending application message: {}", message);
        messageCount.incrementAndGet();
    }
    
    @Override
    public void fromApp(quickfix.Message message, SessionID sessionId) 
            throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, UnsupportedMessageType {
        
        try {
            log.debug("Received application message: {}", message);
            messageHandler.handleApplicationMessage(message, sessionId);
        } catch (Exception e) {
            log.error("Error handling application message: {}", e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * Get message statistics
     */
    public int getMessageCount() {
        return messageCount.get();
    }
    
    /**
     * Get heartbeat count
     */
    public int getHeartbeatCount() {
        return heartbeatCount.get();
    }
    
    /**
     * Reset counters
     */
    public void resetCounters() {
        messageCount.set(0);
        heartbeatCount.set(0);
    }
}
