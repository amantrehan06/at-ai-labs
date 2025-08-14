package com.documentrag.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class DocumentChatRequest {
    
    private String message;
    private String sessionId;
    private String service; // AI service to use (OpenAIChatService, GroqAIChatService)
    private List<ChatMessage> conversationHistory;
    
    @Data
    @NoArgsConstructor
    public static class ChatMessage {
        private String role; // "user" or "assistant"
        private String content;
        private String timestamp;
    }
} 