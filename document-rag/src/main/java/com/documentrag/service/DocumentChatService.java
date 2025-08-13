package com.documentrag.service;

import com.documentrag.model.DocumentChatRequest;
import com.documentrag.model.DocumentChatResponse;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class DocumentChatService {
    
    @Autowired
    private EmbeddingModel embeddingModel;
    
    // In-memory embedding store for now (will upgrade to Chroma later)
    private final EmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();
    
    // In-memory conversation history (in production, use Redis or database)
    private final Map<String, List<DocumentChatRequest.ChatMessage>> conversationHistory = new ConcurrentHashMap<>();
    
    private final DocumentSplitter documentSplitter = DocumentSplitters.recursive(100, 20);
    
    public DocumentChatResponse chatWithDocuments(DocumentChatRequest request) {
        DocumentChatResponse response = new DocumentChatResponse();
        
        try {
            String sessionId = request.getSessionId();
            String userMessage = request.getMessage();
            
            if (userMessage == null || userMessage.trim().isEmpty()) {
                response.setSuccess(false);
                response.setMessage("Please provide a question to ask about your documents.");
                return response;
            }
            
            // Get conversation history for this session
            List<DocumentChatRequest.ChatMessage> history = conversationHistory.getOrDefault(sessionId, new ArrayList<>());
            
            // Add user message to history
            DocumentChatRequest.ChatMessage userMsg = new DocumentChatRequest.ChatMessage();
            userMsg.setRole("user");
            userMsg.setContent(userMessage);
            userMsg.setTimestamp(java.time.LocalDateTime.now().toString());
            history.add(userMsg);
            
            // Search for relevant documents
            List<String> relevantDocs = searchRelevantDocuments(userMessage);
            
            // Generate AI response (simplified for now)
            String aiResponse = generateAIResponse(userMessage, relevantDocs, history);
            
            // Add AI response to history
            DocumentChatRequest.ChatMessage aiMsg = new DocumentChatRequest.ChatMessage();
            aiMsg.setRole("assistant");
            aiMsg.setContent(aiResponse);
            aiMsg.setTimestamp(java.time.LocalDateTime.now().toString());
            history.add(aiMsg);
            
            // Update conversation history
            conversationHistory.put(sessionId, history);
            
            // Build response
            response.setSuccess(true);
            response.setMessage("Response generated successfully");
            response.setAnswer(aiResponse);
            response.setSessionId(sessionId);
            response.setRelevantDocuments(relevantDocs);
            response.setConversationHistory(history);
            
        } catch (Exception e) {
            response.setSuccess(false);
            response.setMessage("Error processing chat request: " + e.getMessage());
            // Log error in document chat
            // System.err.println("Error in document chat: " + e.getMessage());
        }
        
        return response;
    }
    
    public void addDocumentToVectorStore(String documentId, String content, String documentType) {
        try {
            // Create document
            Document document = Document.from(content, dev.langchain4j.data.document.Metadata.from("type", documentType));
            
            // Split document into segments
            List<TextSegment> segments = documentSplitter.split(document);
            
            // Store segments in embedding store
            for (TextSegment segment : segments) {
                Embedding embedding = embeddingModel.embed(segment).content();
                embeddingStore.add(embedding, segment);
            }
            
            // Log successful document addition
            // System.out.println("Added document " + documentId + " to vector store with " + segments.size() + " segments");
            
        } catch (Exception e) {
            // Log error adding document to vector store
            // System.err.println("Error adding document to vector store: " + e.getMessage());
        }
    }
    
    private List<String> searchRelevantDocuments(String query) {
        List<String> relevantDocs = new ArrayList<>();
        
        try {
            // Create embedding for the query
            Embedding queryEmbedding = embeddingModel.embed(query).content();
            
            // Search for similar segments
            List<dev.langchain4j.store.embedding.EmbeddingMatch<TextSegment>> matches = 
                embeddingStore.findRelevant(queryEmbedding, 5);
            
            for (dev.langchain4j.store.embedding.EmbeddingMatch<TextSegment> match : matches) {
                if (match.score() > 0.7) { // Threshold for relevance
                    relevantDocs.add(match.embedded().text());
                }
            }
            
        } catch (Exception e) {
            // Log error searching documents
            // System.err.println("Error searching documents: " + e.getMessage());
        }
        
        return relevantDocs;
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