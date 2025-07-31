package com.codeassistant.service.ai;

import com.codeassistant.model.AnalysisRequest;
import com.codeassistant.model.AnalysisResponse;
import com.codeassistant.model.StreamingAnalysisResponse;
import com.codeassistant.service.strategy.AnalysisStrategy;
import com.common.AIServiceManager;
import com.common.AIServiceConstants;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.StreamingResponseHandler;
import dev.langchain4j.model.output.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

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
    private final AIServiceManager aiServiceManager;
    
    public OpenAIChatService(@Autowired AIServiceManager aiServiceManager,
                            @Autowired List<AnalysisStrategy> strategies) {
        
        this.aiServiceManager = aiServiceManager;
        
        // Initialize analysis strategies
        this.analysisStrategies = strategies.stream()
            .collect(Collectors.toMap(AnalysisStrategy::getAnalysisType, strategy -> strategy));
        
        logger.info("OpenAI chat service initialized with {} analysis strategies", strategies.size());
    }
    
    @Override
    public AnalysisResponse analyzeCode(AnalysisRequest request) throws AIServiceException {
        try {
            // Get the appropriate strategy for the analysis type
            AnalysisStrategy strategy = analysisStrategies.get(request.getAnalysisType());
            if (strategy == null) {
                throw new AIServiceException("No analysis strategy found for type: " + request.getAnalysisType());
            }
            
            // Get OpenAI model from centralized manager using generic method
            ChatLanguageModel chatModel = aiServiceManager.getModel(AIServiceConstants.OPENAI_SERVICE, request.getApiKey());
            
            // Use the strategy to build messages (maintains Strategy pattern)
            List<ChatMessage> messages = strategy.buildMessages(request).toLangChain4jMessages();
            
            Response<AiMessage> response = chatModel.generate(messages);
            
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
    public Flux<StreamingAnalysisResponse> streamAnalysis(AnalysisRequest request) throws AIServiceException {
        try {
            // Get the appropriate strategy for the analysis type
            AnalysisStrategy strategy = analysisStrategies.get(request.getAnalysisType());
            if (strategy == null) {
                throw new AIServiceException("No analysis strategy found for type: " + request.getAnalysisType());
            }
            
            // Get OpenAI streaming model from centralized manager
            StreamingChatLanguageModel streamingModel = aiServiceManager.getStreamingModel(AIServiceConstants.OPENAI_SERVICE, request.getApiKey());
            
            // Use the strategy to build messages (maintains Strategy pattern)
            List<ChatMessage> messages = strategy.buildMessages(request).toLangChain4jMessages();

            // Implement proper streaming using Flux.create and StreamingResponseHandler
            return Flux.create(emitter -> {
                try {
                    streamingModel.generate(messages, new StreamingResponseHandler<AiMessage>() {
                        @Override
                        public void onNext(String token) {
                            // Emit each token as a content chunk
                            logger.info("Emitting content: "+token);
                            emitter.next(StreamingAnalysisResponse.contentChunk(token, request.getAnalysisType(), request.getLanguage()));
                        }

                        @Override
                        public void onComplete(Response<AiMessage> response) {
                            // Emit completion event
                            emitter.next(StreamingAnalysisResponse.complete(request.getAnalysisType(), request.getLanguage()));
                            emitter.complete();
                        }
                        
                        @Override
                        public void onError(Throwable error) {
                            // Emit error event
                            emitter.next(StreamingAnalysisResponse.error("Streaming error: " + error.getMessage(), 
                                request.getAnalysisType(), request.getLanguage()));
                            emitter.complete();
                        }
                    });
                } catch (Exception e) {
                    logger.error("Error in streaming generation", e);
                    emitter.next(StreamingAnalysisResponse.error("Failed to start streaming: " + e.getMessage(), 
                        request.getAnalysisType(), request.getLanguage()));
                    emitter.complete();
                }
            });
        } catch (Exception e) {
            logger.error("Error setting up streaming analysis with OpenAI", e);
            return Flux.just(StreamingAnalysisResponse.error("Failed to setup streaming analysis: " + e.getMessage(), 
                request.getAnalysisType(), request.getLanguage()));
        }
    }
    
    @Override
    public boolean isAvailable() {
        return true; // Service is available, user can provide API key
    }
} 