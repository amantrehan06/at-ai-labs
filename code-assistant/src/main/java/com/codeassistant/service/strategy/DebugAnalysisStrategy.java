package com.codeassistant.service.strategy;

import com.codeassistant.model.AnalysisRequest;
import com.codeassistant.model.AnalysisType;
import com.codeassistant.model.MessagePair;
import com.codeassistant.model.SystemMessage;
import com.codeassistant.model.UserMessage;
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
        return "Identifies potential bugs, issues, and provides debugging suggestions";
    }
} 