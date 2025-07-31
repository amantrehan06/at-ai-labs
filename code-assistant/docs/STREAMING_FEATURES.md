# Streaming Features Documentation

## Overview

This document describes the streaming capabilities of the Code Assistant API, which provides real-time streaming responses for code assistance operations.

## Available Streaming Endpoints

| Regular Endpoint | Streaming Endpoint | Description |
|------------------|-------------------|-------------|
| `POST /api/v1/code/assist` | `POST /api/v1/code/assist/stream` | Generic code assistance with streaming |
| `POST /api/v1/code/assist/{service}` | `POST /api/v1/code/assist/{service}/stream` | Service-specific assistance with streaming |

## Request Format

All streaming endpoints accept the same JSON request format:

```json
{
    "code": "Your code or requirements here",
    "language": "java|python|javascript|typescript|cpp|c|csharp|php|ruby|go|rust|swift|kotlin|scala",
    "analysisType": "WRITE_CODE|REFACTOR|DEBUG|ANALYZE",
    "apiKey": "your-api-key-here"
}
```

## Response Format

Streaming responses use Server-Sent Events (SSE) format with the following event types:

### Content Event
```json
{
    "eventType": "content",
    "content": "Generated or analyzed content here..."
}
```

### Complete Event
```json
{
    "eventType": "complete",
    "analysis": "Final complete analysis result"
}
```

### Error Event
```json
{
    "eventType": "error",
    "error": "Error message describing what went wrong"
}
```

## JavaScript Usage Example

```javascript
// Create EventSource for streaming
const eventSource = new EventSource('/api/v1/code/assist/stream');

// Handle incoming content
eventSource.onmessage = function(event) {
    const data = JSON.parse(event.data);
    
    if (data.eventType === 'content') {
        // Append new content to UI
        appendToResult(data.content);
    } else if (data.eventType === 'complete') {
        // Handle completion
        eventSource.close();
        showCompleteResult(data.analysis);
    } else if (data.eventType === 'error') {
        // Handle error
        eventSource.close();
        showError(data.error);
    }
};

// Handle connection errors
eventSource.onerror = function(event) {
    eventSource.close();
    showError('Connection error occurred');
};
```

## cURL Usage Example

```bash
# Test streaming assist for code generation
curl -X POST http://localhost:8080/api/v1/code/assist/stream \
  -H "Content-Type: application/json" \
  -d '{
    "code": "Create a function that calculates factorial",
    "language": "java",
    "analysisType": "WRITE_CODE"
  }' \
  --no-buffer

# Test streaming assist for code refactoring
curl -X POST http://localhost:8080/api/v1/code/assist/stream \
  -H "Content-Type: application/json" \
  -d '{
    "code": "public int factorial(int n) { return n <= 1 ? 1 : n * factorial(n-1); }",
    "language": "java",
    "analysisType": "REFACTOR"
  }' \
  --no-buffer
```

## Benefits of Streaming

### 1. **Real-time Feedback**
- Users see results as they're generated
- No need to wait for complete response
- Better user experience for long operations

### 2. **Progress Indication**
- Content appears incrementally
- Users can see work in progress
- Reduces perceived wait time

### 3. **Resource Efficiency**
- Server can start processing immediately
- Memory usage is more efficient
- Better handling of large responses

### 4. **Error Handling**
- Immediate error feedback
- Graceful degradation
- Better debugging capabilities

## Implementation Details

### Backend Implementation
- Uses Spring's `SseEmitter` for streaming
- Handles multiple concurrent streams
- Proper error handling and cleanup
- Memory-efficient processing

### Frontend Implementation
- Uses `EventSource` API for SSE
- Automatic reconnection handling
- Graceful error recovery
- Progressive content display

## Testing Streaming Endpoints

```bash
# Test streaming assist for code generation
curl -X POST http://localhost:8080/api/v1/code/assist/stream \
  -H "Content-Type: application/json" \
  -d '{
    "code": "Create a REST API endpoint for user authentication",
    "language": "python",
    "analysisType": "WRITE_CODE"
  }' \
  --no-buffer

# Test with specific service
curl -X POST http://localhost:8080/api/v1/code/assist/GroqAIChatService/stream \
  -H "Content-Type: application/json" \
  -d '{
    "code": "Debug this JavaScript function",
    "language": "javascript",
    "analysisType": "DEBUG"
  }' \
  --no-buffer
```

## Error Handling

Streaming endpoints handle various error scenarios:

1. **Invalid Request**: Returns error event immediately
2. **AI Service Errors**: Propagates service-specific errors
3. **Network Issues**: Graceful timeout and cleanup
4. **Memory Issues**: Proper resource management

## Performance Considerations

- Streaming reduces memory usage on server
- Progressive rendering improves perceived performance
- Connection pooling for multiple concurrent users
- Automatic cleanup of completed streams 