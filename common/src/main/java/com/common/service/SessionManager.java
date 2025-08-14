package com.common.service;

import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for managing chat sessions and their associated memory.
 * Provides session creation, retrieval, and cleanup functionality.
 * Sessions are stored in memory without expiration - cleanup is manual only.
 */
@Service
public class SessionManager {
    
    private static final Logger logger = LoggerFactory.getLogger(SessionManager.class);
    
    // Store sessions in memory with their associated chat memory
    private final Map<String, ChatMemory> sessions = new ConcurrentHashMap<>();
    
    /**
     * Creates a new session with a unique session ID.
     * 
     * @return The generated session ID
     */
    public String createSession() {
        String sessionId = UUID.randomUUID().toString();
        ChatMemory chatMemory = MessageWindowChatMemory.withMaxMessages(10); // Keep last 10 messages
        sessions.put(sessionId, chatMemory);
        
        logger.info("Created new session: {}", sessionId);
        return sessionId;
    }
    
    /**
     * Retrieves the chat memory for a given session ID.
     * 
     * @param sessionId The session ID
     * @return The chat memory for the session, or null if session doesn't exist
     */
    public ChatMemory getSessionMemory(String sessionId) {
        ChatMemory memory = sessions.get(sessionId);
        if (memory == null) {
            logger.warn("Session not found: {}", sessionId);
        } else {
            logger.info("Session memory retrieved - ID: {}, Messages: {}", sessionId, memory.messages().size());
        }
        return memory;
    }
    
    /**
     * Checks if a session exists.
     * 
     * @param sessionId The session ID
     * @return true if session exists, false otherwise
     */
    public boolean sessionExists(String sessionId) {
        return sessions.containsKey(sessionId);
    }
    
    /**
     * Clears a specific session.
     * 
     * @param sessionId The session ID to clear
     * @return true if session was cleared, false if session didn't exist
     */
    public boolean clearSession(String sessionId) {
        ChatMemory removed = sessions.remove(sessionId);
        if (removed != null) {
            logger.info("Cleared session: {}", sessionId);
            return true;
        } else {
            logger.warn("Attempted to clear non-existent session: {}", sessionId);
            return false;
        }
    }
    
    /**
     * Clears all sessions.
     * 
     * @return The number of sessions that were cleared
     */
    public int clearAllSessions() {
        int sessionCount = sessions.size();
        sessions.clear();
        logger.info("Cleared all {} sessions", sessionCount);
        return sessionCount;
    }
    
    /**
     * Gets the total number of active sessions.
     * 
     * @return The number of active sessions
     */
    public int getActiveSessionCount() {
        return sessions.size();
    }
    
    /**
     * Gets all active session IDs.
     * 
     * @return Set of active session IDs
     */
    public java.util.Set<String> getActiveSessionIds() {
        return sessions.keySet();
    }
} 