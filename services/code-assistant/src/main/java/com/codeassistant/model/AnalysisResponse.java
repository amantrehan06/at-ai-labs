package com.codeassistant.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Response model for code analysis results.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalysisResponse {
    
    @JsonProperty("analysis")
    private String analysis;
    
    @JsonProperty("analysisType")
    private AnalysisType analysisType;
    
    @JsonProperty("language")
    private String language;
    
    @JsonProperty("success")
    private boolean success;
    
    @JsonProperty("timestamp")
    @Builder.Default
    private long timestamp = Instant.now().toEpochMilli();
} 