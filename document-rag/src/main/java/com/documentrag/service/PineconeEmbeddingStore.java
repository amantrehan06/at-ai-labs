package com.documentrag.service;

import com.common.service.SecretManagerService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;

@Slf4j
@Service
public class PineconeEmbeddingStore implements EmbeddingStore<TextSegment> {

    // Pinecone Request DTOs
    @lombok.Data
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class PineconeQueryRequest {
        private List<Float> vector;
        private int topK;
        private boolean includeMetadata;
        private boolean includeValues;
        private Map<String, Object> filter; // Use Map instead of string
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class PineconeUpsertRequest {
        private List<PineconeVector> vectors;
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class PineconeVector {
        private String id;
        private List<Float> values;
        private Map<String, Object> metadata;
    }

    private final String apiKey;
    private final String environment;
    private final String projectId;
    private final String indexName;
    private final okhttp3.OkHttpClient client;
    private final ObjectMapper objectMapper;

    @Autowired
    public PineconeEmbeddingStore(SecretManagerService secretManagerService) {
        this.apiKey = secretManagerService.getPineconeApiKey();

        // Hardcoded Pinecone configuration values
        this.environment = "aped-4627-b74a";
        this.projectId = "9dn22sq";
        this.indexName = "at-ai-lab-index-openai-3-small";

        this.client = new okhttp3.OkHttpClient();
        this.objectMapper = new ObjectMapper();

        log.info("Pinecone Embedding Store initialized - Environment: {}, Project: {}, Index: {}", 
                environment, projectId, indexName);
    }

    @Override
    public String add(Embedding embedding) {
        String id = "embedding-" + System.currentTimeMillis();
        addToPinecone(id, embedding, null);
        return id;
    }

    @Override
    public String add(Embedding embedding, TextSegment textSegment) {
        String id = textSegment.metadata().get("documentId")+"##" +textSegment.metadata().get("sessionId")+"##"+ UUID.randomUUID();
        addToPinecone(id, embedding, textSegment);
        return id;
    }

    @Override
    public void add(String id, Embedding embedding) {
        addToPinecone(id, embedding, null);
    }

    @Override
    public List<String> addAll(List<Embedding> embeddings) {
        List<String> ids = new ArrayList<>();
        for (Embedding embedding : embeddings) {
            String id = "embedding-" + System.currentTimeMillis();
            addToPinecone(id, embedding, null);
            ids.add(id);
        }
        return ids;
    }

    @Override
    public List<String> addAll(List<Embedding> embeddings, List<TextSegment> textSegments) {
        List<String> ids = new ArrayList<>();
        for (int i = 0; i < embeddings.size(); i++) {
            Embedding embedding = embeddings.get(i);
            TextSegment textSegment = i < textSegments.size() ? textSegments.get(i) : null;
            String id = textSegment != null ? 
                textSegment.metadata().get("documentId")+"##" +textSegment.metadata().get("sessionId") :
                "embedding-" + System.currentTimeMillis();
            addToPinecone(id, embedding, textSegment);
            ids.add(id);
        }
        return ids;
    }

    @Override
    public List<dev.langchain4j.store.embedding.EmbeddingMatch<TextSegment>> findRelevant(Embedding queryEmbedding, int maxResults) {
        return findRelevantInPinecone(queryEmbedding, maxResults, 0.0, null);
    }

    @Override
    public List<dev.langchain4j.store.embedding.EmbeddingMatch<TextSegment>> findRelevant(Embedding queryEmbedding, int maxResults, double minScore) {
        return findRelevantInPinecone(queryEmbedding, maxResults, minScore, null);
    }

    // Primary method: Find relevant with metadata filtering
    public List<dev.langchain4j.store.embedding.EmbeddingMatch<TextSegment>> findRelevant(
            Embedding queryEmbedding, int maxResults, double minScore, java.util.Map<String, String> metadataFilter) {
        return findRelevantInPinecone(queryEmbedding, maxResults, minScore, metadataFilter);
    }

    // Pinecone-specific methods
    private void addToPinecone(String ids, Embedding embedding, TextSegment textSegment) {
        try {
            // Convert embedding to Pinecone format
            List<Float> vector = embedding.vectorAsList();

            // Build metadata map for the vector
            Map<String, Object> metadataMap = new HashMap<>();
            if (textSegment != null) {
                metadataMap.put("text", textSegment.text());
                // Include all metadata from TextSegment for hybrid search
                metadataMap.putAll(textSegment.metadata().asMap());
            }

            // Create Pinecone vector using DTO
            PineconeVector pineconeVector = new PineconeVector(ids, vector, metadataMap);
            PineconeUpsertRequest upsertRequest = new PineconeUpsertRequest(List.of(pineconeVector));

            String json = objectMapper.writeValueAsString(upsertRequest);

            // Correct Pinecone URL format: https://{index-name}-{project-id}.svc.{environment}.pinecone.io
            String url = String.format("https://%s-%s.svc.%s.pinecone.io/vectors/upsert",
                    indexName, projectId, environment);

            okhttp3.RequestBody body = okhttp3.RequestBody.create(json, okhttp3.MediaType.get("application/json"));
            okhttp3.Request request = new okhttp3.Request.Builder()
                    .url(url)
                    .addHeader("Api-Key", apiKey)
                    .addHeader("Content-Type", "application/json")
                    .post(body)
                    .build();

            try (okhttp3.Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                    log.error("Pinecone API error: {} - {}", response.code(), errorBody);
                    throw new IOException("Failed to add to Pinecone: " + response.code() + " - " + errorBody);
                }

                log.info("Successfully added embedding to Pinecone with ID: {} and metadata: {}", 
                        ids, textSegment != null ? textSegment.metadata().asMap().toString() : "none");
            }

        } catch (Exception e) {
            log.error("Error adding to Pinecone: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to add embedding to Pinecone", e);
        }
    }

    private List<dev.langchain4j.store.embedding.EmbeddingMatch<TextSegment>> findRelevantInPinecone(
            Embedding queryEmbedding, int maxResults, double minScore, java.util.Map<String, String> metadataFilter) {
        try {
            // Convert query embedding to Pinecone format
            List<Float> queryVector = queryEmbedding.vectorAsList();

            // Build Pinecone query request with metadata filtering using DTO
            PineconeQueryRequest queryRequest;
            if (metadataFilter != null && !metadataFilter.isEmpty()) {
                // Convert metadataFilter to Map<String, Object> for Pinecone
                Map<String, Object> filterMap = new HashMap<>();
                for (java.util.Map.Entry<String, String> entry : metadataFilter.entrySet()) {
                    filterMap.put(entry.getKey(), Map.of("$eq", entry.getValue()));
                }
                
                queryRequest = new PineconeQueryRequest(queryVector, maxResults, true, false, filterMap);
                log.debug("Using metadata filter: {}", filterMap.toString());
            } else {
                // No metadata filtering
                queryRequest = new PineconeQueryRequest(queryVector, maxResults, true, false, null);
            }

            String json = objectMapper.writeValueAsString(queryRequest);

            // Correct Pinecone URL format: https://{index-name}-{project-id}.svc.{environment}.pinecone.io
            String url = String.format("https://%s-%s.svc.%s.pinecone.io/query",
                    indexName, projectId, environment);

            okhttp3.RequestBody body = okhttp3.RequestBody.create(json, okhttp3.MediaType.get("application/json"));
            okhttp3.Request request = new okhttp3.Request.Builder()
                    .url(url)
                    .addHeader("Api-Key", apiKey)
                    .addHeader("Content-Type", "application/json")
                    .post(body)
                    .build();

            try (okhttp3.Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                    log.error("Pinecone query API error: {} - {}", response.code(), errorBody);
                    throw new IOException("Failed to query Pinecone: " + response.code() + " - " + errorBody);
                }

                String responseBody = response.body().string();
                JsonNode root = objectMapper.readTree(responseBody);
                JsonNode matches = root.get("matches");

                List<dev.langchain4j.store.embedding.EmbeddingMatch<TextSegment>> results = new ArrayList<>();

                for (JsonNode match : matches) {
                    double score = match.get("score").asDouble();
                    if (score >= minScore) {
                        String matchId = match.get("id").asText();
                        JsonNode metadata = match.get("metadata");

                        TextSegment textSegment = null;
                        if (metadata != null && metadata.has("text")) {
                            // Reconstruct TextSegment with all original metadata
                            dev.langchain4j.data.document.Metadata reconstructedMetadata = new dev.langchain4j.data.document.Metadata();
                            java.util.Iterator<java.util.Map.Entry<String, JsonNode>> fields = metadata.fields();
                            while (fields.hasNext()) {
                                java.util.Map.Entry<String, JsonNode> entry = fields.next();
                                reconstructedMetadata.add(entry.getKey(), entry.getValue().asText());
                            }
                            textSegment = TextSegment.from(metadata.get("text").asText(), reconstructedMetadata);
                        }

                        // Create a mock embedding for the result (since we don't store the actual vectors)
                        List<Float> mockVector = new ArrayList<>();
                        for (int i = 0; i < queryVector.size(); i++) {
                            mockVector.add(0.0f);
                        }
                        Embedding mockEmbedding = Embedding.from(mockVector);

                        results.add(new dev.langchain4j.store.embedding.EmbeddingMatch<TextSegment>(score, matchId, mockEmbedding, textSegment));
                    }
                }

                log.info("Found {} relevant embeddings in Pinecone with score >= {} and metadata filter: {}", 
                        results.size(), minScore, metadataFilter != null ? metadataFilter.toString() : "none");
                return results;
            }

        } catch (Exception e) {
            log.error("Error querying Pinecone: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to query Pinecone", e);
        }
    }
} 