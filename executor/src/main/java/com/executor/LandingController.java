package com.executor;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Landing Controller
 * 
 * Handles the main landing page and navigation routes.
 */
@Controller
public class LandingController {

    /**
     * Landing page - shows all available services
     */
    @GetMapping("/")
    public String landing() {
        return "landing.html";
    }

    /**
     * Code Assistant service page
     */
    @GetMapping("/code-assistant")
    public String codeAssistant() {
        return "index.html";
    }

    /**
     * AI Chat service page
     */
    @GetMapping("/ai-chat")
    public String aiChat() {
        return "ai-chat/index.html";
    }

    /**
     * Code Generator service page
     */
    @GetMapping("/code-generator")
    public String codeGenerator() {
        return "code-generator/index.html";
    }
} 