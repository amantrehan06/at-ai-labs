package com.documentrag.service;

import com.documentrag.model.DocumentChatRequest;
import com.documentrag.model.DocumentChatResponse;
import com.common.AIServiceManager;
import com.common.AIServiceConstants;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.output.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class DocumentChatService {

  @Autowired private PineconeEmbeddingStore embeddingStore;

  @Autowired private OpenAIEmbeddingModel embeddingModel;

  @Autowired private AIServiceManager aiServiceManager;
  
  @Autowired private IntentDetectionService intentDetectionService;

  // In-memory conversation history (in production, use Redis or database)
  private final ConcurrentHashMap<String, List<DocumentChatRequest.ChatMessage>>
      conversationHistory = new ConcurrentHashMap<>();

  public DocumentChatResponse chatWithDocuments(DocumentChatRequest request) {
    DocumentChatResponse response = new DocumentChatResponse();

    try {
      String userMessage = request.getMessage();
      String sessionId = request.getSessionId();
      String service = request.getService();

      log.info(
          "Processing chat request - Session: {}, Service: {}, Message: {}",
          sessionId,
          service,
          userMessage.substring(0, Math.min(userMessage.length(), 50)));

      if (userMessage == null || userMessage.trim().isEmpty()) {
        response.setSuccess(false);
        response.setMessage("Please provide a question to ask about your documents.");
        return response;
      }

      // Get conversation history for this session
      List<DocumentChatRequest.ChatMessage> history = getConversationHistory(sessionId);

      // Add user message to history
      DocumentChatRequest.ChatMessage userMsg = new DocumentChatRequest.ChatMessage();
      userMsg.setRole("user");
      userMsg.setContent(userMessage);
      userMsg.setTimestamp(String.valueOf(System.currentTimeMillis()));
      history.add(userMsg);

      // Search for relevant documents
      List<TextSegment> relevantDocs = searchRelevantDocuments(userMessage, sessionId);
      log.info(
          "Found {} relevant documents for query in session {}", relevantDocs.size(), sessionId);

      // Generate AI response
      String aiResponse = generateAIResponse(userMessage, relevantDocs, history);

      // Add assistant response to conversation history
      DocumentChatRequest.ChatMessage assistantMsg = new DocumentChatRequest.ChatMessage();
      assistantMsg.setRole("assistant");
      assistantMsg.setContent(aiResponse);
      assistantMsg.setTimestamp(String.valueOf(System.currentTimeMillis()));
      history.add(assistantMsg);

      // Store the updated conversation history back to the ConcurrentHashMap
      conversationHistory.put(sessionId, history);
      
      log.info("=== CONVERSATION HISTORY STORED ===");
      log.info("Final history size: {}", history.size());
      log.info("History stored in ConcurrentHashMap for session: {}", sessionId);
      log.info("=== END CONVERSATION HISTORY STORED ===");

      // Build response
      response.setSuccess(true);
      response.setMessage("Response generated successfully");
      response.setAnswer(aiResponse);
      response.setResponse(aiResponse); // Set response field for UI compatibility
      response.setSessionId(sessionId);
      
      // Convert TextSegments to strings for the response
      List<String> relevantDocStrings = relevantDocs.stream()
          .map(segment -> String.format("[%s] %s.%s (Lines %s-%s)", 
              segment.metadata().get("type"),
              segment.metadata().get("class"),
              segment.metadata().get("name"),
              segment.metadata().get("startLine"),
              segment.metadata().get("endLine")))
          .collect(Collectors.toList());
      
      response.setRelevantDocuments(relevantDocStrings);
      response.setConversationHistory(history);

      log.info(
          "Chat response generated successfully - Session: {}, Response length: {}",
          sessionId,
          aiResponse.length());

    } catch (Exception e) {
      log.error("Error processing chat request: {}", e.getMessage(), e);
      response.setSuccess(false);
      response.setMessage("Error processing chat request: " + e.getMessage());
    }

    return response;
  }

  public void addDocumentToVectorStore(
      String documentId, List<TextSegment> segments, String documentType) {
    try {
      log.info(
          "Adding document to vector store - ID: {}, Type: {}, Segments: {}",
          documentId,
          documentType,
          segments.size());

      // Store pre-split segments in embedding store (metadata is already preserved from Document)
      for (TextSegment segment : segments) {
        log.info(
            "Storing segment in embedding store"+ segment);
        Embedding embedding = embeddingModel.embed(segment).content();
        embeddingStore.add(embedding, segment);
      }

      log.info(
          "Successfully added document {} to vector store with {} segments",
          documentId,
          segments.size());

    } catch (Exception e) {
      log.error("Error adding document to vector store: {}", e.getMessage(), e);
    }
  }

    /**
     * Search for relevant documents using hybrid search (semantic + metadata filtering)
     */
    private List<TextSegment> searchRelevantDocuments(String userMessage, String sessionId) {
        try {
            // Use embedding-based intent detection instead of hardcoded regex
            IntentDetectionService.IntentDetectionResult intentResult = intentDetectionService.detectSearchIntent(userMessage);
            IntentDetectionService.SearchIntent searchIntent = intentResult.getIntent();
            double confidence = intentResult.getConfidence();
            
            log.info("Search intent detected: {} (confidence: {}) for query: '{}'", 
                searchIntent, confidence, userMessage);
            
            // Create metadata filter based on search intent
            Map<String, String> metadataFilter = new HashMap<>();
            metadataFilter.put("sessionId", sessionId);
            
            // Add type filtering based on intent
            if (searchIntent != IntentDetectionService.SearchIntent.GENERAL) {
                metadataFilter.put("type", searchIntent.getPineconeFilter());
            }
            
            log.info("Using metadata filter: {}", metadataFilter);
            
            // Generate embedding for the user message first
            Embedding queryEmbedding = embeddingModel.embed(userMessage).content();
            
            // Search with hybrid approach: semantic + metadata filtering
            List<dev.langchain4j.store.embedding.EmbeddingMatch<TextSegment>> matches = embeddingStore.findRelevant(
                queryEmbedding, 
                10, // topK
                0.0, // minScore - lower threshold for intent-based filtering
                metadataFilter
            );
            
            log.info("Pinecone search completed - Found {} matches with intent: {} (confidence: {})", 
                matches.size(), searchIntent, confidence);
            
            // Log detailed results for debugging
            for (int i = 0; i < matches.size(); i++) {
                dev.langchain4j.store.embedding.EmbeddingMatch<TextSegment> match = matches.get(i);
                TextSegment segment = match.embedded();
                String segmentText = segment.text();
                String segmentType = segment.metadata().get("type");
                String segmentName = segment.metadata().get("name");
                String segmentClass = segment.metadata().get("class");
                
                log.info("Match {} - Score: {}, Type: {}, Name: {}, Class: {}, Complete Text: {}", 
                    i + 1, match.score(), segmentType, segmentName, segmentClass, segmentText);
            }
            
            // Convert to TextSegment list
            return matches.stream()
                .map(dev.langchain4j.store.embedding.EmbeddingMatch::embedded)
                .collect(Collectors.toList());
                
        } catch (Exception e) {
            log.error("Error searching relevant documents: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

  private String generateAIResponse(
      String userMessage,
      List<TextSegment> relevantDocs,
      List<DocumentChatRequest.ChatMessage> history) {
    try {
      // Get the LLM model from AIServiceManager (default to OpenAI)
      ChatLanguageModel chatModel =
          aiServiceManager.getModel(AIServiceConstants.OPENAI_SERVICE, null);

      StringBuilder systemPrompt = new StringBuilder();

      // Check if this is the first request (no previous conversation history)
      boolean isFirstRequest = history.size() <= 1; // Only user message exists
      

      log.info("isFirstRequest: {}", isFirstRequest);
      
      // Use embedding-based intent detection for targeted responses
      IntentDetectionService.IntentDetectionResult intentResult = intentDetectionService.detectSearchIntent(userMessage);
      IntentDetectionService.SearchIntent searchIntent = intentResult.getIntent();
      double confidence = intentResult.getConfidence();

      if (isFirstRequest) {
        // First request: Use the detailed system prompt
        systemPrompt.append(
            "You are a helpful AI assistant that helps developers understand Java code. ");
        systemPrompt.append(
            "Use the following relevant code segments to answer the user's question. ");
        systemPrompt.append("If the information is not in the code, say so clearly. ");
        systemPrompt.append(
            "Provide accurate, helpful responses based only on the code content.\n\n");
        systemPrompt.append(
            "IMPORTANT: Format your response in a clear, readable way. Use bullet points, numbered lists, and proper spacing to make the information easy to read.\n\n");
      } else {
        // Follow-up request: Use a concise system prompt
        systemPrompt.append(
            "You are a helpful AI assistant continuing a conversation about Java code. ");
        systemPrompt.append(
            "Use the relevant code segments and conversation history to provide accurate, helpful responses.\n\n");
      }

      // Add specific instructions based on detected intent
      systemPrompt.append(String.format("Detected Intent: %s (confidence: %.2f%%)\n", 
          searchIntent, confidence * 100));
      
      switch (searchIntent) {
        case METHODS:
          systemPrompt.append("The user is asking about METHODS. Focus on method names, signatures, parameters, return types, and functionality. ");
          systemPrompt.append("If listing methods, provide a clear, organized list with method names and brief descriptions.\n\n");
          break;
        case CLASSES:
          systemPrompt.append("The user is asking about CLASSES. Focus on class names, inheritance, interfaces, and overall structure. ");
          systemPrompt.append("If listing classes, provide a clear, organized list with class names and brief descriptions.\n\n");
          break;
        case FIELDS:
          systemPrompt.append("The user is asking about FIELDS/VARIABLES. Focus on field names, types, modifiers, and purpose. ");
          systemPrompt.append("If listing fields, provide a clear, organized list with field names, types, and brief descriptions.\n\n");
          break;
        case CONSTRUCTORS:
          systemPrompt.append("The user is asking about CONSTRUCTORS. Focus on constructor names, parameters, and initialization logic. ");
          systemPrompt.append("If listing constructors, provide a clear, organized list with parameter details.\n\n");
          break;
        case PACKAGES:
          systemPrompt.append("The user is asking about PACKAGES. Focus on package structure and organization.\n\n");
          break;
        case IMPORTS:
          systemPrompt.append("The user is asking about IMPORTS. Focus on imported classes and their purposes.\n\n");
          break;
        default:
          systemPrompt.append("Provide a helpful response about the Java code. Use clear explanations and examples when possible.\n\n");
      }

      if (!relevantDocs.isEmpty()) {
        systemPrompt.append("Relevant code segments found:\n");
        for (int i = 0; i < relevantDocs.size(); i++) {
          TextSegment segment = relevantDocs.get(i);
          String type = segment.metadata().get("type");
          String name = segment.metadata().get("name");
          String className = segment.metadata().get("class");
          String packageName = segment.metadata().get("package");
          String startLine = segment.metadata().get("startLine");
          String endLine = segment.metadata().get("endLine");
          String modifiers = segment.metadata().get("modifiers");
          String javadoc = segment.metadata().get("javadoc");
          
          // Always include comprehensive metadata
          systemPrompt.append(String.format("%d. [%s] %s.%s (Lines %s-%s, Package: %s)\n", 
              i + 1, type.toUpperCase(), className, name, startLine, endLine, packageName));
          
          // Always include modifiers and javadoc if available
          if (modifiers != null && !modifiers.isEmpty()) {
            systemPrompt.append("   Modifiers: ").append(modifiers).append("\n");
          }
          if (javadoc != null && !javadoc.isEmpty()) {
            systemPrompt.append("   Javadoc: ").append(javadoc).append("\n");
          }
          
          // Always include the actual content for complete context
          String content = segment.text();
          systemPrompt.append("   Content:\n").append(content).append("\n\n");
        }
        systemPrompt.append("\n");
      }

      // Build conversation history context (for follow-up requests)
      if (!isFirstRequest && history.size() > 1) {
        systemPrompt.append("Previous conversation context:\n");
        // Skip the current user message (last in history) and get previous conversation
        for (int i = 0; i < history.size() - 1; i++) {
          DocumentChatRequest.ChatMessage msg = history.get(i);
          if (msg.getRole().equals("user")) {
            systemPrompt.append("User: ").append(msg.getContent()).append("\n");
          } else if (msg.getRole().equals("assistant")) {
            systemPrompt.append("Assistant: ").append(msg.getContent()).append("\n");
          }
        }
        systemPrompt.append("\n");
      }

      // Create system and user messages
      SystemMessage systemMessage = new SystemMessage(systemPrompt.toString());
      UserMessage userMsg = new UserMessage(userMessage);

      List<ChatMessage> messages = List.of(systemMessage, userMsg);

      // Log the complete prompt being sent to LLM
      log.info("=== COMPLETE PROMPT SENT TO LLM ===");
      log.info("Request Type: {}", isFirstRequest ? "FIRST_REQUEST" : "FOLLOW_UP_REQUEST");
      log.info("Search Intent: {} (confidence: {})", searchIntent, confidence);
      log.info("User Message: {}", userMessage);
      log.info("Relevant Documents Count: {}", relevantDocs.size());
      log.info("Conversation History Size: {}", history.size());

      log.info("LLM PROMPT: {}" , messages);
      log.info("=== END OF PROMPT ===");

      // Generate response from LLM
      Response<AiMessage> response = chatModel.generate(messages);
      String aiResponse = response.content().text();

      if (aiResponse != null && !aiResponse.trim().isEmpty()) {
        return aiResponse;
      } else {
        return "I apologize, but I couldn't generate a response. Please try rephrasing your question.";
      }

    } catch (Exception e) {
      log.error("Error generating AI response: {}", e.getMessage(), e);
      return "I encountered an error while processing your request. Please try again.";
    }
  }

  public void clearConversationHistory(String sessionId) {
    conversationHistory.remove(sessionId);
  }

  public List<DocumentChatRequest.ChatMessage> getConversationHistory(String sessionId) {
    return conversationHistory.getOrDefault(sessionId, new ArrayList<>());
  }
}
