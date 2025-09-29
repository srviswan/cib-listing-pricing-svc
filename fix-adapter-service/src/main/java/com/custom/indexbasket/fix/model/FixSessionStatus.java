package com.custom.indexbasket.fix.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * FIX Session Status Model
 * 
 * Represents the status of a FIX session connection.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FixSessionStatus {
    
    private String sessionId;
    private String status;
    private boolean connected;
    private boolean loggedOn;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    private LocalDateTime lastConnected;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    private LocalDateTime lastDisconnected;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    private LocalDateTime lastHeartbeat;
    
    private int sequenceNumber;
    private int messagesSent;
    private int messagesReceived;
    private int errors;
    private double averageLatency;
    
    private Map<String, Object> additionalInfo;
    
    /**
     * Check if the session is healthy
     */
    public boolean isHealthy() {
        return connected && loggedOn && 
               (lastHeartbeat == null || lastHeartbeat.isAfter(LocalDateTime.now().minusMinutes(2)));
    }
    
    /**
     * Get the connection uptime in seconds
     */
    public long getUptimeSeconds() {
        if (lastConnected == null) {
            return 0;
        }
        return java.time.Duration.between(lastConnected, LocalDateTime.now()).getSeconds();
    }
}
