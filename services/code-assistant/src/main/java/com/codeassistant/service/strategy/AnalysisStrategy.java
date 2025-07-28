package com.codeassistant.service.strategy;

import com.codeassistant.model.AnalysisRequest;

/**
 * Strategy interface for different types of code analysis.
 * This allows us to easily add new analysis types without modifying existing code.
 */
public interface AnalysisStrategy {
    
    /**
     * Gets the type of analysis this strategy handles.
     * 
     * @return The analysis type
     */
    com.codeassistant.model.AnalysisType getAnalysisType();
    
    /**
     * Builds the prompt for this specific analysis type.
     * 
     * @param request The analysis request
     * @return The formatted prompt for the AI service
     */
    String buildPrompt(AnalysisRequest request);
    
    /**
     * Gets a human-readable description of what this analysis does.
     * 
     * @return Description of the analysis type
     */
    String getDescription();
} 