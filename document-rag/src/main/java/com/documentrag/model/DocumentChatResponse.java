package com.documentrag.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentChatResponse {
    
    private boolean success;
    private String message;
    private String answer;
    private String response; // Alias for answer to match UI expectations
    private String sessionId;
    private List<String> relevantDocuments;
    private List<String> sources;
    private LocalDateTime timestamp = LocalDateTime.now();
    private List<DocumentChatRequest.ChatMessage> conversationHistory;
} 