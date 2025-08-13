package com.documentrag.config;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class DocumentRagConfig {
    
    @Bean
    public EmbeddingModel embeddingModel() {
        // Create a simple mock embedding model for testing
        // TODO: Upgrade to AllMiniLmL6V2EmbeddingModel when dependency issues are resolved
        return new EmbeddingModel() {
            @Override
            public Response<Embedding> embed(TextSegment textSegment) {
                // Return a simple mock embedding (384-dimensional vector of 0.1)
                float[] values = new float[384];
                for (int i = 0; i < values.length; i++) {
                    values[i] = 0.1f;
                }
                return Response.from(Embedding.from(values));
            }
            
            @Override
            public Response<Embedding> embed(String text) {
                // Return a simple mock embedding (384-dimensional vector of 0.1)
                float[] values = new float[384];
                for (int i = 0; i < values.length; i++) {
                    values[i] = 0.1f;
                }
                return Response.from(Embedding.from(values));
            }
            
            @Override
            public Response<List<Embedding>> embedAll(List<TextSegment> textSegments) {
                // Return mock embeddings for all segments
                List<Embedding> embeddings = textSegments.stream()
                    .map(segment -> {
                        float[] values = new float[384];
                        for (int i = 0; i < values.length; i++) {
                            values[i] = 0.1f;
                        }
                        return Embedding.from(values);
                    })
                    .toList();
                return Response.from(embeddings);
            }
        };
    }
} 