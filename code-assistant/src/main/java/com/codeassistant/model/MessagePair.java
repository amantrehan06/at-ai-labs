package com.codeassistant.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Message Pair container for SystemMessage and UserMessage.
 * Provides a clean way to return both system and user messages together.
 */
@Getter
@AllArgsConstructor
public class MessagePair {
    private final SystemMessage systemMessage;
    private final UserMessage userMessage;
    
    /**
     * Get the system message content
     */
    public String getSystemContent() {
        return systemMessage.getContent();
    }
    
    /**
     * Get the user message content
     */
    public String getUserContent() {
        return userMessage.getContent();
    }
    
    /**
     * Get the analysis type from the messages
     */
    public AnalysisType getAnalysisType() {
        return systemMessage.getAnalysisType();
    }
    
    /**
     * Get the language from the messages
     */
    public String getLanguage() {
        return systemMessage.getLanguage();
    }
} 