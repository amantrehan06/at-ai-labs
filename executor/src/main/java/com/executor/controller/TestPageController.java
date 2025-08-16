package com.executor.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class TestPageController {

    @GetMapping("/repo-pdf-test")
    public String repoPdfTestPage() {
        return "repo-pdf-test";
    }
    
    @GetMapping("/document-rag")
    public String documentRagPage() {
        return "document-rag/index";
    }
} 