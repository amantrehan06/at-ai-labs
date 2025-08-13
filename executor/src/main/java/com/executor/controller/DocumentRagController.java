package com.executor.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DocumentRagController {
    
    @GetMapping("/document-rag")
    public String documentRag() {
        return "document-rag/index";
    }
} 