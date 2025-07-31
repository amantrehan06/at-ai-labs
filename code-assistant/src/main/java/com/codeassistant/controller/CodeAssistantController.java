package com.codeassistant.controller;

import com.codeassistant.model.AnalysisRequest;
import com.codeassistant.model.AnalysisResponse;
import com.codeassistant.model.StreamingAnalysisResponse;
import com.codeassistant.service.CodeAnalysisService;
import com.codeassistant.service.ai.AIChatService;
import com.codeassistant.service.ai.AIServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.Map;

/**
 * REST Controller for Code Assistant operations.
 * Provides endpoints for code analysis, refactoring, debugging, and code generation
 * using different AI services (OpenAI, Groq/Llama).
 * 
 * All endpoints require a specific service to be specified in the URL path.
 * Supported services: OpenAIChatService, GroqAIChatService
 */
@RestController
@RequestMapping("/api/v1/code")
@CrossOrigin(origins = "*")
public class CodeAssistantController {
    
    private static final Logger logger = LoggerFactory.getLogger(CodeAssistantController.class);
    
    private final CodeAnalysisService codeAnalysisService;
    
    @Autowired
    public CodeAssistantController(CodeAnalysisService codeAnalysisService) {
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
     * Assist with specific AI service
     */
    @PostMapping("/assist/{service}")
    public ResponseEntity<AnalysisResponse> assistWithService(
            @PathVariable("service") String service,
            @Valid @RequestBody AnalysisRequest request) {
        try {
            logger.info("Received assist request: {} using service: {}", request.getAnalysisType(), service);
            AnalysisResponse response = codeAnalysisService.analyzeCode(request, service);
            return ResponseEntity.ok(response);
        } catch (AIServiceException e) {
            logger.error("AI service error during assist", e);
            AnalysisResponse errorResponse = AnalysisResponse.builder()
                .analysis("Error: " + e.getMessage())
                .analysisType(request.getAnalysisType())
                .language(request.getLanguage())
                .success(false)
                .build();
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(errorResponse);
        } catch (Exception e) {
            logger.error("Unexpected error during assist", e);
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
     * Streaming assist with specific AI service
     */
    @PostMapping(value = "/assist/{service}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<org.springframework.web.servlet.mvc.method.annotation.SseEmitter> streamAssistWithService(
            @PathVariable("service") String service,
            @Valid @RequestBody AnalysisRequest request) {
        org.springframework.web.servlet.mvc.method.annotation.SseEmitter emitter = new org.springframework.web.servlet.mvc.method.annotation.SseEmitter();
        
        try {
            logger.info("Received streaming assist request: {} using service: {}", request.getAnalysisType(), service);
            
            codeAnalysisService.streamAnalysis(request, service)
                .subscribe(
                    response -> {
                        try {
                            emitter.send(response);
                        } catch (Exception e) {
                            logger.error("Error sending SSE event", e);
                            emitter.completeWithError(e);
                        }
                    },
                    error -> {
                        logger.error("Error in streaming assist", error);
                        try {
                            StreamingAnalysisResponse errorResponse = StreamingAnalysisResponse.error(
                                error.getMessage(), request.getAnalysisType(), request.getLanguage());
                            emitter.send(errorResponse);
                            emitter.complete();
                        } catch (Exception e) {
                            emitter.completeWithError(e);
                        }
                    },
                    () -> {
                        logger.debug("Streaming assist completed");
                        emitter.complete();
                    }
                );
                
        } catch (Exception e) {
            logger.error("Error setting up streaming assist", e);
            try {
                StreamingAnalysisResponse errorResponse = StreamingAnalysisResponse.error(
                    e.getMessage(), request.getAnalysisType(), request.getLanguage());
                emitter.send(errorResponse);
                emitter.complete();
            } catch (Exception ex) {
                emitter.completeWithError(ex);
            }
        }
        
        return ResponseEntity.ok(emitter);
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