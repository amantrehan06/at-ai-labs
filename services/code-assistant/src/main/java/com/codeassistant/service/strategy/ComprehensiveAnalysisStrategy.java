package com.codeassistant.service.strategy;

import com.codeassistant.model.AnalysisRequest;
import com.codeassistant.model.AnalysisType;
import org.springframework.stereotype.Component;

/**
 * Strategy for comprehensive code analysis - combines explanation, refactoring, and debugging.
 */
@Component
public class ComprehensiveAnalysisStrategy implements AnalysisStrategy {
    
    @Override
    public AnalysisType getAnalysisType() {
        return AnalysisType.ANALYZE;
    }
    
    @Override
    public String buildPrompt(AnalysisRequest request) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("You are an expert software developer and code analyst specializing in comprehensive code review.\n");
        prompt.append("Your role is to provide a complete analysis including explanation, refactoring suggestions, and debugging insights.\n");
        prompt.append("Provide a thorough review that covers all aspects of the code.\n\n");
        
        // Language context
        if (request.getLanguage() != null && !request.getLanguage().trim().isEmpty()) {
            prompt.append("Language: ").append(request.getLanguage()).append("\n\n");
        }
        
        prompt.append("Please provide a comprehensive analysis of the following code:\n\n");
        prompt.append("```").append(request.getLanguage()).append("\n");
        prompt.append(request.getCode());
        prompt.append("\n```\n\n");
        
        prompt.append("Please provide a comprehensive analysis including explanation, potential improvements, and any issues.\n\n");
        
        prompt.append("Structure your response with:\n");
        prompt.append("## Code Explanation\n");
        prompt.append("- Overview: What the code does and its purpose\n");
        prompt.append("- Functionality: How the code works step by step\n");
        prompt.append("- Key Concepts: Important programming patterns and concepts used\n\n");
        
        prompt.append("## Code Quality Assessment\n");
        prompt.append("- Strengths: What the code does well\n");
        prompt.append("- Areas for Improvement: Specific refactoring opportunities\n");
        prompt.append("- Best Practices: Recommendations for better code structure\n\n");
        
        prompt.append("## Potential Issues\n");
        prompt.append("- Bugs: Possible runtime errors or logic issues\n");
        prompt.append("- Edge Cases: Situations where the code might fail\n");
        prompt.append("- Performance: Bottlenecks or inefficient operations\n");
        prompt.append("- Security: Potential vulnerabilities\n\n");
        
        prompt.append("## Recommendations\n");
        prompt.append("- Refactoring Suggestions: Specific improvements to make\n");
        prompt.append("- Alternative Approaches: Different ways to solve the problem\n");
        prompt.append("- Testing Strategy: How to verify the code works correctly\n");
        
        return prompt.toString();
    }
    
    @Override
    public String getDescription() {
        return "Provides comprehensive analysis including explanation, refactoring suggestions, and debugging insights";
    }
} 