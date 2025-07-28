package com.codeassistant.model;

/**
 * Enum representing different types of code analysis
 */
public enum AnalysisType {
    EXPLAIN("Explain the code functionality and logic"),
    REFACTOR("Provide refactoring suggestions and improvements"),
    DEBUG("Analyze potential bugs and debugging tips"),
    ANALYZE("Comprehensive analysis including explanation, refactoring, and debugging");

    private final String description;

    AnalysisType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
} 