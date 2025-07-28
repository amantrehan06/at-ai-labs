package com.codeassistant.service.strategy;

import com.codeassistant.model.AnalysisRequest;
import com.codeassistant.model.AnalysisType;
import com.codeassistant.model.MessagePair;
import com.codeassistant.model.SystemMessage;
import com.codeassistant.model.UserMessage;
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
    public MessagePair buildMessages(AnalysisRequest request) {
        // Create system message directly
        SystemMessage systemMessage = SystemMessage.createAnalysisMessage(
            request.getAnalysisType(), 
            request.getLanguage()
        );
        
        // Create user message directly
        UserMessage userMessage = UserMessage.createAnalysisMessage(
            request.getCode(),
            request.getAnalysisType(),
            request.getLanguage()
        );
        
        return new MessagePair(systemMessage, userMessage);
    }
    
    @Override
    public String getDescription() {
        return "Suggests improvements for code quality, readability, performance, and maintainability";
    }
} 