package com.codeassistant.service.ai;

import com.codeassistant.model.AnalysisRequest;
import com.codeassistant.model.AnalysisResponse;
import com.codeassistant.model.StreamingAnalysisResponse;
import reactor.core.publisher.Flux;

/**
 * Strategy interface for AI chat services.
 * This allows us to easily switch between different AI providers.
 */
public interface AIChatService {
    
    /**
     * Analyzes code using AI and returns a response.
     * 
     * @param request The analysis request containing code and parameters
     * @return AnalysisResponse with the AI-generated analysis
     * @throws AIServiceException if the AI service fails
     */
    AnalysisResponse analyzeCode(AnalysisRequest request) throws AIServiceException;
    
    /**
     * Streams code analysis using AI and returns a reactive stream of responses.
     * 
     * @param request The analysis request containing code and parameters
     * @return Flux of StreamingAnalysisResponse for real-time streaming
     * @throws AIServiceException if the AI service fails
     */
    Flux<StreamingAnalysisResponse> streamAnalysis(AnalysisRequest request) throws AIServiceException;
    
    /**
     * Checks if the service is properly configured and available.
     * 
     * @return true if the service is available, false otherwise
     */
    boolean isAvailable();
} 