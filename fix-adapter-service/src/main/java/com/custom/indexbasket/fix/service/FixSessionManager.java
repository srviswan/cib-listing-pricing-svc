package com.custom.indexbasket.fix.service;

import com.custom.indexbasket.fix.application.FixApplication;
import com.custom.indexbasket.fix.config.FixConfiguration;
import com.custom.indexbasket.fix.model.FixSessionStatus;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import quickfix.*;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * FIX Session Manager
 * 
 * Manages FIX sessions and connections to Bloomberg FixNet.
 */
@Service
@Slf4j
public class FixSessionManager {
    
    private final FixConfiguration config;
    private final FixApplication fixApplication;
    private final MeterRegistry meterRegistry;
    
    private SessionSettings sessionSettings;
    private MessageStoreFactory messageStoreFactory;
    private LogFactory logFactory;
    private MessageFactory messageFactory;
    private SocketInitiator socketInitiator;
    private Session session;
    
    // Session statistics
    private final AtomicInteger messagesSent = new AtomicInteger(0);
    private final AtomicInteger messagesReceived = new AtomicInteger(0);
    private final AtomicInteger errors = new AtomicInteger(0);
    private final AtomicLong totalLatency = new AtomicLong(0);
    private volatile java.time.LocalDateTime lastConnected;
    private volatile java.time.LocalDateTime lastHeartbeat;
    private volatile boolean connected = false;
    private volatile boolean loggedOn = false;
    
    @Autowired
    public FixSessionManager(FixConfiguration config, FixApplication fixApplication, MeterRegistry meterRegistry) {
        this.config = config;
        this.fixApplication = fixApplication;
        this.meterRegistry = meterRegistry;
    }
    
    @PostConstruct
    public void initialize() {
        try {
            log.info("Initializing FIX session manager...");
            
            // Create session settings
            sessionSettings = createSessionSettings();
            
            // Create factories
            messageStoreFactory = new FileStoreFactory(sessionSettings);
            logFactory = new FileLogFactory(sessionSettings);
            messageFactory = new DefaultMessageFactory();
            
            // Create socket initiator
            socketInitiator = new SocketInitiator(fixApplication, messageStoreFactory, sessionSettings, logFactory, messageFactory);
            
            // Start the session
            socketInitiator.start();
            
            log.info("FIX session manager initialized successfully");
            
        } catch (Exception e) {
            log.error("Failed to initialize FIX session manager: {}", e.getMessage(), e);
            errors.incrementAndGet();
        }
    }
    
    @PreDestroy
    public void shutdown() {
        try {
            log.info("Shutting down FIX session manager...");
            
            if (socketInitiator != null) {
                socketInitiator.stop();
            }
            
            connected = false;
            loggedOn = false;
            
            log.info("FIX session manager shut down successfully");
            
        } catch (Exception e) {
            log.error("Error during FIX session manager shutdown: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Create session settings from configuration
     */
    private SessionSettings createSessionSettings() throws ConfigError {
        SessionSettings settings = new SessionSettings();
        
        SessionID sessionId = new SessionID(
            config.getSession().getBeginString(),
            config.getConnection().getSenderCompId(),
            config.getConnection().getTargetCompId()
        );
        
        // Connection settings
        settings.setString(sessionId, "ConnectionType", "initiator");
        settings.setString(sessionId, "SocketConnectHost", config.getConnection().getHost());
        settings.setString(sessionId, "SocketConnectPort", String.valueOf(config.getConnection().getPort()));
        settings.setBool(sessionId, "SocketUseSSL", config.getConnection().isUseSsl());
        
        // Session settings
        settings.setString(sessionId, "HeartBtInt", String.valueOf(config.getSession().getHeartbeatInterval()));
        settings.setString(sessionId, "StartTime", config.getSession().getStartTime());
        settings.setString(sessionId, "EndTime", config.getSession().getEndTime());
        settings.setString(sessionId, "TimeZone", config.getSession().getTimeZone());
        
        // Data dictionary
        if (config.getSession().isUseDataDictionary()) {
            settings.setString(sessionId, "DataDictionary", config.getSession().getDataDictionary());
        }
        
        // Logging
        settings.setString(sessionId, "FileLogPath", "/var/log/fix");
        
        // Authentication
        if (config.getAuthentication().getUsername() != null) {
            settings.setString(sessionId, "Username", config.getAuthentication().getUsername());
        }
        if (config.getAuthentication().getPassword() != null) {
            settings.setString(sessionId, "Password", config.getAuthentication().getPassword());
        }
        
        return settings;
    }
    
    /**
     * Send a FIX message
     */
    public Mono<Boolean> sendMessage(Message message) {
        return Mono.fromCallable(() -> {
            if (session == null || !session.isLoggedOn()) {
                log.warn("FIX session not logged on, cannot send message");
                return false;
            }
            
            try {
                session.send(message);
                messagesSent.incrementAndGet();
                meterRegistry.counter("fix.messages.sent").increment();
                log.debug("FIX message sent successfully");
                return true;
                
            } catch (Exception e) {
                errors.incrementAndGet();
                meterRegistry.counter("fix.messages.failed").increment();
                log.error("Failed to send FIX message: {}", e.getMessage(), e);
                return false;
            }
        })
        .subscribeOn(Schedulers.boundedElastic());
    }
    
    /**
     * Get session status
     */
    public Mono<FixSessionStatus> getSessionStatus() {
        return Mono.fromCallable(() -> {
            double avgLatency = messagesSent.get() > 0 ? 
                (double) totalLatency.get() / messagesSent.get() : 0.0;
            
            return FixSessionStatus.builder()
                .sessionId(getSessionId())
                .status(connected ? (loggedOn ? "LOGGED_ON" : "CONNECTED") : "DISCONNECTED")
                .connected(connected)
                .loggedOn(loggedOn)
                .lastConnected(lastConnected)
                .lastHeartbeat(lastHeartbeat)
                .sequenceNumber(0) // Would get from session
                .messagesSent(messagesSent.get())
                .messagesReceived(messagesReceived.get())
                .errors(errors.get())
                .averageLatency(avgLatency)
                .build();
        })
        .subscribeOn(Schedulers.boundedElastic());
    }
    
    /**
     * Handle session state change
     */
    public void onSessionStateChange(SessionID sessionId, SessionState state) {
        log.info("Session state changed to: {} for session: {}", state, sessionId);
        
        // Mock implementation - in production, handle actual SessionState enum values
        String stateStr = state.toString();
        if (stateStr.contains("CONNECTED")) {
            connected = true;
            lastConnected = java.time.LocalDateTime.now();
            meterRegistry.gauge("fix.connection.status", 1);
        } else if (stateStr.contains("DISCONNECTED")) {
            connected = false;
            loggedOn = false;
            meterRegistry.gauge("fix.connection.status", 0);
            scheduleReconnect();
        } else if (stateStr.contains("LOGGED_ON")) {
            loggedOn = true;
            meterRegistry.gauge("fix.session.status", 1);
        } else if (stateStr.contains("LOGGED_OUT")) {
            loggedOn = false;
            meterRegistry.gauge("fix.session.status", 0);
        }
    }
    
    /**
     * Handle heartbeat received
     */
    public void onHeartbeat() {
        lastHeartbeat = java.time.LocalDateTime.now();
        meterRegistry.counter("fix.heartbeats.received").increment();
    }
    
    /**
     * Schedule reconnection
     */
    private void scheduleReconnect() {
        // In a real implementation, this would schedule a reconnection attempt
        log.info("Scheduling FIX session reconnection...");
    }
    
    /**
     * Get current session ID
     */
    private String getSessionId() {
        if (session != null) {
            return session.getSessionID().toString();
        }
        return "NO_SESSION";
    }
    
    /**
     * Check if session is healthy
     */
    public boolean isHealthy() {
        return connected && loggedOn && 
               (lastHeartbeat == null || lastHeartbeat.isAfter(java.time.LocalDateTime.now().minusMinutes(2)));
    }
}
