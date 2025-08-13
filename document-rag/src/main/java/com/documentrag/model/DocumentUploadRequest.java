package com.documentrag.model;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

@Data
@NoArgsConstructor
public class DocumentUploadRequest {
    
    @NotNull(message = "Document file is required")
    private MultipartFile file;
    
    private String documentType = "general";
    private String description;
    
    public DocumentUploadRequest(MultipartFile file) {
        this.file = file;
    }
    
    public DocumentUploadRequest(MultipartFile file, String documentType, String description) {
        this.file = file;
        this.documentType = documentType;
        this.description = description;
    }
    
    // Validation methods
    public boolean isValidPdf() {
        return file != null && 
               file.getContentType() != null && 
               file.getContentType().equals("application/pdf");
    }
    
    public boolean isValidFile() {
        return file != null && 
               !file.isEmpty() && 
               file.getSize() > 0 && 
               file.getSize() <= 10 * 1024 * 1024; // 10MB limit
    }
} 