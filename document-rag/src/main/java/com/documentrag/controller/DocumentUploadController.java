package com.documentrag.controller;

import com.documentrag.model.DocumentChatRequest;
import com.documentrag.model.DocumentChatResponse;
import com.documentrag.model.DocumentUploadResponse;
import com.documentrag.service.DocumentChatService;
import com.documentrag.service.DocumentProcessingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.HttpStatus;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/document-rag")
@CrossOrigin(origins = "*")
public class DocumentUploadController {
    
    @Autowired
    private DocumentProcessingService documentProcessingService;
    
    @Autowired
    private DocumentChatService documentChatService;
    
    @PostMapping("/upload")
    public ResponseEntity<DocumentUploadResponse> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam("sessionId") String sessionId) {
        try {
            // Debug: Log file information
            log.info("File upload received:");
            log.info("  - Original filename: {}", file.getOriginalFilename());
            log.info("  - Content type: {}", file.getContentType());
            log.info("  - File size: {}", file.getSize());
            log.info("  - Is empty: {}", file.isEmpty());
            log.info("  - Session ID: {}", sessionId);
            
            // Check if it's a PDF file
            boolean isPdfFile = (file.getContentType() != null && file.getContentType().equals("application/pdf")) ||
                               (file.getOriginalFilename() != null && file.getOriginalFilename().toLowerCase().endsWith(".pdf"));
            
            log.info("  - Is PDF file: {}", isPdfFile);
            
            if (isPdfFile) {
                log.info("Processing as PDF document with session ID: {}", sessionId);
                DocumentUploadResponse response = documentProcessingService.processPdfDocument(file, sessionId);
                return ResponseEntity.ok(response);
            } else {
                log.warn("Non-PDF file rejected: {}", file.getOriginalFilename());
                DocumentUploadResponse errorResponse = new DocumentUploadResponse();
                errorResponse.setSuccess(false);
                errorResponse.setMessage("Only PDF documents are supported. Please upload a valid PDF file.");
                return ResponseEntity.badRequest().body(errorResponse);
            }
        } catch (Exception e) {
            log.error("Error in uploadDocument: {}", e.getMessage(), e);
            DocumentUploadResponse errorResponse = new DocumentUploadResponse();
            errorResponse.setSuccess(false);
            errorResponse.setMessage("Error uploading document: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    @PostMapping("/chat")
    public ResponseEntity<DocumentChatResponse> chatWithDocuments(@RequestBody DocumentChatRequest request) {
        try {
            DocumentChatResponse response = documentChatService.chatWithDocuments(request);
            
            if (response.isSuccess()) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest().body(response);
            }
            
        } catch (Exception e) {
            log.error("Error processing chat request: {}", e.getMessage(), e);
            DocumentChatResponse errorResponse = new DocumentChatResponse(false, "Error processing chat request: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", "document-rag");
        health.put("timestamp", java.time.LocalDateTime.now().toString());
        return ResponseEntity.ok(health);
    }
} 