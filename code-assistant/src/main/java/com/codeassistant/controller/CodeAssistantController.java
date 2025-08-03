package com.codeassistant.controller;

import com.codeassistant.model.AnalysisRequest;
import com.codeassistant.model.AnalysisResponse;
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
 * 
 * The AnalysisType in the request determines the operation:
 * - WRITE_CODE, REFACTOR, DEBUG, ANALYZE: Regular code analysis operations
 * - FOLLOWUP: Follow-up questions in an ongoing conversation
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
     * Assist with specific AI service (handles both regular operations and follow-ups)
     */
    @PostMapping("/assist/{service}")
    public ResponseEntity<AnalysisResponse> assistWithService(
            @PathVariable("service") String service,
            @Valid @RequestBody AnalysisRequest request) {
        try {
            boolean isFollowUp = AnalysisType.FOLLOWUP.equals(request.getAnalysisType());
            String operationType = isFollowUp ? "FOLLOW-UP" : "REGULAR";
            
            logger.info("=== {} ASSIST REQUEST RECEIVED ===", operationType);
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
     * Streaming assist with specific AI service (handles both regular operations and follow-ups)
     */
    @PostMapping(value = "/assist/{service}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<org.springframework.web.servlet.mvc.method.annotation.SseEmitter> streamAssistWithService(
            @PathVariable("service") String service,
            @Valid @RequestBody AnalysisRequest request) {
        org.springframework.web.servlet.mvc.method.annotation.SseEmitter emitter = new org.springframework.web.servlet.mvc.method.annotation.SseEmitter();
        
        try {
            boolean isFollowUp = AnalysisType.FOLLOWUP.equals(request.getAnalysisType());
            String operationType = isFollowUp ? "FOLLOW-UP" : "REGULAR";
            
            logger.info("Received streaming {} request: {} using service: {} for session: {}", 
                operationType, request.getAnalysisType(), service, request.getSessionId());
            
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
} 