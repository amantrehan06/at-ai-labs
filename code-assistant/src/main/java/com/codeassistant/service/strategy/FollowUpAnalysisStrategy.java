package com.codeassistant.service.strategy;

import com.codeassistant.model.AnalysisRequest;
import com.codeassistant.model.AnalysisType;
import com.codeassistant.model.MessagePair;
import com.codeassistant.model.SystemMessage;
import com.codeassistant.model.UserMessage;
import org.springframework.stereotype.Component;

/**
 * Strategy for handling follow-up questions in an ongoing conversation.
 * This strategy provides a conversational approach for follow-up questions.
 */
@Component
public class FollowUpAnalysisStrategy implements AnalysisStrategy {
    
    @Override
    public AnalysisType getAnalysisType() {
        return AnalysisType.FOLLOWUP;
    }
    
    @Override
    public MessagePair buildMessages(AnalysisRequest request) {
        // Create system message for follow-up context
        SystemMessage systemMessage = SystemMessage.createAnalysisMessage(
            request.getAnalysisType(), 
            request.getLanguage()
        );
        
        // Create user message with the follow-up question
        UserMessage userMessage = UserMessage.createAnalysisMessage(
            request.getCode(),
            request.getAnalysisType(),
            request.getLanguage()
        );
        
        return new MessagePair(systemMessage, userMessage);
    }
    
    @Override
    public String getDescription() {
        return "Handles follow-up questions in an ongoing conversation with a conversational approach";
    }
} 