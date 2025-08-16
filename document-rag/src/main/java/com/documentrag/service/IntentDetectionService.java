package com.documentrag.service;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.store.embedding.EmbeddingStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class IntentDetectionService {
    
    @Autowired
    private OpenAIEmbeddingModel embeddingModel;
    
    @Autowired
    private PineconeEmbeddingStore embeddingStore;
    
    // Intent definitions with representative examples
    private static final Map<SearchIntent, List<String>> INTENT_EXAMPLES = Map.of(
        SearchIntent.METHODS, Arrays.asList(
            "methods", "functions", "function list", "show methods", "list methods", 
            "what methods", "method names", "all methods", "get methods",
            "methods in class", "available methods", "public methods"
        ),
        SearchIntent.CLASSES, Arrays.asList(
            "classes", "class names", "show classes", "list classes", 
            "what classes", "all classes", "get classes", "available classes",
            "class structure", "class definition", "class hierarchy"
        ),
        SearchIntent.FIELDS, Arrays.asList(
            "fields", "variables", "show fields", "list fields", 
            "what fields", "all fields", "get fields", "available fields",
            "field names", "field types", "instance variables", "class variables"
        ),
        SearchIntent.CONSTRUCTORS, Arrays.asList(
            "constructors", "show constructors", "list constructors", 
            "what constructors", "all constructors", "get constructors",
            "constructor parameters", "constructor names", "initialization"
        ),
        SearchIntent.PACKAGES, Arrays.asList(
            "packages", "package names", "show packages", "list packages", 
            "what packages", "all packages", "get packages", "package structure",
            "package organization", "namespace"
        ),
        SearchIntent.IMPORTS, Arrays.asList(
            "imports", "import statements", "show imports", "list imports", 
            "what imports", "all imports", "get imports", "imported classes",
            "dependencies", "external classes"
        ),
        SearchIntent.GENERAL, Arrays.asList(
            "explain", "describe", "what is", "how does", "tell me about",
            "overview", "summary", "information", "details", "help"
        )
    );
    
    @PostConstruct
    public void initializeIntentEmbeddings() {
        try {
            log.info("Initializing intent embeddings and storing in Pinecone...");
            
            for (SearchIntent intent : SearchIntent.values()) {
                List<String> examples = INTENT_EXAMPLES.get(intent);
                if (examples != null && !examples.isEmpty()) {
                    // Create a representative text from all examples
                    String representativeText = String.join(" ", examples);
                    
                    // Generate embedding for this intent
                    Embedding embedding = embeddingModel.embed(representativeText).content();
                    
                    // Create a special TextSegment for intent with metadata
                    Map<String, String> intentMetadataMap = new HashMap<>();
                    intentMetadataMap.put("type", "intent");
                    intentMetadataMap.put("intent", intent.name());
                    intentMetadataMap.put("pineconeFilter", intent.getPineconeFilter() != null ? intent.getPineconeFilter() : "general");
                    intentMetadataMap.put("isIntentEmbedding", "true");
                    
                    Metadata intentMetadata = Metadata.from(intentMetadataMap);
                    TextSegment intentSegment = TextSegment.from(representativeText, intentMetadata);
                    
                    // Store intent embedding in Pinecone
                    embeddingStore.add(embedding, intentSegment);
                    
                    log.info("Stored intent embedding in Pinecone: {} ({} examples)", intent, examples.size());
                }
            }
            
            log.info("Intent embeddings initialization completed. Stored {} intents in Pinecone.", SearchIntent.values().length);
            
        } catch (Exception e) {
            log.error("Error initializing intent embeddings: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Detect search intent using Pinecone search (single embedding call)
     * This method now returns both the detected intent and confidence
     */
    public IntentDetectionResult detectSearchIntent(String userMessage) {
        try {
            // Single embedding call for the user message
            Embedding userEmbedding = embeddingModel.embed(userMessage).content();
            
            // Search for intent embeddings in Pinecone
            List<dev.langchain4j.store.embedding.EmbeddingMatch<TextSegment>> intentMatches = embeddingStore.findRelevant(
                userEmbedding,
                5, // topK for intents
                0.3, // minScore threshold for intent detection
                Map.of("type", "intent") // Only search intent embeddings
            );
            
            if (intentMatches.isEmpty()) {
                log.info("No intent matches found, using GENERAL intent");
                return new IntentDetectionResult(SearchIntent.GENERAL, 0.0);
            }
            
            // Get the best intent match
            dev.langchain4j.store.embedding.EmbeddingMatch<TextSegment> bestMatch = intentMatches.get(0);
            String detectedIntentName = bestMatch.embedded().metadata().get("intent");
            double confidence = bestMatch.score();
            
            SearchIntent detectedIntent = SearchIntent.valueOf(detectedIntentName);
            
            log.info("Intent detection completed - Query: '{}', Detected: {} (confidence: {})", 
                userMessage.substring(0, Math.min(userMessage.length(), 50)), detectedIntent, confidence);
            
            // Log top intent matches for debugging
            intentMatches.stream()
                .limit(3)
                .forEach(match -> {
                    String intentName = match.embedded().metadata().get("intent");
                    log.info("Intent match: {} = {}", intentName, match.score());
                });
            
            return new IntentDetectionResult(detectedIntent, confidence);
            
        } catch (Exception e) {
            log.error("Error detecting search intent: {}", e.getMessage(), e);
            return new IntentDetectionResult(SearchIntent.GENERAL, 0.0);
        }
    }
    
    /**
     * Get top N intents with confidence scores using Pinecone
     */
    public List<IntentScore> getTopIntents(String userMessage, int topN) {
        try {
            // Single embedding call
            Embedding userEmbedding = embeddingModel.embed(userMessage).content();
            
            // Search for intent embeddings in Pinecone
            List<dev.langchain4j.store.embedding.EmbeddingMatch<TextSegment>> intentMatches = embeddingStore.findRelevant(
                userEmbedding,
                topN,
                0.0, // Lower threshold to get more results
                Map.of("type", "intent")
            );
            
            return intentMatches.stream()
                .map(match -> {
                    String intentName = match.embedded().metadata().get("intent");
                    SearchIntent intent = SearchIntent.valueOf(intentName);
                    return new IntentScore(intent, match.score());
                })
                .sorted(Comparator.comparing(IntentScore::getConfidence).reversed())
                .toList();
            
        } catch (Exception e) {
            log.error("Error getting top intents: {}", e.getMessage(), e);
            return List.of(new IntentScore(SearchIntent.GENERAL, 0.0));
        }
    }
    
    /**
     * Check if intent embeddings are properly initialized
     */
    public boolean isInitialized() {
        try {
            // Check if we have intent embeddings in Pinecone
            Embedding dummyEmbedding = embeddingModel.embed("test").content();
            List<dev.langchain4j.store.embedding.EmbeddingMatch<TextSegment>> matches = embeddingStore.findRelevant(
                dummyEmbedding, 1, 0.0, Map.of("type", "intent")
            );
            return !matches.isEmpty();
        } catch (Exception e) {
            log.warn("Could not verify intent embeddings in Pinecone: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Get intent examples for debugging
     */
    public Map<SearchIntent, List<String>> getIntentExamples() {
        return new HashMap<>(INTENT_EXAMPLES);
    }
    
    /**
     * Result class for intent detection
     */
    public static class IntentDetectionResult {
        private final SearchIntent intent;
        private final double confidence;
        
        public IntentDetectionResult(SearchIntent intent, double confidence) {
            this.intent = intent;
            this.confidence = confidence;
        }
        
        public SearchIntent getIntent() { return intent; }
        public double getConfidence() { return confidence; }
        
        @Override
        public String toString() {
            return String.format("%s (%.4f)", intent, confidence);
        }
    }
    
    /**
     * Inner class to represent intent with confidence score
     */
    public static class IntentScore {
        private final SearchIntent intent;
        private final double confidence;
        
        public IntentScore(SearchIntent intent, double confidence) {
            this.intent = intent;
            this.confidence = confidence;
        }
        
        public SearchIntent getIntent() { return intent; }
        public double getConfidence() { return confidence; }
        
        @Override
        public String toString() {
            return String.format("%s (%.4f)", intent, confidence);
        }
    }
    
    /**
     * Enum to represent different search intents for targeted filtering
     */
    public enum SearchIntent {
        METHODS("method"),
        CLASSES("class"),
        FIELDS("field"),
        CONSTRUCTORS("constructor"),
        PACKAGES("package"),
        IMPORTS("imports"),
        GENERAL(null);
        
        private final String pineconeFilter;
        
        SearchIntent(String pineconeFilter) {
            this.pineconeFilter = pineconeFilter;
        }
        
        public String getPineconeFilter() {
            return pineconeFilter;
        }
    }
} 