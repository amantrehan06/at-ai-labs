package com.codeassistant.service.strategy;

import com.codeassistant.model.AnalysisRequest;
import com.codeassistant.model.AnalysisType;
import org.springframework.stereotype.Component;

/**
 * Strategy for refactoring code - suggests improvements for code quality and maintainability.
 */
@Component
public class RefactorAnalysisStrategy implements AnalysisStrategy {
    
    @Override
    public AnalysisType getAnalysisType() {
        return AnalysisType.REFACTOR;
    }
    
    @Override
    public String buildPrompt(AnalysisRequest request) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("You are an expert software developer and code reviewer specializing in code refactoring.\n");
        prompt.append("Your role is to provide actionable refactoring suggestions to improve code quality, ");
        prompt.append("readability, performance, and maintainability.\n");
        prompt.append("Always provide specific recommendations with explanations of why they would be beneficial.\n\n");
        
        // Language context
        if (request.getLanguage() != null && !request.getLanguage().trim().isEmpty()) {
            prompt.append("Language: ").append(request.getLanguage()).append("\n\n");
        }
        
        prompt.append("Please analyze the following code for refactoring opportunities:\n\n");
        prompt.append("```").append(request.getLanguage()).append("\n");
        prompt.append(request.getCode());
        prompt.append("\n```\n\n");
        
        prompt.append("Please provide refactoring suggestions to improve this code. ");
        prompt.append("Consider code quality, readability, performance, and best practices. ");
        prompt.append("Provide specific recommendations with explanations of why they would be beneficial. ");
        prompt.append("If possible, include refactored code examples.\n\n");
        
        prompt.append("Structure your response with:\n");
        prompt.append("- Code Quality Issues: Identify problems with current code\n");
        prompt.append("- Refactoring Suggestions: Specific improvements to make\n");
        prompt.append("- Performance Optimizations: Ways to improve efficiency\n");
        prompt.append("- Best Practices: Recommendations following language conventions\n");
        prompt.append("- Refactored Examples: Show improved code versions\n");
        
        return prompt.toString();
    }
    
    @Override
    public String getDescription() {
        return "Suggests improvements for code quality, readability, performance, and maintainability";
    }
} 