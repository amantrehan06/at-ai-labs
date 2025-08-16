package com.documentrag.controller;

import com.common.service.SecretManagerService;
import com.documentrag.service.PineconeEmbeddingStore;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/test")
public class TestController {
    
    @Autowired
    private SecretManagerService secretManagerService;
    
    @Autowired
    private PineconeEmbeddingStore pineconeEmbeddingStore;
    
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("message", "Test controller is working");
        response.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/test-openai-embedding")
    public ResponseEntity<Map<String, Object>> testOpenAIEmbedding(@RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String text = request.get("text");
            if (text == null || text.trim().isEmpty()) {
                text = "Hello, this is a test message for OpenAI embeddings.";
            }
            
            log.info("Testing OpenAI embedding for text: {}", text.substring(0, Math.min(text.length(), 50)));
            
            // Test OpenAI embedding API directly
            String openaiApiKey = secretManagerService.getOpenAIApiKey();
            String embeddingResponse = callOpenAIEmbeddingAPI(openaiApiKey, text);
            
            response.put("success", true);
            response.put("message", "OpenAI embedding test successful");
            response.put("input_text", text);
            response.put("embedding_response", embeddingResponse);
            response.put("timestamp", System.currentTimeMillis());
            
            log.info("OpenAI embedding test successful");
            
        } catch (Exception e) {
            log.error("OpenAI embedding test failed: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "OpenAI embedding test failed: " + e.getMessage());
            response.put("timestamp", System.currentTimeMillis());
        }
        
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/test-pinecone")
    public ResponseEntity<Map<String, Object>> testPinecone(@RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String text = request.get("text");
            if (text == null || text.trim().isEmpty()) {
                text = "Hello, this is a test message for Pinecone.";
            }
            
            log.info("Testing Pinecone API for text: {}", text.substring(0, Math.min(text.length(), 50)));
            
            // Step 1: Get OpenAI embedding for the text
            String openaiApiKey = secretManagerService.getOpenAIApiKey();
            String embeddingResponse = callOpenAIEmbeddingAPI(openaiApiKey, text);
            
            // Step 2: Parse the OpenAI response to extract the embedding vector
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.JsonNode root = mapper.readTree(embeddingResponse);
            com.fasterxml.jackson.databind.JsonNode data = root.get("data").get(0);
            com.fasterxml.jackson.databind.JsonNode embedding = data.get("embedding");
            
            // Convert JSON array to List<Float>
            List<Float> vector = new ArrayList<>();
            for (com.fasterxml.jackson.databind.JsonNode val : embedding) {
                vector.add((float) val.asDouble());
            }
            
            log.info("Generated OpenAI embedding with {} dimensions", vector.size());
            
            // Step 3: Create TextSegment and Embedding objects
            TextSegment textSegment = TextSegment.from(text);
            Embedding realEmbedding = Embedding.from(vector);
            
            // Step 4: Store in Pinecone
            String id = pineconeEmbeddingStore.add(realEmbedding, textSegment);
            
            response.put("success", true);
            response.put("message", "Pinecone test successful");
            response.put("input_text", text);
            response.put("embedding_id", id);
            response.put("embedding_dimensions", vector.size());
            response.put("timestamp", System.currentTimeMillis());
            
            log.info("Pinecone test successful, added embedding with ID: {} and {} dimensions", id, vector.size());
            
        } catch (Exception e) {
            log.error("Pinecone test failed: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Pinecone test failed: " + e.getMessage());
            response.put("timestamp", System.currentTimeMillis());
        }
        
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/test-full-pipeline")
    public ResponseEntity<Map<String, Object>> testFullPipeline(@RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String text = request.get("text");
            if (text == null || text.trim().isEmpty()) {
                text = "Hello, this is a test message for the full pipeline.";
            }
            
            log.info("Testing full pipeline for text: {}", text.substring(0, Math.min(text.length(), 50)));
            
            // Step 1: Test OpenAI embedding
            String openaiApiKey = secretManagerService.getOpenAIApiKey();
            String embeddingResponse = callOpenAIEmbeddingAPI(openaiApiKey, text);
            
            // Step 2: Test Pinecone storage
            TextSegment textSegment = TextSegment.from(text);
            
            // Parse the OpenAI response to extract the embedding vector
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.JsonNode root = mapper.readTree(embeddingResponse);
            com.fasterxml.jackson.databind.JsonNode data = root.get("data").get(0);
            com.fasterxml.jackson.databind.JsonNode embedding = data.get("embedding");
            
            // Convert JSON array to List<Float>
            List<Float> vector = new ArrayList<>();
            for (com.fasterxml.jackson.databind.JsonNode val : embedding) {
                vector.add((float) val.asDouble());
            }
            
            log.info("Generated OpenAI embedding with {} dimensions", vector.size());
            
            Embedding realEmbedding = Embedding.from(vector);
            
            String id = pineconeEmbeddingStore.add(realEmbedding, textSegment);
            
            // Step 3: Test Pinecone retrieval
            var results = pineconeEmbeddingStore.findRelevant(realEmbedding, 5);
            
            response.put("success", true);
            response.put("message", "Full pipeline test successful");
            response.put("input_text", text);
            response.put("openai_embedding", embeddingResponse);
            response.put("pinecone_storage_id", id);
            response.put("pinecone_retrieval_count", results.size());
            response.put("timestamp", System.currentTimeMillis());
            
            log.info("Full pipeline test successful");
            
        } catch (Exception e) {
            log.error("Full pipeline test failed: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Full pipeline test failed: " + e.getMessage());
            response.put("timestamp", System.currentTimeMillis());
        }
        
        return ResponseEntity.ok(response);
    }
    
    private String callOpenAIEmbeddingAPI(String apiKey, String text) throws Exception {
        okhttp3.OkHttpClient client = new okhttp3.OkHttpClient();
        
        String json = String.format("""
            {
                "input": "%s",
                "model": "text-embedding-3-small"
            }
            """, text.replace("\"", "\\\""));
        
        okhttp3.RequestBody body = okhttp3.RequestBody.create(json, okhttp3.MediaType.get("application/json"));
        okhttp3.Request request = new okhttp3.Request.Builder()
                .url("https://api.openai.com/v1/embeddings")
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build();
        
        try (okhttp3.Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                throw new RuntimeException("OpenAI API error: " + response.code() + " - " + errorBody);
            }
            
            String responseBody = response.body().string();
            log.info("OpenAI embedding API response: {}", responseBody.substring(0, Math.min(responseBody.length(), 200)));
            return responseBody;
        }
    }
} 