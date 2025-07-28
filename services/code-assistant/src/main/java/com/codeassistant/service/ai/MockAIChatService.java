package com.codeassistant.service.ai;

import com.codeassistant.model.AnalysisRequest;
import com.codeassistant.model.AnalysisResponse;
import com.codeassistant.service.strategy.AnalysisStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Mock implementation of AIChatService for testing purposes.
 * This service provides predictable responses for unit testing.
 */
@Service
@Profile("test")
public class MockAIChatService implements AIChatService {
    
    private final Map<com.codeassistant.model.AnalysisType, AnalysisStrategy> analysisStrategies;
    
    @Autowired
    public MockAIChatService(List<AnalysisStrategy> strategies) {
        this.analysisStrategies = strategies.stream()
            .collect(Collectors.toMap(AnalysisStrategy::getAnalysisType, strategy -> strategy));
    }
    
    @Override
    public AnalysisResponse analyzeCode(AnalysisRequest request) throws AIServiceException {
        String mockAnalysis = buildMockAnalysis(request);
        
        return AnalysisResponse.builder()
            .analysis(mockAnalysis)
            .analysisType(request.getAnalysisType())
            .language(request.getLanguage())
            .success(true)
            .build();
    }
    
    @Override
    public boolean isAvailable() {
        return true; // Mock service is always available for testing
    }
    
    private String buildMockAnalysis(AnalysisRequest request) {
        StringBuilder analysis = new StringBuilder();
        analysis.append("=== MOCK ANALYSIS ===\n\n");
        analysis.append("Analysis Type: ").append(request.getAnalysisType()).append("\n");
        analysis.append("Language: ").append(request.getLanguage()).append("\n");
        analysis.append("Code Length: ").append(request.getCode().length()).append(" characters\n\n");
        
        // Get the strategy description
        AnalysisStrategy strategy = analysisStrategies.get(request.getAnalysisType());
        if (strategy != null) {
            analysis.append("Strategy Description: ").append(strategy.getDescription()).append("\n\n");
        }
        
        switch (request.getAnalysisType()) {
            case EXPLAIN:
                analysis.append("This is a mock explanation of the provided code.\n");
                analysis.append("The code appears to be a ").append(request.getLanguage()).append(" implementation.\n");
                analysis.append("In a real scenario, this would contain detailed analysis of the code's functionality.");
                break;
            case REFACTOR:
                analysis.append("This is a mock refactoring suggestion.\n");
                analysis.append("Consider improving code structure and readability.\n");
                analysis.append("In a real scenario, this would contain specific refactoring recommendations.");
                break;
            case DEBUG:
                analysis.append("This is a mock debugging analysis.\n");
                analysis.append("Potential issues might be identified here.\n");
                analysis.append("In a real scenario, this would contain specific debugging insights.");
                break;
            case ANALYZE:
                analysis.append("This is a mock comprehensive analysis.\n");
                analysis.append("It would include explanation, refactoring suggestions, and debugging insights.\n");
                analysis.append("In a real scenario, this would be a detailed analysis of the code.");
                break;
        }
        
        return analysis.toString();
    }
} 