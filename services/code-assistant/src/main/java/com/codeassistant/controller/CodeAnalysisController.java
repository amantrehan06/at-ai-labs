package com.codeassistant.controller;

import com.codeassistant.model.AnalysisRequest;
import com.codeassistant.model.AnalysisResponse;
import com.codeassistant.service.CodeAnalysisService;
import com.codeassistant.service.ai.AIChatService;
import com.codeassistant.service.ai.AIServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.Map;

/**
 * REST controller for code analysis endpoints.
 * Provides endpoints for explaining, refactoring, debugging, and analyzing code.
 * Demonstrates Strategy pattern with multiple AI providers.
 */
@RestController
@RequestMapping("/api/v1/code")
@CrossOrigin(origins = "*")
public class CodeAnalysisController {
    
    private static final Logger logger = LoggerFactory.getLogger(CodeAnalysisController.class);
    
    private final CodeAnalysisService codeAnalysisService;
    
    @Autowired
    public CodeAnalysisController(CodeAnalysisService codeAnalysisService) {
        this.codeAnalysisService = codeAnalysisService;
    }
    
    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        boolean hasService = codeAnalysisService.hasAvailableService();
        int serviceCount = codeAnalysisService.getAvailableServiceCount();
        
        if (hasService) {
            return ResponseEntity.ok(String.format("Service is healthy with %d AI services available", serviceCount));
        } else {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body("Service is running but no AI services are available");
        }
    }
    
    /**
     * Generic analyze endpoint that accepts any analysis type
     */
    @PostMapping("/analyze")
    public ResponseEntity<AnalysisResponse> analyze(@Valid @RequestBody AnalysisRequest request) {
        try {
            logger.info("Received analysis request: {}", request.getAnalysisType());
            AnalysisResponse response = codeAnalysisService.analyzeCode(request);
            return ResponseEntity.ok(response);
        } catch (AIServiceException e) {
            logger.error("AI service error during analysis", e);
            AnalysisResponse errorResponse = AnalysisResponse.builder()
                .analysis("Error: " + e.getMessage())
                .analysisType(request.getAnalysisType())
                .language(request.getLanguage())
                .success(false)
                .build();
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(errorResponse);
        } catch (Exception e) {
            logger.error("Unexpected error during analysis", e);
            AnalysisResponse errorResponse = AnalysisResponse.builder()
                .analysis("Unexpected error: " + e.getMessage())
                .analysisType(request.getAnalysisType())
                .language(request.getLanguage())
                .success(false)
                .build();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * Analyze with specific AI service
     */
    @PostMapping("/analyze/{service}")
    public ResponseEntity<AnalysisResponse> analyzeWithService(
            @PathVariable String service,
            @Valid @RequestBody AnalysisRequest request) {
        try {
            logger.info("Received analysis request: {} using service: {}", request.getAnalysisType(), service);
            AnalysisResponse response = codeAnalysisService.analyzeCode(request, service);
            return ResponseEntity.ok(response);
        } catch (AIServiceException e) {
            logger.error("AI service error during analysis", e);
            AnalysisResponse errorResponse = AnalysisResponse.builder()
                .analysis("Error: " + e.getMessage())
                .analysisType(request.getAnalysisType())
                .language(request.getLanguage())
                .success(false)
                .build();
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(errorResponse);
        } catch (Exception e) {
            logger.error("Unexpected error during analysis", e);
            AnalysisResponse errorResponse = AnalysisResponse.builder()
                .analysis("Unexpected error: " + e.getMessage())
                .analysisType(request.getAnalysisType())
                .language(request.getLanguage())
                .success(false)
                .build();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * Explain code endpoint
     */
    @PostMapping("/explain")
    public ResponseEntity<AnalysisResponse> explain(@Valid @RequestBody AnalysisRequest request) {
        request.setAnalysisType(com.codeassistant.model.AnalysisType.EXPLAIN);
        return analyze(request);
    }
    
    /**
     * Refactor code endpoint
     */
    @PostMapping("/refactor")
    public ResponseEntity<AnalysisResponse> refactor(@Valid @RequestBody AnalysisRequest request) {
        request.setAnalysisType(com.codeassistant.model.AnalysisType.REFACTOR);
        return analyze(request);
    }
    
    /**
     * Debug code endpoint
     */
    @PostMapping("/debug")
    public ResponseEntity<AnalysisResponse> debug(@Valid @RequestBody AnalysisRequest request) {
        request.setAnalysisType(com.codeassistant.model.AnalysisType.DEBUG);
        return analyze(request);
    }
    
    /**
     * Get available AI services
     */
    @GetMapping("/services")
    public ResponseEntity<Map<String, String>> getAvailableServices() {
        Map<String, AIChatService> services = codeAnalysisService.getAvailableServices();
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
            "services", codeAnalysisService.getAvailableServices().keySet()
        );
        return ResponseEntity.ok(stats);
    }
} 