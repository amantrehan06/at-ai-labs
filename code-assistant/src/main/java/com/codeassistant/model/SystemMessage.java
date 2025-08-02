package com.codeassistant.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * System Message model for defining AI behavior and instructions.
 * This class represents the system-level instructions that define how the AI should behave
 * across all interactions, providing consistent behavior and professional responses.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemMessage {
    
    @JsonProperty("role")
    @Builder.Default
    private String role = "system";
    
    @JsonProperty("content")
    private String content;
    
    @JsonProperty("analysisType")
    private AnalysisType analysisType;
    
    @JsonProperty("language")
    private String language;
    
    @JsonProperty("context")
    private String context;
    
    /**
     * Create a system message for specific analysis type
     */
    public static SystemMessage createAnalysisMessage(AnalysisType analysisType, String language) {
        String roleSpecificContent = getRoleSpecificContent(analysisType);
        String languageContext = getLanguageContext(language);
        String responseStructure = getResponseStructure(analysisType);
        
        StringBuilder content = new StringBuilder();
        content.append("You are an expert software developer and code analyst with deep knowledge of multiple programming languages. ");
        content.append("Your role is to provide professional, accurate, and educational code analysis. ");
        content.append("Always be helpful, clear, and thorough in your explanations. ");
        content.append("Use proper technical terminology while remaining accessible to developers of all levels.\n\n");
        
        content.append(roleSpecificContent).append("\n\n");
        
        if (languageContext != null) {
            content.append(languageContext).append("\n\n");
        }
        
        content.append(responseStructure);
        
        return SystemMessage.builder()
            .content(content.toString())
            .analysisType(analysisType)
            .language(language)
            .context(languageContext)
            .build();
    }
    
    private static String getRoleSpecificContent(AnalysisType analysisType) {
        switch (analysisType) {
            case WRITE_CODE:
                return "You are an expert code generator specializing in creating high-quality, functional code based on user requirements. " +
                       "Your goal is to write clean, efficient, and well-documented code that meets the specified requirements. " +
                       "Focus on best practices, proper error handling, and maintainable code structure.";
            case DEBUG:
                return "You are an expert debugging specialist with deep knowledge of common programming errors and issues. " +
                       "Your role is to identify potential problems, suggest fixes, and explain why issues occur. " +
                       "Be thorough in your analysis and provide actionable solutions.";
            case REFACTOR:
                return "You are an expert code refactoring specialist focused on improving code quality, readability, and maintainability. " +
                       "Your goal is to suggest improvements while preserving functionality. " +
                       "Focus on clean code principles, performance, and best practices.";
            case ANALYZE:
                return "You are an expert code analyst providing comprehensive code reviews. " +
                       "Your role is to analyze code from multiple perspectives including functionality, performance, security, " +
                       "and maintainability. Provide detailed insights and actionable recommendations.";
            case FOLLOWUP:
                return "You are a helpful AI programming assistant in an ongoing conversation. " +
                       "This is a follow-up question that builds on our previous discussion. " +
                       "Please provide a conversational and helpful response that continues the conversation naturally. " +
                       "If the question is about code, provide code examples. If it's about explaining concepts, provide clear explanations. " +
                       "If it's about improving or modifying code, provide the improved code with explanations.";
            default:
                return "You are an expert code analyst providing professional code analysis.";
        }
    }
    
    private static String getLanguageContext(String language) {
        if (language == null) return null;
        
        switch (language.toLowerCase()) {
            case "java":
                return "This is Java code. Consider Java-specific patterns, conventions, and best practices.";
            case "python":
                return "This is Python code. Consider Python-specific patterns, PEP guidelines, and best practices.";
            case "javascript":
            case "js":
                return "This is JavaScript code. Consider JavaScript-specific patterns, ES6+ features, and best practices.";
            case "cpp":
            case "c++":
                return "This is C++ code. Consider C++-specific patterns, memory management, and best practices.";
            default:
                return null;
        }
    }
    
    private static String getResponseStructure(AnalysisType analysisType) {
        switch (analysisType) {
            case WRITE_CODE:
                return "Structure your response with:\n" +
                       "- Requirements Analysis: Summary of what the user requested\n" +
                       "- Generated Code: Complete, functional code that meets the requirements\n" +
                       "- Code Explanation: Brief explanation of how the code works\n" +
                       "- Usage Instructions: How to use or implement the generated code\n" +
                       "- Additional Notes: Any important considerations or alternatives";
            case DEBUG:
                return "Structure your response with:\n" +
                       "- Issues Found: List of potential problems identified\n" +
                       "- Root Causes: Explanation of why these issues occur\n" +
                       "- Solutions: Specific fixes and improvements\n" +
                       "- Prevention: How to avoid similar issues in the future";
            case REFACTOR:
                return "Structure your response with:\n" +
                       "- Current Issues: Problems with the current code\n" +
                       "- Suggested Improvements: Specific refactoring recommendations\n" +
                       "- Benefits: Why these changes improve the code\n" +
                       "- Implementation: How to apply the suggested changes";
            case ANALYZE:
                return "Structure your response with:\n" +
                       "- Code Overview: Summary of functionality and purpose\n" +
                       "- Strengths: What the code does well\n" +
                       "- Areas for Improvement: Specific suggestions for enhancement\n" +
                       "- Security Considerations: Potential security implications\n" +
                       "- Performance Analysis: Efficiency considerations\n" +
                       "- Best Practices: Recommendations for better code quality";
            default:
                return "Provide a comprehensive analysis of the code.";
        }
    }
} 