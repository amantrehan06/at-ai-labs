package com.codeassistant.service.ai;

import com.codeassistant.model.AnalysisRequest;
import com.codeassistant.model.AnalysisResponse;
import com.codeassistant.service.strategy.AnalysisStrategy;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.output.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * OpenAI implementation of the AIChatService using Strategy pattern.
 * This class is responsible for communicating with OpenAI's API.
 */
@Service
public class OpenAIChatService implements AIChatService {
    
    private static final Logger logger = LoggerFactory.getLogger(OpenAIChatService.class);
    
    private final Map<com.codeassistant.model.AnalysisType, AnalysisStrategy> analysisStrategies;
    private final String defaultModel;
    
    public OpenAIChatService(@Value("${openai.model:gpt-3.5-turbo}") String defaultModel,
                            @Autowired List<AnalysisStrategy> strategies) {
        
        // Initialize analysis strategies
        this.analysisStrategies = strategies.stream()
            .collect(Collectors.toMap(AnalysisStrategy::getAnalysisType, strategy -> strategy));
        
        this.defaultModel = defaultModel;
        logger.info("OpenAI chat service initialized with {} analysis strategies", strategies.size());
    }
    
    @Override
    public AnalysisResponse analyzeCode(AnalysisRequest request) throws AIServiceException {
        try {
            // Get API key from request or use default
            String apiKey = request.getApiKey();
            if (apiKey == null || apiKey.trim().isEmpty()) {
                throw new AIServiceException("OpenAI API key is required for this service.");
            }
            
            // Get the appropriate strategy for the analysis type
            AnalysisStrategy strategy = analysisStrategies.get(request.getAnalysisType());
            if (strategy == null) {
                throw new AIServiceException("No analysis strategy found for type: " + request.getAnalysisType());
            }
            
            // Create OpenAI client with API key from request or default
            ChatLanguageModel chatModel = OpenAiChatModel.builder()
                .apiKey(apiKey)
                .modelName(defaultModel)
                .timeout(Duration.ofSeconds(60))
                .build();
            
            String prompt = strategy.buildPrompt(request);
            Response<AiMessage> response = chatModel.generate(List.of(new UserMessage(prompt)));
            
            String analysis = response.content().text();
            
            return AnalysisResponse.builder()
                .analysis(analysis)
                .analysisType(request.getAnalysisType())
                .language(request.getLanguage())
                .success(true)
                .build();
                
        } catch (Exception e) {
            logger.error("Error analyzing code with OpenAI", e);
            throw new AIServiceException("Failed to analyze code: " + e.getMessage(), e);
        }
    }
    
    @Override
    public boolean isAvailable() {
        // Always return true since API key will be provided by user
        return true;
    }
} 