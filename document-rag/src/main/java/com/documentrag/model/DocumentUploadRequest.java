package com.documentrag.model;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocumentUploadRequest {
    
    @NotNull(message = "Document file is required")
    private MultipartFile file;
    
    private String documentType = "general";
    private String description;
    
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