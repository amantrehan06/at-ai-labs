package com.codeassistant.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Streaming Analysis Response for Server-Sent Events (SSE)
 * Used to stream AI analysis responses in real-time to improve UI polish.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StreamingAnalysisResponse {
    
    /**
     * Type of streaming event
     */
    private String eventType;
    
    /**
     * The streaming content chunk
     */
    private String content;
    
    /**
     * Analysis type being performed
     */
    private AnalysisType analysisType;
    
    /**
     * Programming language being analyzed
     */
    private String language;
    
    /**
     * Whether the streaming is complete
     */
    private boolean isComplete;
    
    /**
     * Error message if any
     */
    private String error;
    
    /**
     * Success status
     */
    private boolean success;
    
    /**
     * Create a content chunk event
     */
    public static StreamingAnalysisResponse contentChunk(String content, AnalysisType analysisType, String language) {
        return StreamingAnalysisResponse.builder()
            .eventType("content")
            .content(content)
            .analysisType(analysisType)
            .language(language)
            .isComplete(false)
            .success(true)
            .build();
    }
    
    /**
     * Create a completion event
     */
    public static StreamingAnalysisResponse complete(AnalysisType analysisType, String language) {
        return StreamingAnalysisResponse.builder()
            .eventType("complete")
            .analysisType(analysisType)
            .language(language)
            .isComplete(true)
            .success(true)
            .build();
    }
    
    /**
     * Create an error event
     */
    public static StreamingAnalysisResponse error(String errorMessage, AnalysisType analysisType, String language) {
        return StreamingAnalysisResponse.builder()
            .eventType("error")
            .error(errorMessage)
            .analysisType(analysisType)
            .language(language)
            .isComplete(true)
            .success(false)
            .build();
    }
} 