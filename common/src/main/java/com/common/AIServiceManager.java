package com.common;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Centralized AI Service Manager
 * 
 * Provides centralized AI model management for all modules.
 * Handles different AI providers and model selection.
 */
@Service
public class AIServiceManager {

    @Value("${openai.api.key:}")
    private String openaiApiKey;

    @Value("${groq.api.key:}")
    private String groqApiKey;

    @Value("${openai.model:" + AIServiceConstants.DEFAULT_OPENAI_MODEL + "}")
    private String openaiModel;

    @Value("${groq.model:" + AIServiceConstants.DEFAULT_GROQ_MODEL + "}")
    private String groqModel;

    private final Map<String, ChatLanguageModel> chatModels = new HashMap<>();

    /**
     * Get OpenAI chat model
     */
    public ChatLanguageModel getOpenAIModel(String apiKey) {
        String key = apiKey != null && !apiKey.trim().isEmpty() ? apiKey : openaiApiKey;
        if (key == null || key.trim().isEmpty()) {
            throw new IllegalArgumentException(AIServiceConstants.ERROR_OPENAI_API_KEY_REQUIRED);
        }

        String cacheKey = AIServiceConstants.OPENAI_CACHE_PREFIX + key.hashCode();
        if (!chatModels.containsKey(cacheKey)) {
            String modelName = openaiModel.trim();
            
            chatModels.put(cacheKey, OpenAiChatModel.builder()
                .apiKey(key)
                .modelName(modelName)
                .timeout(Duration.ofSeconds(AIServiceConstants.DEFAULT_TIMEOUT_SECONDS))
                .build());
        }
        return chatModels.get(cacheKey);
    }

    /**
     * Get Groq chat model (Llama)
     */
    public ChatLanguageModel getGroqModel(String apiKey) {
        String key = apiKey != null && !apiKey.trim().isEmpty() ? apiKey : groqApiKey;
        if (key == null || key.trim().isEmpty()) {
            throw new IllegalArgumentException(AIServiceConstants.ERROR_GROQ_API_KEY_REQUIRED);
        }

        String cacheKey = AIServiceConstants.GROQ_CACHE_PREFIX + key.hashCode();
        if (!chatModels.containsKey(cacheKey)) {
            String modelName = groqModel.trim();
            
            chatModels.put(cacheKey, OpenAiChatModel.builder()
                .apiKey(key)
                .baseUrl(AIServiceConstants.GROQ_BASE_URL)
                .modelName(modelName)
                .timeout(Duration.ofSeconds(AIServiceConstants.DEFAULT_TIMEOUT_SECONDS))
                .temperature(AIServiceConstants.GROQ_DEFAULT_TEMPERATURE)
                .build());
        }
        return chatModels.get(cacheKey);
    }

    /**
     * Get AI model by service name
     */
    public ChatLanguageModel getModel(String serviceName, String apiKey) {
        switch (serviceName.toLowerCase()) {
            case AIServiceConstants.OPENAI_SERVICE:
                return getOpenAIModel(apiKey);
            case AIServiceConstants.GROQ_SERVICE:
                return getGroqModel(apiKey);
            default:
                throw new IllegalArgumentException(AIServiceConstants.ERROR_UNKNOWN_SERVICE + serviceName);
        }
    }

    /**
     * Check if AI service is available
     */
    public boolean isServiceAvailable(String serviceName) {
        switch (serviceName.toLowerCase()) {
            case AIServiceConstants.OPENAI_SERVICE:
                return openaiApiKey != null && !openaiApiKey.isEmpty();
            case AIServiceConstants.GROQ_SERVICE:
                return groqApiKey != null && !groqApiKey.isEmpty();
            default:
                return false;
        }
    }

    /**
     * Get available services
     */
    public Map<String, Boolean> getAvailableServices() {
        Map<String, Boolean> services = new HashMap<>();
        services.put(AIServiceConstants.OPENAI_DISPLAY_NAME, isServiceAvailable(AIServiceConstants.OPENAI_SERVICE));
        services.put(AIServiceConstants.GROQ_DISPLAY_NAME, isServiceAvailable(AIServiceConstants.GROQ_SERVICE));
        return services;
    }
} 