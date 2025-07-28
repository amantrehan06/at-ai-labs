package com.codeassistant.service;

import com.codeassistant.model.AnalysisRequest;
import com.codeassistant.model.AnalysisResponse;
import com.codeassistant.service.ai.AIChatService;
import com.codeassistant.service.ai.AIServiceException;
import com.codeassistant.service.factory.AIServiceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Main service for code analysis that uses the Strategy pattern with Factory pattern.
 * This service acts as a facade and can work with different AI providers dynamically.
 */
@Service
public class CodeAnalysisService {
    
    private static final Logger logger = LoggerFactory.getLogger(CodeAnalysisService.class);
    
    private final AIServiceFactory aiServiceFactory;
    
    @Autowired
    public CodeAnalysisService(AIServiceFactory aiServiceFactory) {
        this.aiServiceFactory = aiServiceFactory;
        logger.info("CodeAnalysisService initialized with AI service factory");
    }
    
    /**
     * Analyzes code using the best available AI service.
     * 
     * @param request The analysis request
     * @return AnalysisResponse with the results
     * @throws AIServiceException if no AI service is available
     */
    public AnalysisResponse analyzeCode(AnalysisRequest request) throws AIServiceException {
        logger.debug("Analyzing code with request: {}", request);
        
        try {
            AIChatService service = aiServiceFactory.getBestAvailableService();
            logger.info("Using AI service: {}", service.getClass().getSimpleName());
            
            AnalysisResponse response = service.analyzeCode(request);
            logger.debug("Analysis completed successfully");
            return response;
        } catch (AIServiceException e) {
            logger.error("AI service failed to analyze code", e);
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error during code analysis", e);
            throw new AIServiceException("Unexpected error during code analysis: " + e.getMessage(), e);
        }
    }
    
    /**
     * Analyzes code using a specific AI service.
     * 
     * @param request The analysis request
     * @param serviceName The name of the service to use (e.g., "OpenAIChatService", "LlamaAIChatService")
     * @return AnalysisResponse with the results
     * @throws AIServiceException if the service is not available
     */
    public AnalysisResponse analyzeCode(AnalysisRequest request, String serviceName) throws AIServiceException {
        logger.debug("Analyzing code with request: {} using service: {}", request, serviceName);
        
        try {
            AIChatService service = aiServiceFactory.getService(serviceName);
            logger.info("Using specific AI service: {}", service.getClass().getSimpleName());
            
            AnalysisResponse response = service.analyzeCode(request);
            logger.debug("Analysis completed successfully");
            return response;
        } catch (AIServiceException e) {
            logger.error("AI service failed to analyze code", e);
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error during code analysis", e);
            throw new AIServiceException("Unexpected error during code analysis: " + e.getMessage(), e);
        }
    }
    
    /**
     * Gets the list of available AI services.
     * 
     * @return Map of available AI chat services
     */
    public java.util.Map<String, AIChatService> getAvailableServices() {
        return aiServiceFactory.getAllAvailableServices();
    }
    
    /**
     * Checks if any AI service is available.
     * 
     * @return true if at least one AI service is available
     */
    public boolean hasAvailableService() {
        return aiServiceFactory.hasAvailableServices();
    }
    
    /**
     * Gets the number of available services.
     * 
     * @return Number of available services
     */
    public int getAvailableServiceCount() {
        return aiServiceFactory.getAvailableServiceCount();
    }
} 