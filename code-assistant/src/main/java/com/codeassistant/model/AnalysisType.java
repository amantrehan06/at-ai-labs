package com.codeassistant.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Enum representing different types of code analysis
 */
@Getter
@RequiredArgsConstructor
public enum AnalysisType {
    WRITE_CODE("Generate code based on user requirements and specifications"),
    REFACTOR("Provide refactoring suggestions and improvements"),
    DEBUG("Analyze potential bugs and debugging tips"),
    ANALYZE("Comprehensive analysis including explanation, refactoring, and debugging"),
    FOLLOWUP("Handle follow-up questions in an ongoing conversation");

    private final String description;
} 