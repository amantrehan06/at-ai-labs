package com.documentrag.service;

import com.documentrag.model.DocumentChatRequest;
import com.documentrag.model.DocumentChatResponse;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class DocumentChatService {
    
    @Autowired
    private PineconeEmbeddingStore embeddingStore;
    
    @Autowired
    private OpenAIEmbeddingModel embeddingModel;
    
    // In-memory conversation history (in production, use Redis or database)
    private final ConcurrentHashMap<String, List<DocumentChatRequest.ChatMessage>> conversationHistory = new ConcurrentHashMap<>();
    
    public DocumentChatResponse chatWithDocuments(DocumentChatRequest request) {
        DocumentChatResponse response = new DocumentChatResponse();
        
        try {
            String userMessage = request.getMessage();
            String sessionId = request.getSessionId();
            String service = request.getService();
            
            log.info("Processing chat request - Session: {}, Service: {}, Message: {}", sessionId, service, userMessage.substring(0, Math.min(userMessage.length(), 50)));
            
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
            List<String> relevantDocs = searchRelevantDocuments(userMessage, sessionId);
            log.info("Found {} relevant documents for query in session {}", relevantDocs.size(), sessionId);
            
            // Generate AI response (simplified for now)
            String aiResponse = generateAIResponse(userMessage, relevantDocs, history);
            
            // Add AI response to history
            DocumentChatRequest.ChatMessage aiMsg = new DocumentChatRequest.ChatMessage();
            aiMsg.setRole("assistant");
            aiMsg.setContent(aiResponse);
            aiMsg.setTimestamp(String.valueOf(System.currentTimeMillis()));
            history.add(aiMsg);
            
            // Update conversation history
            conversationHistory.put(sessionId, history);
            
            // Build response
            response.setSuccess(true);
            response.setMessage("Response generated successfully");
            response.setAnswer(aiResponse);
            response.setResponse(aiResponse); // Set response field for UI compatibility
            response.setSessionId(sessionId);
            response.setRelevantDocuments(relevantDocs);
            response.setConversationHistory(history);
            
            log.info("Chat response generated successfully - Session: {}, Response length: {}", sessionId, aiResponse.length());
            
        } catch (Exception e) {
            log.error("Error processing chat request: {}", e.getMessage(), e);
            response.setSuccess(false);
            response.setMessage("Error processing chat request: " + e.getMessage());
        }
        
        return response;
    }
    
    public void addDocumentToVectorStore(String documentId, List<TextSegment> segments, String documentType) {
        try {
            log.info("Adding document to vector store - ID: {}, Type: {}, Segments: {}", documentId, documentType, segments.size());
            
            // Store pre-split segments in embedding store (metadata is already preserved from Document)
            for (TextSegment segment : segments) {
                Embedding embedding = embeddingModel.embed(segment).content();
                embeddingStore.add(embedding, segment);
            }
            
            log.info("Successfully added document {} to vector store with {} segments", documentId, segments.size());
            
        } catch (Exception e) {
            log.error("Error adding document to vector store: {}", e.getMessage(), e);
        }
    }
    
    private List<String> searchRelevantDocuments(String query, String sessionId) {
        List<String> relevantDocs = new ArrayList<>();
        
        try {
            log.info("Searching for relevant documents for query: {} in session: {}", 
                    query.substring(0, Math.min(query.length(), 100)), sessionId);
            
            // Create embedding for the query
            Embedding queryEmbedding = embeddingModel.embed(query).content();
            
            // Create metadata filter for sessionId
            java.util.Map<String, String> metadataFilter = java.util.Map.of("sessionId", sessionId);
            
            // Use Pinecone's metadata filtering for efficient hybrid search
            List<dev.langchain4j.store.embedding.EmbeddingMatch<TextSegment>> matches = 
                embeddingStore.findRelevant(queryEmbedding, 5, 0.1, metadataFilter);
            
            // Process results (no need for Java-side filtering since Pinecone handles it)
            for (dev.langchain4j.store.embedding.EmbeddingMatch<TextSegment> match : matches) {
                TextSegment segment = match.embedded();
                String segmentText = segment.text();
                
                // Build structured result with metadata (all results are already session-filtered)
                StringBuilder result = new StringBuilder();
                result.append("[Document ID: ").append(segment.metadata().get("documentId")).append("] ");
                result.append("[Session: ").append(segment.metadata().get("sessionId")).append("] ");
                result.append("[Segment ").append(segment.metadata().get("segmentIndex")).append("/")
                      .append(segment.metadata().get("totalSegments")).append("] ");
                result.append(segmentText);
                
                relevantDocs.add(result.toString());
            }
            
            log.info("Found {} relevant documents with score > 0.7 in session {} using Pinecone metadata filtering", 
                    relevantDocs.size(), sessionId);
            
        } catch (Exception e) {
            log.error("Error searching documents: {}", e.getMessage(), e);
        }
        
        return relevantDocs;
    }
    
    public List<String> searchDocumentsByDocumentId(String documentId) {
        List<String> documentSegments = new ArrayList<>();
        
        try {
            log.info("Searching for segments from document ID: {}", documentId);
            
            // Create metadata filter for documentId
            java.util.Map<String, String> metadataFilter = java.util.Map.of("documentId", documentId);
            
            // Use Pinecone's metadata filtering for efficient document-specific search
            // Create a dummy embedding to search (we'll filter by metadata)
            String dummyQuery = "document " + documentId;
            Embedding queryEmbedding = embeddingModel.embed(dummyQuery).content();
            
            // Search for segments using metadata filtering
            List<dev.langchain4j.store.embedding.EmbeddingMatch<TextSegment>> matches = 
                embeddingStore.findRelevant(queryEmbedding, 100, 0.0, metadataFilter);
            
            // Process results (no need for Java-side filtering since Pinecone handles it)
            for (dev.langchain4j.store.embedding.EmbeddingMatch<TextSegment> match : matches) {
                TextSegment segment = match.embedded();
                String segmentText = segment.text();
                
                StringBuilder result = new StringBuilder();
                result.append("[Segment ").append(segment.metadata().get("segmentIndex")).append("/")
                      .append(segment.metadata().get("totalSegments")).append("] ");
                result.append(segmentText);
                
                documentSegments.add(result.toString());
            }
            
            log.info("Found {} segments for document ID: {} using Pinecone metadata filtering", documentSegments.size(), documentId);
            
        } catch (Exception e) {
            log.error("Error searching documents by document ID: {}", e.getMessage(), e);
        }
        
        return documentSegments;
    }
    
    public List<String> searchDocumentsBySessionId(String sessionId) {
        List<String> documentSegments = new ArrayList<>();
        
        try {
            log.info("Searching for segments from session ID: {}", sessionId);
            
            // Create metadata filter for sessionId
            java.util.Map<String, String> metadataFilter = java.util.Map.of("sessionId", sessionId);
            
            // Use Pinecone's metadata filtering for efficient session-specific search
            // Create a dummy embedding to search (we'll filter by metadata)
            String dummyQuery = "session " + sessionId;
            Embedding queryEmbedding = embeddingModel.embed(dummyQuery).content();
            
            // Search for segments using metadata filtering
            List<dev.langchain4j.store.embedding.EmbeddingMatch<TextSegment>> matches = 
                embeddingStore.findRelevant(queryEmbedding, 100, 0.0, metadataFilter);
            
            // Process results (no need for Java-side filtering since Pinecone handles it)
            for (dev.langchain4j.store.embedding.EmbeddingMatch<TextSegment> match : matches) {
                TextSegment segment = match.embedded();
                String segmentText = segment.text();
                
                StringBuilder result = new StringBuilder();
                result.append("[Document ID: ").append(segment.metadata().get("documentId")).append("] ");
                result.append("[Session: ").append(segment.metadata().get("sessionId")).append("] ");
                result.append("[Segment ").append(segment.metadata().get("segmentIndex")).append("/")
                      .append(segment.metadata().get("totalSegments")).append("] ");
                result.append(segmentText);
                
                documentSegments.add(result.toString());
            }
            
            log.info("Found {} segments for session ID: {} using Pinecone metadata filtering", documentSegments.size(), sessionId);
            
        } catch (Exception e) {
            log.error("Error searching documents by session ID: {}", e.getMessage(), e);
        }
        
        return documentSegments;
    }
    
    private String generateAIResponse(String userMessage, List<String> relevantDocs, List<DocumentChatRequest.ChatMessage> history) {
        // Simplified AI response generation
        // In production, this would use a proper LLM like OpenAI GPT-4
        
        StringBuilder response = new StringBuilder();
        
        if (relevantDocs.isEmpty()) {
            response.append("I couldn't find any relevant information in your uploaded documents to answer your question: \"")
                   .append(userMessage)
                   .append("\"\n\nPlease make sure you have uploaded relevant documents or try rephrasing your question.");
        } else {
            response.append("Based on your uploaded documents, here's what I found:\n\n");
            
            for (int i = 0; i < Math.min(relevantDocs.size(), 3); i++) {
                String doc = relevantDocs.get(i);
                response.append("• ").append(doc.substring(0, Math.min(doc.length(), 200)))
                       .append(doc.length() > 200 ? "..." : "")
                       .append("\n\n");
            }
            
            response.append("This information should help answer your question: \"").append(userMessage).append("\"");
        }
        
        return response.toString();
    }
    
    public void clearConversationHistory(String sessionId) {
        conversationHistory.remove(sessionId);
    }
    
    public List<DocumentChatRequest.ChatMessage> getConversationHistory(String sessionId) {
        return conversationHistory.getOrDefault(sessionId, new ArrayList<>());
    }
} 