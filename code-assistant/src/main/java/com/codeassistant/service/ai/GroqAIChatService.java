package com.codeassistant.service.ai;

import com.codeassistant.model.AnalysisRequest;
import com.codeassistant.model.AnalysisResponse;
import com.codeassistant.model.MessagePair;
import com.codeassistant.service.strategy.AnalysisStrategy;
import com.common.AIServiceManager;
import com.common.AIServiceConstants;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Groq AI implementation of the AIChatService using Strategy pattern.
 * This class is responsible for communicating with Groq's API.
 */
@Service
public class GroqAIChatService implements AIChatService {
    
    private static final Logger logger = LoggerFactory.getLogger(GroqAIChatService.class);
    
    private final Map<com.codeassistant.model.AnalysisType, AnalysisStrategy> analysisStrategies;
    private final AIServiceManager aiServiceManager;
    
    public GroqAIChatService(@Autowired AIServiceManager aiServiceManager,
                             @Autowired List<AnalysisStrategy> strategies) {
        
        this.aiServiceManager = aiServiceManager;
        
        // Initialize analysis strategies
        this.analysisStrategies = strategies.stream()
            .collect(Collectors.toMap(AnalysisStrategy::getAnalysisType, strategy -> strategy));
        
        logger.info("Groq AI chat service initialized with {} analysis strategies", strategies.size());
    }
    
    @Override
    public AnalysisResponse analyzeCode(AnalysisRequest request) throws AIServiceException {
        try {
            // Get the appropriate strategy for the analysis type
            AnalysisStrategy strategy = analysisStrategies.get(request.getAnalysisType());
            if (strategy == null) {
                throw new AIServiceException("No analysis strategy found for type: " + request.getAnalysisType());
            }
            
            // Get Groq model from centralized manager using generic method
            ChatLanguageModel chatModel = aiServiceManager.getModel(AIServiceConstants.GROQ_SERVICE, request.getApiKey());
            
            // Use the strategy to build messages (maintains Strategy pattern)
            MessagePair messages = strategy.buildMessages(request);
            
            // Convert to LangChain4j message types
            List<ChatMessage> chatMessages = List.of(
                new dev.langchain4j.data.message.SystemMessage(messages.getSystemContent()),
                new dev.langchain4j.data.message.UserMessage(messages.getUserContent())
            );
            
            Response<AiMessage> response = chatModel.generate(chatMessages);
            
            String analysis = response.content().text();
            
            return AnalysisResponse.builder()
                .analysis(analysis)
                .analysisType(request.getAnalysisType())
                .language(request.getLanguage())
                .success(true)
                .build();
                
        } catch (Exception e) {
            logger.error("Error analyzing code with Groq AI", e);
            throw new AIServiceException("Failed to analyze code: " + e.getMessage(), e);
        }
    }
    
    @Override
    public boolean isAvailable() {
        return true; // Service is available, user can provide API key
    }
} 