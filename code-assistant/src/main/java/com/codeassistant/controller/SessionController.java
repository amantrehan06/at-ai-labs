package com.codeassistant.controller;

import com.codeassistant.model.SessionResponse;
import com.codeassistant.service.SessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST Controller for session management operations.
 * Provides endpoints for creating, clearing, and managing chat sessions.
 */
@RestController
@RequestMapping("/api/v1/code")
@CrossOrigin(origins = "*")
public class SessionController {
    
    private static final Logger logger = LoggerFactory.getLogger(SessionController.class);
    
    private final SessionManager sessionManager;
    
    @Autowired
    public SessionController(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
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
            "activeSessionIds", sessionManager.getActiveSessionIds()
        );
        return ResponseEntity.ok(stats);
    }
} 