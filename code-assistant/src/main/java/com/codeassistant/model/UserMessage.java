package com.codeassistant.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * User Message model for containing user requests and code to be analyzed.
 * This class represents the user's input that will be processed by the AI
 * based on the system instructions provided in SystemMessage.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserMessage {
    
    @JsonProperty("role")
    private String role = "user";
    
    @JsonProperty("content")
    private String content;
    
    @JsonProperty("code")
    private String code;
    
    @JsonProperty("language")
    private String language;
    
    @JsonProperty("analysisType")
    private AnalysisType analysisType;
    
    /**
     * Create a user message for code analysis
     */
    public static UserMessage createAnalysisMessage(String code, AnalysisType analysisType, String language) {
        StringBuilder content = new StringBuilder();
        
        content.append("Please analyze the following code");
        
        switch (analysisType) {
            case EXPLAIN:
                content.append(" and provide a clear explanation of what it does and how it works");
                break;
            case DEBUG:
                content.append(" for potential bugs, issues, or areas of concern");
                break;
            case REFACTOR:
                content.append(" for refactoring opportunities to improve code quality and maintainability");
                break;
            case ANALYZE:
                content.append(" comprehensively including functionality, performance, security, and maintainability");
                break;
            default:
                content.append(" professionally");
        }
        
        content.append(":\n\n");
        content.append("```").append(language).append("\n");
        content.append(code);
        content.append("\n```");
        
        return UserMessage.builder()
            .content(content.toString())
            .code(code)
            .language(language)
            .analysisType(analysisType)
            .build();
    }
} 