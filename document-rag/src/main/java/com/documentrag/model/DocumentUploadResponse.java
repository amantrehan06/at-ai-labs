package com.documentrag.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocumentUploadResponse {
    
    private boolean success;
    private String message;
    private String documentId;
    private String fileName;
    private String documentType;
    private long fileSize;
    private int segmentsProcessed;
    private LocalDateTime uploadedAt = LocalDateTime.now();
    private Map<String, Object> metadata;
} 