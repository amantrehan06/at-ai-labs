package com.documentrag.service;

import com.documentrag.model.DocumentUploadResponse;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class DocumentProcessingService {
    
    @Autowired
    private EmbeddingModel embeddingModel;
    
    @Autowired
    private DocumentChatService documentChatService;
    
    // In-memory document store
    private final ConcurrentHashMap<String, DocumentInfo> documentStore = new ConcurrentHashMap<>();
    private final DocumentSplitter documentSplitter = DocumentSplitters.recursive(100, 20);
    
    public DocumentUploadResponse processPdfDocument(MultipartFile file, String documentType, String description) {
        DocumentUploadResponse response = new DocumentUploadResponse();
        
        try {
            // Validate file
            if (!isValidPdfFile(file)) {
                response.setSuccess(false);
                response.setMessage("Invalid PDF file. Please upload a valid PDF document.");
                return response;
            }
            
            // Extract text from PDF
            String pdfText = extractTextFromPdf(file.getInputStream());
            
            if (pdfText == null || pdfText.trim().isEmpty()) {
                response.setSuccess(false);
                response.setMessage("Could not extract text from PDF. The file might be corrupted or password protected.");
                return response;
            }
            
            // Generate document ID
            String documentId = UUID.randomUUID().toString();
            
            // Create document with metadata
            Document document = Document.from(pdfText, dev.langchain4j.data.document.Metadata.from("type", documentType));
            
            // Split document into segments
            List<TextSegment> segments = documentSplitter.split(document);
            
            // Add document to vector store for chat functionality
            documentChatService.addDocumentToVectorStore(documentId, pdfText, documentType);
            
            // Store document info
            DocumentInfo docInfo = new DocumentInfo(
                documentId,
                file.getOriginalFilename(),
                documentType,
                description,
                file.getSize(),
                segments.size(),
                segments.size(), // All segments processed
                pdfText
            );
            documentStore.put(documentId, docInfo);
            
            // Build response
            response.setSuccess(true);
            response.setMessage("Document uploaded and processed successfully. You can now ask questions about this document!");
            response.setDocumentId(documentId);
            response.setFileName(file.getOriginalFilename());
            response.setDocumentType(documentType);
            response.setFileSize(file.getSize());
            response.setSegmentsProcessed(segments.size());
            
            // Add metadata
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("totalSegments", segments.size());
            metadata.put("processedSegments", segments.size());
            metadata.put("documentType", documentType);
            metadata.put("description", description);
            metadata.put("vectorStore", "Chroma");
            response.setMetadata(metadata);
            
            // Log successful document processing
            // System.out.println("Document processed: " + documentId + " (" + file.getOriginalFilename() + 
            //                  ", segments: " + segments.size() + ", added to vector store)");
            
        } catch (Exception e) {
            response.setSuccess(false);
            response.setMessage("Error processing document: " + e.getMessage());
            // Log error processing PDF
            // System.err.println("Error processing PDF: " + e.getMessage());
        }
        
        return response;
    }
    
    private boolean isValidPdfFile(MultipartFile file) {
        return file != null && 
               !file.isEmpty() && 
               file.getContentType() != null && 
               file.getContentType().equals("application/pdf") &&
               file.getSize() > 0 && 
               file.getSize() <= 10 * 1024 * 1024; // 10MB limit
    }
    
    private String extractTextFromPdf(InputStream inputStream) throws IOException {
        try (PDDocument document = PDDocument.load(inputStream)) {
            if (document.isEncrypted()) {
                throw new IOException("PDF is encrypted and cannot be processed");
            }
            
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }
    
    public DocumentInfo getDocumentInfo(String documentId) {
        return documentStore.get(documentId);
    }
    
    public List<DocumentInfo> getAllDocuments() {
        return documentStore.values().stream().toList();
    }
    
    public void deleteDocument(String documentId) {
        documentStore.remove(documentId);
    }
    
    public void clearAllDocuments() {
        documentStore.clear();
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