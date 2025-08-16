package com.documentrag.service;

import com.common.service.SecretManagerService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class OpenAIEmbeddingModel implements EmbeddingModel {

    // Record for OpenAI embedding request
    public record OpenAIEmbeddingRequest(String model, List<String> input) {}

    private static final String OPENAI_EMBEDDINGS_URL = "https://api.openai.com/v1/embeddings";
    private final String apiKey;
    private final String model;
    private final OkHttpClient client;
    private final ObjectMapper objectMapper;

    @Autowired
    public OpenAIEmbeddingModel(SecretManagerService secretManagerService) {
        this.apiKey = secretManagerService.getOpenAIApiKey();
        this.model = "text-embedding-3-small"; // Default to the latest OpenAI embedding model
        this.client = new OkHttpClient();
        this.objectMapper = new ObjectMapper();
        log.info("OpenAI Embedding Model initialized with model: {}", this.model);
    }

    @Override
    public Response<Embedding> embed(TextSegment textSegment) {
        List<TextSegment> segments = List.of(textSegment);
        Response<List<Embedding>> response = embedAll(segments);
        if (response.content().isEmpty()) {
            throw new RuntimeException("Failed to generate embedding for text segment");
        }
        return Response.from(response.content().get(0));
    }

    @Override
    public Response<Embedding> embed(String text) {
        TextSegment segment = TextSegment.from(text);
        return embed(segment);
    }

    @Override
    public Response<List<Embedding>> embedAll(List<TextSegment> segments) {
        List<Embedding> embeddings = new ArrayList<>();

        try {
            log.info("Generating embeddings for {} text segments", segments.size());
            
            // Build JSON request
            List<String> inputs = segments.stream().map(TextSegment::text).collect(Collectors.toList());
            String json = objectMapper.writeValueAsString(
                    new OpenAIEmbeddingRequest(OpenAIEmbeddingModel.this.model, inputs)
            );

            RequestBody body = RequestBody.create(
                    json, MediaType.get("application/json")
            );

            Request request = new Request.Builder()
                    .url(OPENAI_EMBEDDINGS_URL)
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .post(body)
                    .build();

            try (okhttp3.Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                    log.error("OpenAI API error: {} - {}", response.code(), errorBody);
                    throw new IOException("Failed to get embeddings: " + response.code() + " - " + errorBody);
                }

                String responseBody = response.body().string();
                JsonNode root = objectMapper.readTree(responseBody);
                JsonNode data = root.get("data");

                for (JsonNode item : data) {
                    List<Float> vector = new ArrayList<>();
                    for (JsonNode val : item.get("embedding")) {
                        vector.add((float) val.asDouble());
                    }
                    embeddings.add(Embedding.from(vector));
                }
                
                log.info("Successfully generated {} embeddings", embeddings.size());
            }

        } catch (IOException e) {
            log.error("Error generating OpenAI embeddings: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate embeddings", e);
        }

        return Response.from(embeddings);
    }
} 