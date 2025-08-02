package com.codeassistant.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Response model for session operations.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionResponse {
    
    @JsonProperty("sessionId")
    private String sessionId;
    
    @JsonProperty("message")
    private String message;
    
    @JsonProperty("success")
    private boolean success;
    
    @JsonProperty("timestamp")
    @Builder.Default
    private long timestamp = Instant.now().toEpochMilli();
    
    @JsonProperty("activeSessionCount")
    private int activeSessionCount;
} 