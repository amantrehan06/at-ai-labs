package com.codeassistant.service.strategy;

import com.codeassistant.model.AnalysisRequest;
import com.codeassistant.model.AnalysisType;
import org.springframework.stereotype.Component;

/**
 * Strategy for debugging code - identifies potential issues and suggests fixes.
 */
@Component
public class DebugAnalysisStrategy implements AnalysisStrategy {
    
    @Override
    public AnalysisType getAnalysisType() {
        return AnalysisType.DEBUG;
    }
    
    @Override
    public String buildPrompt(AnalysisRequest request) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("You are an expert software developer and code reviewer specializing in debugging and code analysis.\n");
        prompt.append("Your role is to identify potential bugs, issues, and areas of concern in code.\n");
        prompt.append("Focus on logical errors, edge cases, performance issues, and security vulnerabilities.\n");
        prompt.append("Provide specific debugging tips and suggestions for improvement.\n\n");
        
        // Language context
        if (request.getLanguage() != null && !request.getLanguage().trim().isEmpty()) {
            prompt.append("Language: ").append(request.getLanguage()).append("\n\n");
        }
        
        prompt.append("Please analyze the following code for potential issues:\n\n");
        prompt.append("```").append(request.getLanguage()).append("\n");
        prompt.append(request.getCode());
        prompt.append("\n```\n\n");
        
        prompt.append("Please analyze this code for potential bugs, issues, or areas of concern. ");
        prompt.append("Identify any logical errors, edge cases, performance issues, or security vulnerabilities. ");
        prompt.append("Provide debugging tips and suggestions for improvement.\n\n");
        
        prompt.append("Structure your response with:\n");
        prompt.append("- Potential Bugs: Identify possible runtime errors or logic issues\n");
        prompt.append("- Edge Cases: Situations where the code might fail\n");
        prompt.append("- Performance Issues: Bottlenecks or inefficient operations\n");
        prompt.append("- Security Concerns: Vulnerabilities or unsafe practices\n");
        prompt.append("- Debugging Tips: How to troubleshoot and fix issues\n");
        prompt.append("- Suggested Fixes: Specific solutions for identified problems\n");
        
        return prompt.toString();
    }
    
    @Override
    public String getDescription() {
        return "Identifies potential bugs, issues, and provides debugging suggestions";
    }
} 