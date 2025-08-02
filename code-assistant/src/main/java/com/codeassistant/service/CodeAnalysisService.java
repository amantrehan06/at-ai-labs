package com.codeassistant.service;

import com.codeassistant.model.AnalysisRequest;
import com.codeassistant.model.AnalysisResponse;
import com.codeassistant.model.StreamingAnalysisResponse;
import com.codeassistant.service.ai.AIChatService;
import com.codeassistant.service.ai.AIServiceException;
import com.codeassistant.service.factory.AIServiceFactory;
import dev.langchain4j.memory.ChatMemory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import com.codeassistant.model.AnalysisType;

/**
 * Main service for code analysis that uses the Strategy pattern with Factory pattern.
 * This service acts as a facade and can work with different AI providers dynamically.
 * Now includes session management for conversation memory.
 */
@Service
public class CodeAnalysisService {
    
    private static final Logger logger = LoggerFactory.getLogger(CodeAnalysisService.class);
    
    private final AIServiceFactory aiServiceFactory;
    private final SessionManager sessionManager;
    
    @Autowired
    public CodeAnalysisService(AIServiceFactory aiServiceFactory, SessionManager sessionManager) {
        this.aiServiceFactory = aiServiceFactory;
        this.sessionManager = sessionManager;
        logger.info("CodeAnalysisService initialized with AI service factory and session manager");
    }
    
    /**
     * Analyzes code using the best available AI service with session memory.
     * 
     * @param request The analysis request
     * @return AnalysisResponse with the results
     * @throws AIServiceException if no AI service is available or session is invalid
     */
    public AnalysisResponse analyzeCode(AnalysisRequest request) throws AIServiceException {
        logger.debug("Analyzing code with request: {} for session: {}", request.getAnalysisType(), request.getSessionId());
        
        // Validate session exists
        if (!sessionManager.sessionExists(request.getSessionId())) {
            throw new AIServiceException("Session not found: " + request.getSessionId());
        }
        
        try {
            AIChatService service = aiServiceFactory.getBestAvailableService();
            logger.info("Using AI service: {} for session: {}", service.getClass().getSimpleName(), request.getSessionId());
            
            // Get session memory and enhance request with conversation context
            ChatMemory sessionMemory = sessionManager.getSessionMemory(request.getSessionId());
            AnalysisRequest enhancedRequest = enhanceRequestWithMemory(request, sessionMemory);
            
            // Log the exact prompt being sent to the LLM
            logger.info("LLM prompt sent - Session: {}, Type: {}, Language: {}, Prompt length: {}", 
                request.getSessionId(), enhancedRequest.getAnalysisType(), enhancedRequest.getLanguage(), enhancedRequest.getCode().length());
            logger.info("=== FULL PROMPT SENT TO LLM ===");
            logger.info(enhancedRequest.getCode());
            logger.info("=== END OF PROMPT ===");
            
            AnalysisResponse response = service.analyzeCode(enhancedRequest);
            
            // Log the LLM response
            logger.info("LLM response received - Session: {}, Response length: {}", 
                request.getSessionId(), response.getAnalysis().length());
            logger.info("=== LLM RESPONSE ===");
            logger.info(response.getAnalysis());
            logger.info("=== END OF RESPONSE ===");
            
            // Add messages to chat memory for future follow-ups
            addToChatMemory(sessionMemory, request, response);
            
            // Update response with session information
            response.setSessionId(request.getSessionId());
            response.setConversationContext("Session: " + request.getSessionId() + " | Messages in memory: " + 
                (sessionMemory.messages().size() + 1)); // +1 for current message
            
            logger.debug("Analysis completed successfully for session: {}", request.getSessionId());
            return response;
        } catch (AIServiceException e) {
            logger.error("AI service failed to analyze code for session: {}", request.getSessionId(), e);
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error during code analysis for session: {}", request.getSessionId(), e);
            throw new AIServiceException("Unexpected error during code analysis: " + e.getMessage(), e);
        }
    }
    
    /**
     * Analyzes code using a specific AI service with session memory.
     * 
     * @param request The analysis request
     * @param serviceName The name of the service to use (e.g., "OpenAIChatService", "LlamaAIChatService")
     * @return AnalysisResponse with the results
     * @throws AIServiceException if the service is not available or session is invalid
     */
    public AnalysisResponse analyzeCode(AnalysisRequest request, String serviceName) throws AIServiceException {
        logger.debug("Analyzing code with request: {} using service: {} for session: {}", 
            request.getAnalysisType(), serviceName, request.getSessionId());
        
        // Validate session exists
        if (!sessionManager.sessionExists(request.getSessionId())) {
            throw new AIServiceException("Session not found: " + request.getSessionId());
        }
        
        try {
            AIChatService service = aiServiceFactory.getService(serviceName);
            logger.info("Using specific AI service: {} for session: {}", service.getClass().getSimpleName(), request.getSessionId());
            
            // Get session memory and enhance request with conversation context
            ChatMemory sessionMemory = sessionManager.getSessionMemory(request.getSessionId());
            AnalysisRequest enhancedRequest = enhanceRequestWithMemory(request, sessionMemory);
            
            // Log the exact prompt being sent to the LLM
            logger.info("LLM prompt sent - Session: {}, Type: {}, Language: {}, Prompt length: {}", 
                request.getSessionId(), enhancedRequest.getAnalysisType(), enhancedRequest.getLanguage(), enhancedRequest.getCode().length());
            logger.info("=== FULL PROMPT SENT TO LLM ===");
            logger.info(enhancedRequest.getCode());
            logger.info("=== END OF PROMPT ===");
            
            AnalysisResponse response = service.analyzeCode(enhancedRequest);
            
            // Log the LLM response
            logger.info("LLM response received - Session: {}, Response length: {}", 
                request.getSessionId(), response.getAnalysis().length());
            logger.info("=== LLM RESPONSE ===");
            logger.info(response.getAnalysis());
            logger.info("=== END OF RESPONSE ===");
            
            // Add messages to chat memory for future follow-ups
            addToChatMemory(sessionMemory, request, response);
            
            // Update response with session information
            response.setSessionId(request.getSessionId());
            response.setConversationContext("Session: " + request.getSessionId() + " | Messages in memory: " + 
                (sessionMemory.messages().size() + 1)); // +1 for current message
            
            logger.debug("Analysis completed successfully for session: {}", request.getSessionId());
            return response;
        } catch (AIServiceException e) {
            logger.error("AI service failed to analyze code for session: {}", request.getSessionId(), e);
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error during code analysis for session: {}", request.getSessionId(), e);
            throw new AIServiceException("Unexpected error during code analysis: " + e.getMessage(), e);
        }
    }
    
    /**
     * Streams code analysis using the best available AI service with session memory.
     * 
     * @param request The analysis request
     * @return Flux of StreamingAnalysisResponse for real-time streaming
     * @throws AIServiceException if no AI service is available or session is invalid
     */
    public Flux<StreamingAnalysisResponse> streamAnalysis(AnalysisRequest request) throws AIServiceException {
        logger.debug("Streaming analysis with request: {} for session: {}", request.getAnalysisType(), request.getSessionId());
        
        // Validate session exists
        if (!sessionManager.sessionExists(request.getSessionId())) {
            throw new AIServiceException("Session not found: " + request.getSessionId());
        }
        
        try {
            AIChatService service = aiServiceFactory.getBestAvailableService();
            logger.info("Using AI service for streaming: {} for session: {}", service.getClass().getSimpleName(), request.getSessionId());
            
            // Get session memory and enhance request with conversation context
            ChatMemory sessionMemory = sessionManager.getSessionMemory(request.getSessionId());
            AnalysisRequest enhancedRequest = enhanceRequestWithMemory(request, sessionMemory);
            
            // Log the exact prompt being sent to the LLM
            logger.info("LLM prompt sent - Session: {}, Type: {}, Language: {}, Prompt length: {}", 
                request.getSessionId(), enhancedRequest.getAnalysisType(), enhancedRequest.getLanguage(), enhancedRequest.getCode().length());
            logger.info("=== FULL PROMPT SENT TO LLM ===");
            logger.info(enhancedRequest.getCode());
            logger.info("=== END OF PROMPT ===");
            
            Flux<StreamingAnalysisResponse> response = service.streamAnalysis(enhancedRequest);
            
            // Collect the full response and add to memory after completion
            return response.collectList()
                .flatMapMany(streamingResponses -> {
                    // Combine all streaming responses into a single analysis
                    StringBuilder fullAnalysis = new StringBuilder();
                    for (StreamingAnalysisResponse streamingResponse : streamingResponses) {
                        if (streamingResponse.getContent() != null) {
                            fullAnalysis.append(streamingResponse.getContent());
                        }
                    }
                    
                    // Log the complete LLM response
                    String completeResponse = fullAnalysis.toString();
                    logger.info("LLM response received - Session: {}, Response length: {}", 
                        request.getSessionId(), completeResponse.length());
                    logger.info("=== LLM RESPONSE ===");
                    logger.info(completeResponse);
                    logger.info("=== END OF RESPONSE ===");
                    
                    // Create a mock AnalysisResponse for memory storage
                    AnalysisResponse mockResponse = AnalysisResponse.builder()
                        .analysis(completeResponse)
                        .success(true)
                        .sessionId(request.getSessionId())
                        .build();
                    
                    // Add to chat memory
                    addToChatMemory(sessionMemory, request, mockResponse);
                    
                    // Return the original streaming responses
                    return Flux.fromIterable(streamingResponses);
                });
            
        } catch (AIServiceException e) {
            logger.error("AI service failed to stream analysis for session: {}", request.getSessionId(), e);
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error during streaming analysis for session: {}", request.getSessionId(), e);
            throw new AIServiceException("Unexpected error during streaming analysis: " + e.getMessage(), e);
        }
    }
    
    /**
     * Streams code analysis using a specific AI service with session memory.
     * 
     * @param request The analysis request
     * @param serviceName The name of the service to use (e.g., "OpenAIChatService", "LlamaAIChatService")
     * @return Flux of StreamingAnalysisResponse for real-time streaming
     * @throws AIServiceException if the service is not available or session is invalid
     */
    public Flux<StreamingAnalysisResponse> streamAnalysis(AnalysisRequest request, String serviceName) throws AIServiceException {
        logger.debug("Streaming analysis with request: {} using service: {} for session: {}", 
            request.getAnalysisType(), serviceName, request.getSessionId());
        
        // Validate session exists
        if (!sessionManager.sessionExists(request.getSessionId())) {
            throw new AIServiceException("Session not found: " + request.getSessionId());
        }
        
        try {
            AIChatService service = aiServiceFactory.getService(serviceName);
            logger.info("Using specific AI service for streaming: {} for session: {}", service.getClass().getSimpleName(), request.getSessionId());
            
            // Get session memory and enhance request with conversation context
            ChatMemory sessionMemory = sessionManager.getSessionMemory(request.getSessionId());
            AnalysisRequest enhancedRequest = enhanceRequestWithMemory(request, sessionMemory);
            
            // Log the exact prompt being sent to the LLM
            logger.info("LLM prompt sent - Session: {}, Type: {}, Language: {}, Prompt length: {}", 
                request.getSessionId(), enhancedRequest.getAnalysisType(), enhancedRequest.getLanguage(), enhancedRequest.getCode().length());
            logger.info("=== FULL PROMPT SENT TO LLM ===");
            logger.info(enhancedRequest.getCode());
            logger.info("=== END OF PROMPT ===");
            
            Flux<StreamingAnalysisResponse> response = service.streamAnalysis(enhancedRequest);
            
            // Collect the full response and add to memory after completion
            return response.collectList()
                .flatMapMany(streamingResponses -> {
                    // Combine all streaming responses into a single analysis
                    StringBuilder fullAnalysis = new StringBuilder();
                    for (StreamingAnalysisResponse streamingResponse : streamingResponses) {
                        if (streamingResponse.getContent() != null) {
                            fullAnalysis.append(streamingResponse.getContent());
                        }
                    }
                    
                    // Log the complete LLM response
                    String completeResponse = fullAnalysis.toString();
                    logger.info("LLM response received - Session: {}, Response length: {}", 
                        request.getSessionId(), completeResponse.length());
                    logger.info("=== LLM RESPONSE ===");
                    logger.info(completeResponse);
                    logger.info("=== END OF RESPONSE ===");
                    
                    // Create a mock AnalysisResponse for memory storage
                    AnalysisResponse mockResponse = AnalysisResponse.builder()
                        .analysis(completeResponse)
                        .success(true)
                        .sessionId(request.getSessionId())
                        .build();
                    
                    // Add to chat memory
                    addToChatMemory(sessionMemory, request, mockResponse);
                    
                    // Return the original streaming responses
                    return Flux.fromIterable(streamingResponses);
                });
            
        } catch (AIServiceException e) {
            logger.error("AI service failed to stream analysis for session: {}", request.getSessionId(), e);
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error during streaming analysis for session: {}", request.getSessionId(), e);
            throw new AIServiceException("Unexpected error during streaming analysis: " + e.getMessage(), e);
        }
    }
    
    /**
     * Enhances the analysis request with conversation memory context.
     * 
     * @param request The original request
     * @param sessionMemory The session's chat memory
     * @return Enhanced request with conversation context
     */
    private AnalysisRequest enhanceRequestWithMemory(AnalysisRequest request, ChatMemory sessionMemory) {
        // For follow-up questions, we use a different approach
        if (AnalysisType.FOLLOWUP.equals(request.getAnalysisType())) {
            return handleFollowUpRequest(request, sessionMemory);
        }
        
        // For regular requests, enhance the code with conversation context
        StringBuilder enhancedCode = new StringBuilder();
        
        // Add conversation context if there are previous messages
        if (!sessionMemory.messages().isEmpty()) {
            enhancedCode.append("// Previous conversation context:\n");
            sessionMemory.messages().forEach(message -> {
                enhancedCode.append("// ").append(message.type()).append(": ").append(message.text()).append("\n");
            });
            enhancedCode.append("\n// Current request:\n");
        }
        
        // Handle null code field (for follow-up requests)
        String codeContent = request.getCode() != null ? request.getCode() : "";
        enhancedCode.append(codeContent);
        
        String finalPrompt = enhancedCode.toString();
        
        // Log the regular request enhancement
        logger.info("Regular request enhanced - Type: {}, Language: {}, Session: {}, Memory: {} messages", 
            request.getAnalysisType(), request.getLanguage(), request.getSessionId(), sessionMemory.messages().size());
        
        return new AnalysisRequest(
            finalPrompt,
            request.getAnalysisType(),
            request.getLanguage(),
            request.getSessionId(),
            request.getApiKey()
        );
    }
    
    /**
     * Handles follow-up requests with a special system prompt.
     * 
     * @param request The follow-up request
     * @param sessionMemory The session's chat memory
     * @return Enhanced request for follow-up
     */
    private AnalysisRequest handleFollowUpRequest(AnalysisRequest request, ChatMemory sessionMemory) {
        StringBuilder enhancedCode = new StringBuilder();
        
        // Add a special system prompt for follow-ups
        enhancedCode.append("// SYSTEM: This is a follow-up question in an ongoing conversation.\n");
        enhancedCode.append("// Please provide a helpful response that builds on the previous conversation.\n");
        enhancedCode.append("// Be conversational and helpful. If the question is about code, provide code examples.\n\n");
        
        // Add conversation context
        if (!sessionMemory.messages().isEmpty()) {
            enhancedCode.append("// Previous conversation context:\n");
            sessionMemory.messages().forEach(message -> {
                enhancedCode.append("// ").append(message.type()).append(": ").append(message.text()).append("\n");
            });
            enhancedCode.append("\n// Follow-up question:\n");
        } else {
            logger.warn("Session memory is empty for follow-up request: {}", request.getSessionId());
        }
        
        // Use the code field as the follow-up question
        enhancedCode.append(request.getCode());
        
        String finalPrompt = enhancedCode.toString();
        
        // Log the special follow-up prompt
        logger.info("Follow-up request processed - Session: {}, Memory: {} messages, Question: {}", 
            request.getSessionId(), sessionMemory.messages().size(), request.getCode());
        
        return new AnalysisRequest(
            finalPrompt,
            request.getAnalysisType(),
            request.getLanguage(),
            request.getSessionId(),
            request.getApiKey()
        );
    }
    
    /**
     * Adds messages to the chat memory after processing a response.
     * This is useful for follow-up questions or subsequent interactions.
     * 
     * @param sessionMemory The session's chat memory
     * @param request The original request
     * @param response The analysis response
     */
    private void addToChatMemory(ChatMemory sessionMemory, AnalysisRequest request, AnalysisResponse response) {
        try {
            int memoryBefore = sessionMemory.messages().size();
            
            // Add the user message to the memory
            sessionMemory.add(dev.langchain4j.data.message.UserMessage.from(request.getCode()));
            
            // Add the AI response to the memory
            sessionMemory.add(dev.langchain4j.data.message.AiMessage.from(response.getAnalysis()));
            
            int memoryAfter = sessionMemory.messages().size();
            
            logger.info("Memory updated - Session: {}, Before: {} messages, After: {} messages, User message length: {}, AI response length: {}", 
                request.getSessionId(), memoryBefore, memoryAfter, request.getCode().length(), response.getAnalysis().length());
                
        } catch (Exception e) {
            logger.warn("Failed to add messages to chat memory for session: {}", request.getSessionId(), e);
        }
    }
    
    /**
     * Gets the list of available AI services.
     * 
     * @return Map of available AI chat services
     */
    public java.util.Map<String, AIChatService> getAvailableServices() {
        return aiServiceFactory.getAllAvailableServices();
    }
    
    /**
     * Checks if any AI service is available.
     * 
     * @return true if at least one AI service is available
     */
    public boolean hasAvailableService() {
        return aiServiceFactory.hasAvailableServices();
    }
    
    /**
     * Gets the number of available services.
     * 
     * @return Number of available services
     */
    public int getAvailableServiceCount() {
        return aiServiceFactory.getAvailableServiceCount();
    }
} 