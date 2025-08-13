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