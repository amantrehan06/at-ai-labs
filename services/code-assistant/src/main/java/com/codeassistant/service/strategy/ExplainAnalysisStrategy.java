package com.codeassistant.service.strategy;

import com.codeassistant.model.AnalysisRequest;
import com.codeassistant.model.AnalysisType;
import org.springframework.stereotype.Component;

/**
 * Strategy for explaining code - describes what the code does and how it works.
 */
@Component
public class ExplainAnalysisStrategy implements AnalysisStrategy {
    
    @Override
    public AnalysisType getAnalysisType() {
        return AnalysisType.EXPLAIN;
    }
    
    @Override
    public String buildPrompt(AnalysisRequest request) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("You are an expert software developer and code analyst specializing in code explanation.\n");
        prompt.append("Your role is to provide clear, detailed explanations of code functionality and logic.\n");
        prompt.append("Focus on making complex concepts understandable for developers of all levels.\n\n");
        
        // Language context
        if (request.getLanguage() != null && !request.getLanguage().trim().isEmpty()) {
            prompt.append("Language: ").append(request.getLanguage()).append("\n\n");
        }
        
        prompt.append("Please analyze the following code:\n\n");
        prompt.append("```").append(request.getLanguage()).append("\n");
        prompt.append(request.getCode());
        prompt.append("\n```\n\n");
        
        prompt.append("Please provide a clear and detailed explanation of what this code does, ");
        prompt.append("including its purpose, how it works, and any important concepts or patterns used. ");
        prompt.append("Focus on making it understandable for developers of all levels.\n\n");
        
        prompt.append("Structure your response with:\n");
        prompt.append("- Overview: Brief summary of what the code does\n");
        prompt.append("- Functionality: Detailed explanation of how it works\n");
        prompt.append("- Key Concepts: Important programming concepts used\n");
        prompt.append("- Code Flow: Step-by-step breakdown of the logic\n");
        
        return prompt.toString();
    }
    
    @Override
    public String getDescription() {
        return "Explains what the code does, how it works, and the key concepts involved";
    }
} 