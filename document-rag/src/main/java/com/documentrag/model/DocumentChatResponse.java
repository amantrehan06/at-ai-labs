package com.documentrag.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocumentChatResponse {
    
    private boolean success;
    private String message;
    private String answer;
    private String sessionId;
    private List<String> relevantDocuments;
    private List<String> sources;
    private LocalDateTime timestamp = LocalDateTime.now();
    private List<DocumentChatRequest.ChatMessage> conversationHistory;
    
    public DocumentChatResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }
} 