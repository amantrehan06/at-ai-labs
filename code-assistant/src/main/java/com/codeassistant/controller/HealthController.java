package com.codeassistant.controller;

import com.codeassistant.service.CodeAnalysisService;
import com.codeassistant.service.SessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST Controller for health checks and service statistics.
 * Provides endpoints for monitoring service health and available services.
 */
@RestController
@RequestMapping("/api/v1/code")
@CrossOrigin(origins = "*")
public class HealthController {
    
    private static final Logger logger = LoggerFactory.getLogger(HealthController.class);
    
    private final CodeAnalysisService codeAnalysisService;
    private final SessionManager sessionManager;
    
    @Autowired
    public HealthController(CodeAnalysisService codeAnalysisService, SessionManager sessionManager) {
        this.codeAnalysisService = codeAnalysisService;
        this.sessionManager = sessionManager;
    }
    
    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        boolean hasService = codeAnalysisService.hasAvailableService();
        int serviceCount = codeAnalysisService.getAvailableServiceCount();
        int sessionCount = sessionManager.getActiveSessionCount();
        
        if (hasService) {
            return ResponseEntity.ok(String.format("Service is healthy with %d AI services available and %d active sessions", 
                serviceCount, sessionCount));
        } else {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body("Service is running but no AI services are available");
        }
    }
    
    /**
     * Get available AI services
     */
    @GetMapping("/services")
    public ResponseEntity<Map<String, String>> getAvailableServices() {
        Map<String, com.codeassistant.service.ai.AIChatService> services = codeAnalysisService.getAvailableServices();
        Map<String, String> serviceInfo = services.entrySet().stream()
            .collect(java.util.stream.Collectors.toMap(
                Map.Entry::getKey,
                entry -> entry.getValue().getClass().getSimpleName()
            ));
        return ResponseEntity.ok(serviceInfo);
    }
    
    /**
     * Get service statistics
     */
    @GetMapping("/services/stats")
    public ResponseEntity<Map<String, Object>> getServiceStats() {
        Map<String, Object> stats = Map.of(
            "availableServices", codeAnalysisService.getAvailableServiceCount(),
            "hasServices", codeAnalysisService.hasAvailableService(),
            "services", codeAnalysisService.getAvailableServices().keySet(),
            "activeSessionCount", sessionManager.getActiveSessionCount()
        );
        return ResponseEntity.ok(stats);
    }
} 