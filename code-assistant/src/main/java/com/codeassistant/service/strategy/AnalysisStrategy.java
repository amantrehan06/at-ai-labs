package com.codeassistant.service.strategy;

import com.codeassistant.model.AnalysisRequest;
import com.codeassistant.model.MessagePair;

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
     * Builds both system and user messages for this specific analysis type.
     * This is the preferred method for new implementations.
     * 
     * @param request The analysis request
     * @return MessagePair containing system and user messages
     */
    MessagePair buildMessages(AnalysisRequest request);
    
    /**
     * Gets a human-readable description of what this analysis does.
     * 
     * @return Description of the analysis type
     */
    String getDescription();
} 