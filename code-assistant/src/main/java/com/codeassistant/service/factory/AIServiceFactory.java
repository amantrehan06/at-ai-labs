package com.codeassistant.service.factory;

import com.codeassistant.service.ai.AIChatService;
import com.codeassistant.service.ai.AIServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Factory class for AI services that demonstrates the Strategy pattern.
 * This allows dynamic selection of AI providers based on availability and preferences.
 */
@Component
public class AIServiceFactory {
    
    private static final Logger logger = LoggerFactory.getLogger(AIServiceFactory.class);
    
    private final Map<String, AIChatService> availableServices;
    
    @Autowired
    public AIServiceFactory(List<AIChatService> services) {
        this.availableServices = services.stream()
            .filter(AIChatService::isAvailable)
            .collect(Collectors.toMap(
                service -> service.getClass().getSimpleName(),
                service -> service
            ));
        
        logger.info("AIServiceFactory initialized with {} available services: {}", 
            availableServices.size(), 
            availableServices.keySet());
    }
    
    /**
     * Gets a specific AI service by name.
     * 
     * @param serviceName The name of the service (e.g., "OpenAIChatService", "GroqAIChatService")
     * @return The requested AI service
     * @throws AIServiceException if the service is not available
     */
    public AIChatService getService(String serviceName) throws AIServiceException {
        AIChatService service = availableServices.get(serviceName);
        if (service == null) {
            throw new AIServiceException("Service '" + serviceName + "' is not available. Available services: " + 
                availableServices.keySet());
        }
        return service;
    }
    
    /**
     * Gets all available services.
     * 
     * @return Map of service names to services
     */
    public Map<String, AIChatService> getAllAvailableServices() {
        return availableServices;
    }
    
    /**
     * Checks if any services are available.
     * 
     * @return true if at least one service is available
     */
    public boolean hasAvailableServices() {
        return !availableServices.isEmpty();
    }
    
    /**
     * Gets the number of available services.
     * 
     * @return Number of available services
     */
    public int getAvailableServiceCount() {
        return availableServices.size();
    }
} 