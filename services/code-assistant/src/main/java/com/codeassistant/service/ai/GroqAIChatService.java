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
 * Groq AI implementation of the AIChatService using Strategy pattern.
 * This class uses OpenAiChatModel with Groq's OpenAI-compatible API endpoint.
 */
@Service
public class GroqAIChatService implements AIChatService {
    
    private static final Logger logger = LoggerFactory.getLogger(GroqAIChatService.class);
    private static final String GROQ_BASE_URL = "https://api.groq.com/openai/v1";
    private static final String DEFAULT_GROQ_API_KEY = "";
    
    private final String model;
    private final Map<com.codeassistant.model.AnalysisType, AnalysisStrategy> analysisStrategies;
    
    public GroqAIChatService(@Value("${groq.model:llama3-8b-8192}") String model,
                             @Autowired List<AnalysisStrategy> strategies) {
        
        // Initialize analysis strategies
        this.analysisStrategies = strategies.stream()
            .collect(Collectors.toMap(AnalysisStrategy::getAnalysisType, strategy -> strategy));
        
        this.model = model;
        logger.info("Groq AI chat service initialized with {} analysis strategies", strategies.size());
    }
    
    @Override
    public AnalysisResponse analyzeCode(AnalysisRequest request) throws AIServiceException {
        try {
            // Get API key from request or use default
            String apiKey = request.getApiKey();
            if (apiKey == null || apiKey.trim().isEmpty()) {
                apiKey = DEFAULT_GROQ_API_KEY;
                logger.debug("Using default Groq API key");
            }
            
            // Get the appropriate strategy for the analysis type
            AnalysisStrategy strategy = analysisStrategies.get(request.getAnalysisType());
            if (strategy == null) {
                throw new AIServiceException("No analysis strategy found for type: " + request.getAnalysisType());
            }
            
            // Create Groq client with API key from request or default
            ChatLanguageModel chatModel = OpenAiChatModel.builder()
                .apiKey(apiKey)
                .baseUrl(GROQ_BASE_URL)
                .modelName(model)
                .timeout(Duration.ofSeconds(60))
                .temperature(0.3)
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
            logger.error("Error analyzing code with Groq AI", e);
            throw new AIServiceException("Failed to analyze code with Groq: " + e.getMessage(), e);
        }
    }
    
    @Override
    public boolean isAvailable() {
        // Always return true since API key will be provided by user
        return true;
    }
} 