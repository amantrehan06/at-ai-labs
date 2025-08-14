package com.documentrag.controller;

import com.documentrag.service.RepoPdfGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/repo-pdf")
public class RepoPdfTestController {

    @Autowired
    private RepoPdfGenerator repoPdfGenerator;

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "Repository PDF Generator");
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("endpoints", new String[]{
            "GET /health - Service health check",
            "GET /generate - Generate PDF from current repository",
            "GET /generate/{repoName} - Generate PDF from specific repository path"
        });
        return ResponseEntity.ok(response);
    }

    @GetMapping("/generate")
    public ResponseEntity<ByteArrayResource> generateCurrentRepoPdf() {
        try {
            // Get the current working directory (should be the project root)
            String currentRepoPath = System.getProperty("user.dir");
            log.info("Generating PDF from current repository: {}", currentRepoPath);
            
            byte[] pdfContent = repoPdfGenerator.generateRepoPdf(currentRepoPath);
            
            // Create filename with timestamp
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String fileName = "at-ai-labs-repo_" + timestamp + ".pdf";
            
            ByteArrayResource resource = new ByteArrayResource(pdfContent);
            
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .contentLength(pdfContent.length)
                    .body(resource);
                    
        } catch (IOException e) {
            log.error("Error generating PDF from current repository: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/generate/{repoName}")
    public ResponseEntity<ByteArrayResource> generateSpecificRepoPdf(@PathVariable String repoName) {
        try {
            // Try to find the repository in common locations
            String[] possiblePaths = {
                System.getProperty("user.dir"), // Current directory
                System.getProperty("user.home") + "/Documents", // Documents folder
                System.getProperty("user.home") + "/Desktop", // Desktop
                System.getProperty("user.home") + "/Projects", // Projects folder
                System.getProperty("user.home") + "/git", // Git folder
                System.getProperty("user.home") + "/repos" // Repos folder
            };
            
            String repoPath = null;
            for (String path : possiblePaths) {
                Path fullPath = Paths.get(path, repoName);
                if (fullPath.toFile().exists() && fullPath.toFile().isDirectory()) {
                    repoPath = fullPath.toString();
                    break;
                }
            }
            
            if (repoPath == null) {
                log.error("Repository '{}' not found in common locations", repoName);
                return ResponseEntity.notFound().build();
            }
            
            log.info("Generating PDF from repository: {}", repoPath);
            
            byte[] pdfContent = repoPdfGenerator.generateRepoPdf(repoPath);
            
            // Create filename with timestamp
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String fileName = repoName + "_" + timestamp + ".pdf";
            
            ByteArrayResource resource = new ByteArrayResource(pdfContent);
            
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .contentLength(pdfContent.length)
                    .body(resource);
                    
        } catch (IOException e) {
            log.error("Error generating PDF from repository '{}': {}", repoName, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/generate-custom")
    public ResponseEntity<ByteArrayResource> generateCustomRepoPdf(@RequestBody Map<String, String> request) {
        try {
            String repoPath = request.get("repoPath");
            if (repoPath == null || repoPath.trim().isEmpty()) {
                return ResponseEntity.badRequest().build();
            }
            
            log.info("Generating PDF from custom repository path: {}", repoPath);
            
            byte[] pdfContent = repoPdfGenerator.generateRepoPdf(repoPath);
            
            // Create filename with timestamp
            String repoName = Paths.get(repoPath).getFileName().toString();
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String fileName = repoName + "_" + timestamp + ".pdf";
            
            ByteArrayResource resource = new ByteArrayResource(pdfContent);
            
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .contentLength(pdfContent.length)
                    .body(resource);
                    
        } catch (IOException e) {
            log.error("Error generating PDF from custom repository path: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> getRepositoryInfo() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String currentRepoPath = System.getProperty("user.dir");
            Path repoPath = Paths.get(currentRepoPath);
            
            response.put("currentRepository", repoPath.getFileName().toString());
            response.put("fullPath", currentRepoPath);
            response.put("exists", repoPath.toFile().exists());
            response.put("isDirectory", repoPath.toFile().isDirectory());
            response.put("canRead", repoPath.toFile().canRead());
            response.put("timestamp", LocalDateTime.now().toString());
            
            // Count Java files only
            long javaFileCount = java.nio.file.Files.walk(repoPath)
                    .filter(p -> !p.toFile().isDirectory())
                    .filter(p -> p.toString().toLowerCase().endsWith(".java"))
                    .count();
            
            response.put("javaFileCount", javaFileCount);
            
        } catch (Exception e) {
            log.error("Error getting repository info: {}", e.getMessage(), e);
            response.put("error", e.getMessage());
        }
        
        return ResponseEntity.ok(response);
    }
} 