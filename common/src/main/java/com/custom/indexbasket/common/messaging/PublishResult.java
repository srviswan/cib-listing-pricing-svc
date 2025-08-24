package com.custom.indexbasket.common.messaging;

import java.time.Instant;
import java.util.UUID;

/**
 * Represents the result of an event publishing operation.
 * Contains status information and metadata about the publishing attempt.
 */
public class PublishResult {
    
    private final String resultId;
    private final boolean success;
    private final String topic;
    private final String messageId;
    private final Instant timestamp;
    private final long latencyMs;
    private final String errorMessage;
    private final PublishStatus status;
    
    public PublishResult(String topic, String messageId, boolean success, long latencyMs) {
        this(topic, messageId, success, latencyMs, null);
    }
    
    public PublishResult(String topic, String messageId, boolean success, long latencyMs, String errorMessage) {
        this.resultId = UUID.randomUUID().toString();
        this.topic = topic;
        this.messageId = messageId;
        this.success = success;
        this.latencyMs = latencyMs;
        this.errorMessage = errorMessage;
        this.timestamp = Instant.now();
        this.status = success ? PublishStatus.SUCCESS : PublishStatus.FAILED;
    }
    
    // Getters
    public String getResultId() { return resultId; }
    public boolean isSuccess() { return success; }
    public String getTopic() { return topic; }
    public String getMessageId() { return messageId; }
    public Instant getTimestamp() { return timestamp; }
    public long getLatencyMs() { return latencyMs; }
    public String getErrorMessage() { return errorMessage; }
    public PublishStatus getStatus() { return status; }
    
    // Utility methods
    public boolean isFailed() {
        return !success;
    }
    
    public boolean hasError() {
        return errorMessage != null && !errorMessage.isEmpty();
    }
    
    public boolean isFast() {
        return latencyMs < 10; // <10ms is considered fast
    }
    
    public boolean isSlow() {
        return latencyMs > 100; // >100ms is considered slow
    }
    
    @Override
    public String toString() {
        return String.format("PublishResult{id=%s, topic=%s, success=%s, latency=%dms, status=%s}",
                resultId, topic, success, latencyMs, status);
    }
    
    public enum PublishStatus {
        SUCCESS("Successfully published"),
        FAILED("Failed to publish"),
        TIMEOUT("Publishing timed out"),
        PARTIAL("Partially published");
        
        private final String description;
        
        PublishStatus(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
}
