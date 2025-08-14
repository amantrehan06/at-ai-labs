package com.documentrag.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.List;

@Slf4j
@Service
public class RepoPdfGenerator {

    public static final String[] IGNORE_DIRS = {
        "target", "build", "node_modules", ".git", ".idea", "out", "bin", "obj"
    };

    public byte[] generateRepoPdf(String repoPath) throws IOException {
        StringBuilder repoContent = new StringBuilder();
        
        // Add header
        repoContent.append("Java Repository Code Analysis\n");
        repoContent.append("Generated on: ").append(java.time.LocalDateTime.now()).append("\n");
        repoContent.append("Repository: ").append(new File(repoPath).getName()).append("\n");
        repoContent.append("=".repeat(80)).append("\n\n");

        // Step 1: Recursively scan Java files only
        Files.walk(Paths.get(repoPath))
                .filter(p -> !p.toFile().isDirectory())
                .filter(p -> p.toString().toLowerCase().endsWith(".java"))
                .filter(p -> !isInIgnoredDirectory(p.toString()))
                .sorted()
                .forEach(file -> {
                    try {
                        List<String> lines = Files.readAllLines(file);
                        String relativePath = file.toString().replace(repoPath, "").replace("\\", "/");
                        
                        repoContent.append("File: ").append(relativePath).append("\n");
                        repoContent.append("Size: ").append(lines.size()).append(" lines\n");
                        
                        // Extract class information
                        String className = extractClassName(lines);
                        String packageName = extractPackageName(lines);
                        
                        if (packageName != null) {
                            repoContent.append("Package: ").append(packageName).append("\n");
                        }
                        repoContent.append("Class: ").append(className).append("\n");
                        repoContent.append("-".repeat(60)).append("\n");

                        // Add file content (limit to first 100 lines to keep PDF manageable)
                        int maxLines = Math.min(lines.size(), 100);
                        for (int i = 0; i < maxLines; i++) {
                            repoContent.append(String.format("%4d: %s\n", i + 1, lines.get(i)));
                        }
                        
                        if (lines.size() > 100) {
                            repoContent.append("... (truncated, showing first 100 lines)\n");
                        }
                        
                        repoContent.append("\n").append("=".repeat(80)).append("\n\n");
                        
                    } catch (IOException e) {
                        log.error("Error reading file: {}", file, e);
                        repoContent.append("Error reading file: ").append(file).append("\n");
                        repoContent.append("Error: ").append(e.getMessage()).append("\n\n");
                    }
                });

        // Step 2: Generate PDF
        return generatePdfFromContent(repoContent.toString());
    }

    private boolean isInIgnoredDirectory(String filePath) {
        String lowerPath = filePath.toLowerCase();
        for (String dir : IGNORE_DIRS) {
            if (lowerPath.contains("/" + dir + "/") || lowerPath.contains("\\" + dir + "\\")) {
                return true;
            }
        }
        return false;
    }

    private String extractClassName(List<String> lines) {
        for (String line : lines) {
            line = line.trim();
            if (line.startsWith("public class") || line.startsWith("class") ||
                line.startsWith("public interface") || line.startsWith("interface") ||
                line.startsWith("enum") || line.startsWith("public enum")) {
                String[] parts = line.split("\\s+");
                for (int i = 0; i < parts.length; i++) {
                    if (parts[i].equals("class") || parts[i].equals("interface") || parts[i].equals("enum")) {
                        if (i + 1 < parts.length) {
                            // Handle generic types like "class MyClass<T>"
                            String className = parts[i + 1];
                            int genericIndex = className.indexOf('<');
                            if (genericIndex > 0) {
                                className = className.substring(0, genericIndex);
                            }
                            return className;
                        }
                    }
                }
            }
        }
        return "UnknownClass";
    }

    private String extractPackageName(List<String> lines) {
        for (String line : lines) {
            line = line.trim();
            if (line.startsWith("package ")) {
                return line.substring(8, line.length() - 1); // Remove "package " and ";"
            }
        }
        return null;
    }

    private byte[] generatePdfFromContent(String content) throws IOException {
        PDDocument doc = new PDDocument();
        
        // Split content into pages
        String[] lines = content.split("\n");
        int linesPerPage = 50;
        int currentLine = 0;
        
        while (currentLine < lines.length) {
            PDPage page = new PDPage();
            doc.addPage(page);
            
            PDPageContentStream contentStream = new PDPageContentStream(doc, page);
            contentStream.beginText();
            contentStream.setFont(PDType1Font.COURIER, 8);
            contentStream.setLeading(10f);
            contentStream.newLineAtOffset(25, 750);
            
            // Add page content
            int pageLines = 0;
            while (currentLine < lines.length && pageLines < linesPerPage) {
                String line = lines[currentLine];
                
                // Handle long lines by wrapping
                if (line.length() > 120) {
                    String[] wrappedLines = wrapText(line, 120);
                    for (String wrappedLine : wrappedLines) {
                        if (pageLines >= linesPerPage) break;
                        contentStream.showText(wrappedLine);
                        contentStream.newLine();
                        pageLines++;
                    }
                } else {
                    contentStream.showText(line);
                    contentStream.newLine();
                    pageLines++;
                }
                
                currentLine++;
            }
            
            contentStream.endText();
            contentStream.close();
        }
        
        // Convert to byte array
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        doc.save(baos);
        doc.close();
        
        return baos.toByteArray();
    }

    private String[] wrapText(String text, int maxLength) {
        if (text.length() <= maxLength) {
            return new String[]{text};
        }
        
        java.util.List<String> lines = new java.util.ArrayList<>();
        int start = 0;
        
        while (start < text.length()) {
            int end = Math.min(start + maxLength, text.length());
            
            // Try to break at a word boundary
            if (end < text.length()) {
                int lastSpace = text.lastIndexOf(' ', end);
                if (lastSpace > start) {
                    end = lastSpace;
                }
            }
            
            lines.add(text.substring(start, end));
            start = end;
            
            // Skip leading spaces
            while (start < text.length() && text.charAt(start) == ' ') {
                start++;
            }
        }
        
        return lines.toArray(new String[0]);
    }
} 