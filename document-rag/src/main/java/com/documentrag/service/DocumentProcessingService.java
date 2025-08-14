package com.documentrag.service;

import com.documentrag.model.DocumentUploadResponse;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class DocumentProcessingService {
    
    @Autowired
    private DocumentChatService documentChatService;
    
    // In-memory document store
    private final ConcurrentHashMap<String, DocumentInfo> documentStore = new ConcurrentHashMap<>();
    private final DocumentSplitter documentSplitter = DocumentSplitters.recursive(800, 100);
    
    public DocumentUploadResponse processPdfDocument(MultipartFile file, String sessionId) {
        DocumentUploadResponse response = new DocumentUploadResponse();
        
        try {
            // Validate file
            if (!isValidPdfFile(file)) {
                response.setSuccess(false);
                response.setMessage("Invalid PDF file. Please upload a valid PDF document.");
                return response;
            }
            
            // Generate document ID
            String documentId = UUID.randomUUID().toString();
            
            // Get file details
            String fileName = file.getOriginalFilename();
            
            // Extract text from PDF
            String extractedText = extractTextFromPdf(file);
            if (extractedText == null || extractedText.trim().isEmpty()) {
                response.setSuccess(false);
                response.setMessage("Could not extract text from PDF. The document may be empty or contain only images.");
                return response;
            }
            
            // Create document with comprehensive metadata including sessionId for hybrid search
            dev.langchain4j.data.document.Metadata metadata = dev.langchain4j.data.document.Metadata.from("type", "pdf");
            metadata.add("documentId", documentId);
            metadata.add("sessionId", sessionId); // Add sessionId for hybrid search
            metadata.add("fileName", fileName);
            metadata.add("fileSize", String.valueOf(file.getSize()));
            metadata.add("uploadedAt", String.valueOf(System.currentTimeMillis()));
            metadata.add("contentType", "pdf");
            metadata.add("source", "upload");
            
            Document document = Document.from(extractedText, metadata);
            
            // Split document into segments
            List<TextSegment> segments = documentSplitter.split(document);
            
            // Add document to vector store for chat functionality
            documentChatService.addDocumentToVectorStore(documentId, segments, "pdf");
            
            // Store document info
            DocumentInfo docInfo = new DocumentInfo(
                documentId,
                fileName,
                "pdf",
                "PDF document",
                file.getSize(),
                segments.size(),
                segments.size(), // All segments processed
                extractedText
            );
            documentStore.put(documentId, docInfo);
            
            // Build response
            response.setSuccess(true);
            response.setMessage("PDF document uploaded successfully! You can now ask questions about your document.");
            response.setDocumentId(documentId);
            response.setFileName(fileName);
            response.setDocumentType("pdf");
            response.setFileSize(file.getSize());
            response.setSegmentsProcessed(segments.size());
            
            // Add metadata
            Map<String, Object> metadataMap = new HashMap<>();
            metadataMap.put("totalSegments", segments.size());
            metadataMap.put("processedSegments", segments.size());
            metadataMap.put("documentType", "pdf");
            metadataMap.put("description", "PDF document");
            metadataMap.put("vectorStore", "Pinecone");
            metadataMap.put("textLength", extractedText.length());
            metadataMap.put("averageSegmentLength", segments.isEmpty() ? 0 : extractedText.length() / segments.size());
            metadataMap.put("sessionId", sessionId); // Include sessionId in response metadata
            response.setMetadata(metadataMap);
            
            // Log successful PDF processing
            log.info("PDF document processed successfully - ID: {}, Session: {}, Name: {}, Text Length: {}, Segments: {}", 
                    documentId, sessionId, fileName, extractedText.length(), segments.size());
            
        } catch (Exception e) {
            log.error("Error processing PDF document: {}", e.getMessage(), e);
            response.setSuccess(false);
            response.setMessage("Error processing PDF document: " + e.getMessage());
        }
        
        return response;
    }
    
    private boolean isValidPdfFile(MultipartFile file) {
        return file != null && 
               !file.isEmpty() && 
               file.getContentType() != null && 
               file.getContentType().equals("application/pdf") &&
               file.getSize() > 0 && 
               file.getSize() <= 50 * 1024 * 1024; // 50MB limit for PDF files
    }
    
    private String extractTextFromPdf(MultipartFile file) throws IOException {
        try (org.apache.pdfbox.pdmodel.PDDocument document = org.apache.pdfbox.pdmodel.PDDocument.load(file.getInputStream())) {
            org.apache.pdfbox.text.PDFTextStripper stripper = new org.apache.pdfbox.text.PDFTextStripper();
            stripper.setSortByPosition(true);
            return stripper.getText(document);
        }
    }
    
    // Inner class to store document information
    public static class DocumentInfo {
        private final String documentId;
        private final String fileName;
        private final String documentType;
        private final String description;
        private final long fileSize;
        private final int totalSegments;
        private final int processedSegments;
        private final String content;
        private final long uploadedAt;
        
        public DocumentInfo(String documentId, String fileName, String documentType, 
                          String description, long fileSize, int totalSegments, 
                          int processedSegments, String content) {
            this.documentId = documentId;
            this.fileName = fileName;
            this.documentType = documentType;
            this.description = description;
            this.fileSize = fileSize;
            this.totalSegments = totalSegments;
            this.processedSegments = processedSegments;
            this.content = content;
            this.uploadedAt = System.currentTimeMillis();
        }
        
        // Getters
        public String getDocumentId() { return documentId; }
        public String getFileName() { return fileName; }
        public String getDocumentType() { return documentType; }
        public String getDescription() { return description; }
        public long getFileSize() { return fileSize; }
        public int getTotalSegments() { return totalSegments; }
        public int getProcessedSegments() { return processedSegments; }
        public String getContent() { return content; }
        public long getUploadedAt() { return uploadedAt; }
    }
} 