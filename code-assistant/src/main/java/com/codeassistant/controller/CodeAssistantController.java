package com.codeassistant.controller;

import com.codeassistant.model.AnalysisRequest;
import com.codeassistant.model.AnalysisResponse;
import com.codeassistant.model.SessionResponse;
import com.codeassistant.model.StreamingAnalysisResponse;
import com.codeassistant.service.CodeAnalysisService;
import com.codeassistant.service.SessionManager;
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
import com.codeassistant.model.AnalysisType;

/**
 * REST Controller for Code Assistant operations.
 * Provides endpoints for code analysis, refactoring, debugging, and code generation
 * using different AI services (OpenAI, Groq/Llama) with session management.
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
    private final SessionManager sessionManager;
    
    @Autowired
    public CodeAssistantController(CodeAnalysisService codeAnalysisService, SessionManager sessionManager) {
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
     * Create a new session
     */
    @PostMapping("/sessions")
    public ResponseEntity<SessionResponse> createSession() {
        try {
            String sessionId = sessionManager.createSession();
            SessionResponse response = SessionResponse.builder()
                .sessionId(sessionId)
                .message("Session created successfully")
                .success(true)
                .activeSessionCount(sessionManager.getActiveSessionCount())
                .build();
            
            logger.info("Created new session: {}", sessionId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error creating session", e);
            SessionResponse errorResponse = SessionResponse.builder()
                .message("Error creating session: " + e.getMessage())
                .success(false)
                .activeSessionCount(sessionManager.getActiveSessionCount())
                .build();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * Clear a specific session
     */
    @DeleteMapping("/sessions/{sessionId}")
    public ResponseEntity<SessionResponse> clearSession(@PathVariable("sessionId") String sessionId) {
        try {
            boolean cleared = sessionManager.clearSession(sessionId);
            if (cleared) {
                SessionResponse response = SessionResponse.builder()
                    .sessionId(sessionId)
                    .message("Session cleared successfully")
                    .success(true)
                    .activeSessionCount(sessionManager.getActiveSessionCount())
                    .build();
                return ResponseEntity.ok(response);
            } else {
                SessionResponse errorResponse = SessionResponse.builder()
                    .sessionId(sessionId)
                    .message("Session not found")
                    .success(false)
                    .activeSessionCount(sessionManager.getActiveSessionCount())
                    .build();
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
            }
        } catch (Exception e) {
            logger.error("Error clearing session: {}", sessionId, e);
            SessionResponse errorResponse = SessionResponse.builder()
                .sessionId(sessionId)
                .message("Error clearing session: " + e.getMessage())
                .success(false)
                .activeSessionCount(sessionManager.getActiveSessionCount())
                .build();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * Clear all sessions
     */
    @DeleteMapping("/sessions")
    public ResponseEntity<SessionResponse> clearAllSessions() {
        try {
            int clearedCount = sessionManager.clearAllSessions();
            SessionResponse response = SessionResponse.builder()
                .message("All sessions cleared successfully. Cleared " + clearedCount + " sessions")
                .success(true)
                .activeSessionCount(sessionManager.getActiveSessionCount())
                .build();
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error clearing all sessions", e);
            SessionResponse errorResponse = SessionResponse.builder()
                .message("Error clearing all sessions: " + e.getMessage())
                .success(false)
                .activeSessionCount(sessionManager.getActiveSessionCount())
                .build();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * Get session statistics
     */
    @GetMapping("/sessions/stats")
    public ResponseEntity<Map<String, Object>> getSessionStats() {
        Map<String, Object> stats = Map.of(
            "activeSessionCount", sessionManager.getActiveSessionCount(),
            "activeSessionIds", sessionManager.getActiveSessionIds(),
            "availableServices", codeAnalysisService.getAvailableServiceCount(),
            "hasServices", codeAnalysisService.hasAvailableService()
        );
        return ResponseEntity.ok(stats);
    }
    
    /**
     * Assist with specific AI service
     */
    @PostMapping("/assist/{service}")
    public ResponseEntity<AnalysisResponse> assistWithService(
            @PathVariable("service") String service,
            @Valid @RequestBody AnalysisRequest request) {
        try {
            logger.info("=== REGULAR ASSIST REQUEST RECEIVED ===");
            logger.info("Service: {}", service);
            logger.info("Session ID: {}", request.getSessionId());
            logger.info("Language: {}", request.getLanguage());
            logger.info("Analysis Type: {}", request.getAnalysisType());
            logger.info("Code Length: {}", request.getCode() != null ? request.getCode().length() : 0);
            logger.info("API Key Present: {}", request.getApiKey() != null && !request.getApiKey().isEmpty());
            
            // Validate session exists
            if (!sessionManager.sessionExists(request.getSessionId())) {
                AnalysisResponse errorResponse = AnalysisResponse.builder()
                    .analysis("Error: Session not found - " + request.getSessionId())
                    .analysisType(request.getAnalysisType())
                    .language(request.getLanguage())
                    .sessionId(request.getSessionId())
                    .success(false)
                    .build();
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
            }
            
            AnalysisResponse response = codeAnalysisService.analyzeCode(request, service);
            return ResponseEntity.ok(response);
        } catch (AIServiceException e) {
            logger.error("AI service error during assist for session: {}", request.getSessionId(), e);
            AnalysisResponse errorResponse = AnalysisResponse.builder()
                .analysis("Error: " + e.getMessage())
                .analysisType(request.getAnalysisType())
                .language(request.getLanguage())
                .sessionId(request.getSessionId())
                .success(false)
                .build();
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(errorResponse);
        } catch (Exception e) {
            logger.error("Unexpected error during assist for session: {}", request.getSessionId(), e);
            AnalysisResponse errorResponse = AnalysisResponse.builder()
                .analysis("Unexpected error: " + e.getMessage())
                .analysisType(request.getAnalysisType())
                .language(request.getLanguage())
                .sessionId(request.getSessionId())
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
            logger.info("Received streaming assist request: {} using service: {} for session: {}", 
                request.getAnalysisType(), service, request.getSessionId());
            
            // Validate session exists
            if (!sessionManager.sessionExists(request.getSessionId())) {
                StreamingAnalysisResponse errorResponse = StreamingAnalysisResponse.error(
                    "Session not found - " + request.getSessionId(), request.getAnalysisType(), request.getLanguage());
                errorResponse.setSessionId(request.getSessionId());
                emitter.send(errorResponse);
                emitter.complete();
                return ResponseEntity.ok(emitter);
            }
            
            codeAnalysisService.streamAnalysis(request, service)
                .subscribe(
                    response -> {
                        try {
                            // Ensure session ID is set in streaming response
                            response.setSessionId(request.getSessionId());
                            emitter.send(response);
                        } catch (Exception e) {
                            logger.error("Error sending SSE event for session: {}", request.getSessionId(), e);
                            emitter.completeWithError(e);
                        }
                    },
                    error -> {
                        logger.error("Error in streaming assist for session: {}", request.getSessionId(), error);
                        try {
                            StreamingAnalysisResponse errorResponse = StreamingAnalysisResponse.error(
                                error.getMessage(), request.getAnalysisType(), request.getLanguage());
                            errorResponse.setSessionId(request.getSessionId());
                            emitter.send(errorResponse);
                            emitter.complete();
                        } catch (Exception e) {
                            emitter.completeWithError(e);
                        }
                    },
                    () -> {
                        logger.debug("Streaming assist completed for session: {}", request.getSessionId());
                        emitter.complete();
                    }
                );
                
        } catch (Exception e) {
            logger.error("Error setting up streaming assist for session: {}", request.getSessionId(), e);
            try {
                StreamingAnalysisResponse errorResponse = StreamingAnalysisResponse.error(
                    e.getMessage(), request.getAnalysisType(), request.getLanguage());
                errorResponse.setSessionId(request.getSessionId());
                emitter.send(errorResponse);
                emitter.complete();
            } catch (Exception ex) {
                emitter.completeWithError(ex);
            }
        }
        
        return ResponseEntity.ok(emitter);
    }
    
    /**
     * Handle follow-up questions for an existing conversation
     */
    @PostMapping("/assist/{service}/followup")
    public ResponseEntity<AnalysisResponse> followUpWithService(
            @PathVariable("service") String service,
            @Valid @RequestBody AnalysisRequest request) {
        
        logger.info("Follow-up request received - Service: {}, Session: {}, Question: {}", 
            service, request.getSessionId(), request.getCode());
        
        // Validate session exists
        if (!sessionManager.sessionExists(request.getSessionId())) {
            logger.warn("Follow-up request with invalid session ID: {}", request.getSessionId());
            return ResponseEntity.badRequest()
                .body(AnalysisResponse.builder()
                    .success(false)
                    .analysis("Error: Invalid session ID. Please start a new conversation.")
                    .sessionId(request.getSessionId())
                    .build());
        }
        
        try {
            // For follow-ups, we use a special analysis type that indicates this is a follow-up question
            AnalysisRequest followUpRequest = new AnalysisRequest(
                request.getCode(), // Use the code field as the question content
                AnalysisType.FOLLOWUP, // Special analysis type for follow-ups
                request.getLanguage(),
                request.getSessionId(),
                request.getApiKey()
            );
            
            AnalysisResponse response = codeAnalysisService.analyzeCode(followUpRequest);
            
            logger.info("Follow-up response generated - Session: {}", request.getSessionId());
            return ResponseEntity.ok(response);
            
        } catch (AIServiceException e) {
            logger.error("AI service error during follow-up: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(AnalysisResponse.builder()
                    .success(false)
                    .analysis("Error: " + e.getMessage())
                    .sessionId(request.getSessionId())
                    .build());
        } catch (Exception e) {
            logger.error("Unexpected error during follow-up: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(AnalysisResponse.builder()
                    .success(false)
                    .analysis("Error: An unexpected error occurred")
                    .sessionId(request.getSessionId())
                    .build());
        }
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
            "services", codeAnalysisService.getAvailableServices().keySet(),
            "activeSessionCount", sessionManager.getActiveSessionCount()
        );
        return ResponseEntity.ok(stats);
    }
} 