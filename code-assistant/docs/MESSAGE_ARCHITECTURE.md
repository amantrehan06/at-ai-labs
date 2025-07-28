# Message Architecture with Strategy Pattern

## Overview

This document explains the simplified message architecture that combines `SystemMessage` and `UserMessage` classes with the existing Strategy pattern to provide a clean, straightforward approach to AI interactions.

## Architecture Components

### 1. SystemMessage Class

The `SystemMessage` class defines the overall behavior and instructions for the AI. It contains:

- **Role**: Always set to "system"
- **Content**: AI behavior instructions, role-specific content, language context, and response structure
- **AnalysisType**: The type of analysis being performed
- **Language**: The programming language being analyzed
- **Context**: Language-specific context information

```java
SystemMessage systemMessage = SystemMessage.createAnalysisMessage(
    AnalysisType.EXPLAIN, 
    "java"
);
```

### 2. UserMessage Class

The `UserMessage` class contains the user's specific request or code to be analyzed:

- **Role**: Always set to "user"
- **Content**: Formatted user request with code
- **Code**: The actual code to be analyzed
- **Language**: Programming language of the code
- **AnalysisType**: Type of analysis requested

```java
UserMessage userMessage = UserMessage.createAnalysisMessage(
    code,
    AnalysisType.EXPLAIN,
    "java"
);
```

### 3. MessagePair Class

The `MessagePair` class provides a clean container for both system and user messages:

```java
@Getter
@AllArgsConstructor
public class MessagePair {
    private final SystemMessage systemMessage;
    private final UserMessage userMessage;
    
    // Convenience methods for accessing content
    public String getSystemContent() { return systemMessage.getContent(); }
    public String getUserContent() { return userMessage.getContent(); }
    public AnalysisType getAnalysisType() { return systemMessage.getAnalysisType(); }
    public String getLanguage() { return systemMessage.getLanguage(); }
}
```

### 4. Strategy Pattern Integration

The Strategy pattern is simplified and enhanced to work with the new message structure:

#### AnalysisStrategy Interface

```java
public interface AnalysisStrategy {
    AnalysisType getAnalysisType();
    MessagePair buildMessages(AnalysisRequest request);
    String getDescription();
}
```

#### Simplified Strategy Implementations

Each strategy directly creates messages without complex dependencies:

```java
@Component
public class ExplainAnalysisStrategy implements AnalysisStrategy {
    
    @Override
    public MessagePair buildMessages(AnalysisRequest request) {
        // Create system message directly
        SystemMessage systemMessage = SystemMessage.createAnalysisMessage(
            request.getAnalysisType(), 
            request.getLanguage()
        );
        
        // Create user message directly
        UserMessage userMessage = UserMessage.createAnalysisMessage(
            request.getCode(),
            request.getAnalysisType(),
            request.getLanguage()
        );
        
        return new MessagePair(systemMessage, userMessage);
    }
}
```

## Benefits of This Simplified Architecture

### 1. **Simplicity**
- Direct message creation without complex factory dependencies
- Clear, readable code with minimal method calls
- Easy to understand and maintain

### 2. **Separation of Concerns**
- SystemMessage: Defines AI behavior and instructions
- UserMessage: Contains user input and code
- MessagePair: Clean container for message pairs
- Strategy: Defines analysis type and behavior

### 3. **Consistency**
- All AI interactions use the same message structure
- Language-specific context is automatically included
- Response structures are standardized

### 4. **Maintainability**
- Easy to modify AI behavior by updating SystemMessage
- Easy to add new analysis types by creating new strategies
- No complex dependency injection chains
- Lombok annotations reduce boilerplate code

## Usage Examples

### Simple Strategy Usage

```java
// Create analysis request
AnalysisRequest request = new AnalysisRequest();
request.setCode("public class HelloWorld { ... }");
request.setAnalysisType(AnalysisType.EXPLAIN);
request.setLanguage("java");

// Use strategy to build messages (simple one-line call)
AnalysisStrategy strategy = strategies.get(AnalysisType.EXPLAIN);
MessagePair messages = strategy.buildMessages(request);

// Use with AI service
List<ChatMessage> chatMessages = List.of(
    new SystemMessage(messages.getSystemContent()),
    new UserMessage(messages.getUserContent())
);
```

### Comparing Different Strategies

```java
// Same code, different strategies
String code = "def factorial(n): return 1 if n <= 1 else n * factorial(n-1)";

AnalysisRequest request = new AnalysisRequest();
request.setCode(code);
request.setLanguage("python");

// EXPLAIN strategy
request.setAnalysisType(AnalysisType.EXPLAIN);
MessagePair explainMessages = explainStrategy.buildMessages(request);

// DEBUG strategy
request.setAnalysisType(AnalysisType.DEBUG);
MessagePair debugMessages = debugStrategy.buildMessages(request);
```

## Analysis Types and Their System Messages

### 1. EXPLAIN
- **Purpose**: Explain what code does and how it works
- **System Message**: Focuses on clarity and educational value
- **Response Structure**: Overview, Functionality, Key Concepts, Code Flow

### 2. DEBUG
- **Purpose**: Identify potential issues and suggest fixes
- **System Message**: Focuses on problem identification and solutions
- **Response Structure**: Issues Found, Root Causes, Solutions, Prevention

### 3. REFACTOR
- **Purpose**: Suggest improvements for code quality
- **System Message**: Focuses on clean code principles and best practices
- **Response Structure**: Current Issues, Suggested Improvements, Benefits, Implementation

### 4. ANALYZE (Comprehensive)
- **Purpose**: Provide comprehensive code review
- **System Message**: Focuses on multiple perspectives (functionality, performance, security)
- **Response Structure**: Code Overview, Strengths, Areas for Improvement, Security, Performance, Best Practices

## Language Support

The architecture automatically includes language-specific context for:

- **Java**: Java-specific patterns, conventions, and best practices
- **Python**: Python-specific patterns, PEP guidelines, and best practices
- **JavaScript**: JavaScript-specific patterns, ES6+ features, and best practices
- **C++**: C++-specific patterns, memory management, and best practices

## Migration from Old Architecture

The new architecture provides a clean, modern approach:

1. **Strategies** use the `buildMessages()` method for structured message creation
2. **AI services** automatically handle the new message structure
3. **No complex dependencies** - strategies work independently
4. **Lombok annotations** reduce boilerplate code

## Simplified Implementation Pattern

### Creating a New Strategy

```java
@Component
public class CustomAnalysisStrategy implements AnalysisStrategy {
    
    @Override
    public AnalysisType getAnalysisType() {
        return AnalysisType.CUSTOM;
    }
    
    @Override
    public MessagePair buildMessages(AnalysisRequest request) {
        // Direct message creation - no complex dependencies
        SystemMessage systemMessage = SystemMessage.createAnalysisMessage(
            request.getAnalysisType(), 
            request.getLanguage()
        );
        
        UserMessage userMessage = UserMessage.createAnalysisMessage(
            request.getCode(),
            request.getAnalysisType(),
            request.getLanguage()
        );
        
        return new MessagePair(systemMessage, userMessage);
    }
    
    @Override
    public String getDescription() {
        return "Custom analysis description";
    }
}
```

## Testing

The architecture includes comprehensive testing:

- Unit tests for each component
- Integration tests for complete workflows
- Strategy pattern testing
- Message creation testing

## Conclusion

This simplified architecture provides a clean, maintainable, and extensible approach to AI interactions while preserving the existing Strategy pattern. It eliminates unnecessary complexity while maintaining all the benefits of proper message separation and strategy-based analysis. The use of Lombok annotations further reduces boilerplate code and improves readability. 