package com.common;

/**
 * AI Service Constants
 * 
 * Centralized constants for AI service names, model names, and configuration.
 * Prevents spelling mistakes and ensures consistency across all modules.
 */
public final class AIServiceConstants {

    // Private constructor to prevent instantiation
    private AIServiceConstants() {}

    // AI Service Names
    public static final String OPENAI_SERVICE = "openai";
    public static final String GROQ_SERVICE = "groq";

    // Model Names
    public static final String OPENAI_MODEL_GPT_3_5_TURBO = "gpt-3.5-turbo";
    public static final String OPENAI_MODEL_GPT_4 = "gpt-4";
    public static final String GROQ_MODEL_LLAMA_3_8B = "llama3-8b-8192";
    public static final String GROQ_MODEL_LLAMA_3_70B = "llama3-70b-8192";

    // Default Models
    public static final String DEFAULT_OPENAI_MODEL = OPENAI_MODEL_GPT_3_5_TURBO;
    public static final String DEFAULT_GROQ_MODEL = GROQ_MODEL_LLAMA_3_8B;

    // Configuration Properties
    public static final String OPENAI_API_KEY_PROPERTY = "openai.api.key";
    public static final String GROQ_API_KEY_PROPERTY = "groq.api.key";
    public static final String OPENAI_MODEL_PROPERTY = "openai.model";
    public static final String GROQ_MODEL_PROPERTY = "groq.model";

    // Error Messages
    public static final String ERROR_OPENAI_API_KEY_REQUIRED = "OpenAI API key is required";
    public static final String ERROR_GROQ_API_KEY_REQUIRED = "Groq API key is required";
    public static final String ERROR_UNKNOWN_SERVICE = "Unknown AI service: ";

    // Service Display Names
    public static final String OPENAI_DISPLAY_NAME = "OpenAI";
    public static final String GROQ_DISPLAY_NAME = "Groq";

    // Cache Keys
    public static final String OPENAI_CACHE_PREFIX = "openai-";
    public static final String GROQ_CACHE_PREFIX = "groq-";
    public static final String OPENAI_STREAMING_CACHE_PREFIX = "openai-streaming-";
    public static final String GROQ_STREAMING_CACHE_PREFIX = "groq-streaming-";

    // Timeout Configuration
    public static final int DEFAULT_TIMEOUT_SECONDS = 60;
    public static final double GROQ_DEFAULT_TEMPERATURE = 0.3;

    // Base URLs
    public static final String GROQ_BASE_URL = "https://api.groq.com/openai/v1";
} 