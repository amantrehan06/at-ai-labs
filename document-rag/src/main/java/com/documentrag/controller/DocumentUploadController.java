package com.documentrag.controller;

import com.documentrag.model.DocumentChatRequest;
import com.documentrag.model.DocumentChatResponse;
import com.documentrag.model.DocumentUploadResponse;
import com.documentrag.service.DocumentChatService;
import com.documentrag.service.DocumentProcessingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
            @RequestParam(value = "documentType", defaultValue = "general") String documentType,
            @RequestParam(value = "description", required = false) String description) {
        
        try {
            // Validate file
            if (file == null || file.isEmpty()) {
                DocumentUploadResponse errorResponse = new DocumentUploadResponse(false, "No file provided");
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            // Process the PDF document
            DocumentUploadResponse response = documentProcessingService.processPdfDocument(file, documentType, description);
            
            if (response.isSuccess()) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest().body(response);
            }
            
        } catch (Exception e) {
            DocumentUploadResponse errorResponse = new DocumentUploadResponse(false, "Error processing document: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
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
            DocumentChatResponse errorResponse = new DocumentChatResponse(false, "Error processing chat request: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    @GetMapping("/chat/history/{sessionId}")
    public ResponseEntity<Map<String, Object>> getChatHistory(@PathVariable String sessionId) {
        try {
            List<DocumentChatRequest.ChatMessage> history = documentChatService.getConversationHistory(sessionId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("sessionId", sessionId);
            response.put("history", history);
            response.put("count", history.size());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Error retrieving chat history: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    @DeleteMapping("/chat/history/{sessionId}")
    public ResponseEntity<Map<String, Object>> clearChatHistory(@PathVariable String sessionId) {
        try {
            documentChatService.clearConversationHistory(sessionId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Chat history cleared successfully");
            response.put("sessionId", sessionId);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Error clearing chat history: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    @GetMapping("/documents")
    public ResponseEntity<Map<String, Object>> getAllDocuments() {
        try {
            List<DocumentProcessingService.DocumentInfo> documents = documentProcessingService.getAllDocuments();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("documents", documents);
            response.put("count", documents.size());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Error retrieving documents: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    @GetMapping("/documents/{documentId}")
    public ResponseEntity<Map<String, Object>> getDocumentInfo(@PathVariable String documentId) {
        try {
            DocumentProcessingService.DocumentInfo document = documentProcessingService.getDocumentInfo(documentId);
            
            if (document == null) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("error", "Document not found");
                return ResponseEntity.notFound().build();
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("document", document);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Error retrieving document: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    @DeleteMapping("/documents/{documentId}")
    public ResponseEntity<Map<String, Object>> deleteDocument(@PathVariable String documentId) {
        try {
            DocumentProcessingService.DocumentInfo document = documentProcessingService.getDocumentInfo(documentId);
            
            if (document == null) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("error", "Document not found");
                return ResponseEntity.notFound().build();
            }
            
            documentProcessingService.deleteDocument(documentId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Document deleted successfully");
            response.put("documentId", documentId);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Error deleting document: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    @DeleteMapping("/documents")
    public ResponseEntity<Map<String, Object>> clearAllDocuments() {
        try {
            documentProcessingService.clearAllDocuments();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "All documents cleared successfully");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Error clearing documents: " + e.getMessage());
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