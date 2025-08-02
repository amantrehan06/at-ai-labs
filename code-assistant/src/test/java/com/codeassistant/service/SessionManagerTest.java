package com.codeassistant.service;

import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for SessionManager.
 */
public class SessionManagerTest {
    
    private SessionManager sessionManager;
    
    @BeforeEach
    public void setUp() {
        sessionManager = new SessionManager();
    }
    
    @Test
    public void testSessionCreation() {
        // Create a new session
        String sessionId = sessionManager.createSession();
        
        // Verify session was created
        assertNotNull(sessionId);
        assertTrue(sessionManager.sessionExists(sessionId));
        assertEquals(1, sessionManager.getActiveSessionCount());
        
        // Verify session memory is available
        ChatMemory memory = sessionManager.getSessionMemory(sessionId);
        assertNotNull(memory);
        assertTrue(memory instanceof MessageWindowChatMemory);
    }
    
    @Test
    public void testSessionClearance() {
        // Create multiple sessions
        String session1 = sessionManager.createSession();
        String session2 = sessionManager.createSession();
        
        assertEquals(2, sessionManager.getActiveSessionCount());
        
        // Clear specific session
        boolean cleared = sessionManager.clearSession(session1);
        assertTrue(cleared);
        assertFalse(sessionManager.sessionExists(session1));
        assertTrue(sessionManager.sessionExists(session2));
        assertEquals(1, sessionManager.getActiveSessionCount());
        
        // Clear all sessions
        int clearedCount = sessionManager.clearAllSessions();
        assertEquals(1, clearedCount);
        assertEquals(0, sessionManager.getActiveSessionCount());
    }
    
    @Test
    public void testNonExistentSession() {
        // Try to access non-existent session
        assertFalse(sessionManager.sessionExists("non-existent-session"));
        assertNull(sessionManager.getSessionMemory("non-existent-session"));
        
        // Try to clear non-existent session
        boolean cleared = sessionManager.clearSession("non-existent-session");
        assertFalse(cleared);
    }
    
    @Test
    public void testSessionIdUniqueness() {
        // Create multiple sessions and verify they have unique IDs
        String session1 = sessionManager.createSession();
        String session2 = sessionManager.createSession();
        String session3 = sessionManager.createSession();
        
        assertNotEquals(session1, session2);
        assertNotEquals(session1, session3);
        assertNotEquals(session2, session3);
        
        // Verify all sessions exist
        assertTrue(sessionManager.sessionExists(session1));
        assertTrue(sessionManager.sessionExists(session2));
        assertTrue(sessionManager.sessionExists(session3));
        
        assertEquals(3, sessionManager.getActiveSessionCount());
    }
    
    @Test
    public void testGetActiveSessionIds() {
        // Create sessions
        String session1 = sessionManager.createSession();
        String session2 = sessionManager.createSession();
        
        var activeSessionIds = sessionManager.getActiveSessionIds();
        assertEquals(2, activeSessionIds.size());
        assertTrue(activeSessionIds.contains(session1));
        assertTrue(activeSessionIds.contains(session2));
        
        // Clear one session
        sessionManager.clearSession(session1);
        activeSessionIds = sessionManager.getActiveSessionIds();
        assertEquals(1, activeSessionIds.size());
        assertFalse(activeSessionIds.contains(session1));
        assertTrue(activeSessionIds.contains(session2));
    }
} 