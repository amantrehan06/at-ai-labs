package com.codeassistant.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request model for code analysis.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnalysisRequest {
    
    @NotBlank(message = "Code cannot be blank")
    @JsonProperty("code")
    private String code;
    
    @NotNull(message = "Analysis type cannot be null")
    @JsonProperty("analysisType")
    private AnalysisType analysisType;
    
    @NotBlank(message = "Language cannot be blank")
    @JsonProperty("language")
    private String language;
    
    @NotBlank(message = "Session ID cannot be blank")
    @JsonProperty("sessionId")
    private String sessionId;
} 