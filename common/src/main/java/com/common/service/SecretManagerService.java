package com.common.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Service for managing API keys from system properties or environment variables
 * Hybrid approach: System properties (VM options) take priority, fallback to environment variables
 */
@Service
public class SecretManagerService {
    
    private static final Logger logger = LoggerFactory.getLogger(SecretManagerService.class);
    
    // API Key Constants
    public static final String OPENAI_API_KEY_NAME = "OPENAI_API_KEY";
    public static final String GROK_API_KEY_NAME = "GROK_API_KEY";
    
    // Model Constants
    public static final String OPENAI_MODEL_GPT_3_5_TURBO = "gpt-3.5-turbo";
    public static final String OPENAI_MODEL_GPT_4 = "gpt-4";
    public static final String GROQ_MODEL_LLAMA_3_8B = "llama3-8b-8192";
    public static final String GROQ_MODEL_LLAMA_3_70B = "llama3-70b-8192";
    
    // Default Models
    public static final String DEFAULT_OPENAI_MODEL = OPENAI_MODEL_GPT_3_5_TURBO;
    public static final String DEFAULT_GROQ_MODEL = GROQ_MODEL_LLAMA_3_8B;
    
    /**
     * Get OpenAI API key from system property or environment variable
     * Priority: System Property (VM options) > Environment Variable
     */
    public String getOpenAIApiKey() {
        // First try system property (for local VM options)
        String key = System.getProperty(OPENAI_API_KEY_NAME);
        
        // Fall back to environment variable (for Cloud Run)
        if (key == null || key.trim().isEmpty()) {
            key = System.getenv(OPENAI_API_KEY_NAME);
            if (key != null && !key.trim().isEmpty()) {
                logger.info("Retrieved OpenAI API key from environment variable");
            }
        } else {
            logger.info("Retrieved OpenAI API key from system property (VM option)");
        }
        
        if (key == null || key.trim().isEmpty()) {
            logger.error("OpenAI API key not found in system properties or environment variables");
            throw new RuntimeException(OPENAI_API_KEY_NAME + " is required. Set as VM option (-D" + OPENAI_API_KEY_NAME + "=...) or environment variable");
        }
        
        return key;
    }
    
    /**
     * Get Groq API key from system property or environment variable
     * Priority: System Property (VM options) > Environment Variable
     */
    public String getGroqApiKey() {
        // First try system property (for local VM options)
        String key = System.getProperty(GROK_API_KEY_NAME);
        
        // Fall back to environment variable (for Cloud Run)
        if (key == null || key.trim().isEmpty()) {
            key = System.getenv(GROK_API_KEY_NAME);
            if (key != null && !key.trim().isEmpty()) {
                logger.info("Retrieved Groq API key from environment variable");
            }
        } else {
            logger.info("Retrieved Groq API key from system property (VM option)");
        }
        
        if (key == null || key.trim().isEmpty()) {
            logger.error("Groq API key not found in system properties or environment variables");
            throw new RuntimeException(GROK_API_KEY_NAME + " is required. Set as VM option (-D" + GROK_API_KEY_NAME + "=...) or environment variable");
        }
        
        return key;
    }
    
    /**
     * Get the default OpenAI model name
     */
    public String getDefaultOpenAIModel() {
        return DEFAULT_OPENAI_MODEL;
    }
    
    /**
     * Get the default Groq model name
     */
    public String getDefaultGroqModel() {
        return DEFAULT_GROQ_MODEL;
    }
} 