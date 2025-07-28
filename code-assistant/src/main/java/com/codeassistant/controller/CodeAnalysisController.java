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
     * Streaming analyze endpoint that provides Server-Sent Events (SSE)
     */
    @PostMapping(value = "/analyze/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<org.springframework.web.servlet.mvc.method.annotation.SseEmitter> streamAnalyze(@Valid @RequestBody AnalysisRequest request) {
        org.springframework.web.servlet.mvc.method.annotation.SseEmitter emitter = new org.springframework.web.servlet.mvc.method.annotation.SseEmitter();
        
        try {
            logger.info("Received streaming analysis request: {}", request.getAnalysisType());
            
            codeAnalysisService.streamAnalysis(request)
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
                        logger.error("Error in streaming analysis", error);
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
                        logger.debug("Streaming analysis completed");
                        emitter.complete();
                    }
                );
                
        } catch (Exception e) {
            logger.error("Error setting up streaming analysis", e);
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
     * Analyze with specific AI service
     */
    @PostMapping("/analyze/{service}")
    public ResponseEntity<AnalysisResponse> analyzeWithService(
            @PathVariable("service") String service,
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
     * Streaming analyze with specific AI service
     */
    @PostMapping(value = "/analyze/{service}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<org.springframework.web.servlet.mvc.method.annotation.SseEmitter> streamAnalyzeWithService(
            @PathVariable("service") String service,
            @Valid @RequestBody AnalysisRequest request) {
        org.springframework.web.servlet.mvc.method.annotation.SseEmitter emitter = new org.springframework.web.servlet.mvc.method.annotation.SseEmitter();
        
        try {
            logger.info("Received streaming analysis request: {} using service: {}", request.getAnalysisType(), service);
            
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
                        logger.error("Error in streaming analysis", error);
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
                        logger.debug("Streaming analysis completed");
                        emitter.complete();
                    }
                );
                
        } catch (Exception e) {
            logger.error("Error setting up streaming analysis", e);
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
     * Explain code endpoint
     */
    @PostMapping("/explain")
    public ResponseEntity<AnalysisResponse> explain(@Valid @RequestBody AnalysisRequest request) {
        request.setAnalysisType(com.codeassistant.model.AnalysisType.EXPLAIN);
        return analyze(request);
    }
    
    /**
     * Streaming explain code endpoint
     */
    @PostMapping(value = "/explain/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<org.springframework.web.servlet.mvc.method.annotation.SseEmitter> streamExplain(@Valid @RequestBody AnalysisRequest request) {
        request.setAnalysisType(com.codeassistant.model.AnalysisType.EXPLAIN);
        return streamAnalyze(request);
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
     * Streaming refactor code endpoint
     */
    @PostMapping(value = "/refactor/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<org.springframework.web.servlet.mvc.method.annotation.SseEmitter> streamRefactor(@Valid @RequestBody AnalysisRequest request) {
        request.setAnalysisType(com.codeassistant.model.AnalysisType.REFACTOR);
        return streamAnalyze(request);
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
     * Streaming debug code endpoint
     */
    @PostMapping(value = "/debug/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<org.springframework.web.servlet.mvc.method.annotation.SseEmitter> streamDebug(@Valid @RequestBody AnalysisRequest request) {
        request.setAnalysisType(com.codeassistant.model.AnalysisType.DEBUG);
        return streamAnalyze(request);
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