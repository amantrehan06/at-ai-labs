package com.codeassistant.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import dev.langchain4j.data.message.ChatMessage;

import java.util.List;

/**
 * Message Pair container for SystemMessage and UserMessage.
 * Provides a clean way to return both system and user messages together.
 */
@Data
@AllArgsConstructor
public class MessagePair {
    private final SystemMessage systemMessage;
    private final UserMessage userMessage;
    
    /**
     * Convert to LangChain4j ChatMessage list
     */
    public List<ChatMessage> toLangChain4jMessages() {
        return List.of(
            new dev.langchain4j.data.message.SystemMessage(systemMessage.getContent()),
            new dev.langchain4j.data.message.UserMessage(userMessage.getContent())
        );
    }
} 