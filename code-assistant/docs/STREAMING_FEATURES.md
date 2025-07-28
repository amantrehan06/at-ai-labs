# Streaming Features

## Overview

The Code Assistant service now supports **real-time streaming responses** using Server-Sent Events (SSE) to provide a more polished user experience. This allows users to see AI responses as they are generated, rather than waiting for the complete response.

## Features

### ✅ Implemented Features

1. **Streaming Endpoints**: All analysis endpoints now have streaming variants
2. **Server-Sent Events (SSE)**: Real-time streaming using Spring's SSE support
3. **Error Handling**: Proper error handling in streaming responses
4. **Reactive Programming**: Built with Reactor/Flux for non-blocking operations
5. **Strategy Pattern Integration**: Streaming works with all existing analysis strategies

### 🔄 Streaming Endpoints

| Regular Endpoint | Streaming Endpoint | Description |
|------------------|-------------------|-------------|
| `POST /api/v1/code/analyze` | `POST /api/v1/code/analyze/stream` | Generic analysis with streaming |
| `POST /api/v1/code/explain` | `POST /api/v1/code/explain/stream` | Code explanation with streaming |
| `POST /api/v1/code/refactor` | `POST /api/v1/code/refactor/stream` | Code refactoring with streaming |
| `POST /api/v1/code/debug` | `POST /api/v1/code/debug/stream` | Code debugging with streaming |
| `POST /api/v1/code/analyze/{service}` | `POST /api/v1/code/analyze/{service}/stream` | Service-specific analysis with streaming |

### 📊 Response Format

Streaming responses use the `StreamingAnalysisResponse` model:

```json
{
  "eventType": "content|complete|error",
  "content": "streaming text chunk",
  "analysisType": "EXPLAIN|REFACTOR|DEBUG|COMPREHENSIVE",
  "language": "java|python|javascript|...",
  "isComplete": false,
  "error": "error message if any",
  "success": true
}
```

### 🔧 Technical Implementation

#### Architecture
- **Reactor/Flux**: Non-blocking reactive streams
- **SSE Emitter**: Spring's Server-Sent Events support
- **StreamingResponseHandler**: LangChain4j integration (placeholder)
- **Strategy Pattern**: Maintains existing design patterns

#### Components
1. **StreamingAnalysisResponse**: Model for streaming events
2. **AIChatService.streamAnalysis()**: Interface for streaming
3. **CodeAnalysisController**: SSE endpoints
4. **AIServiceManager**: Streaming model management

### 🚀 Usage Examples

#### JavaScript Client Example
```javascript
const eventSource = new EventSource('/api/v1/code/explain/stream');

eventSource.onmessage = function(event) {
    const response = JSON.parse(event.data);
    
    if (response.eventType === 'content') {
        // Append content to UI
        document.getElementById('output').innerHTML += response.content;
    } else if (response.eventType === 'complete') {
        // Handle completion
        eventSource.close();
    } else if (response.eventType === 'error') {
        // Handle error
        console.error(response.error);
        eventSource.close();
    }
};
```

#### cURL Example
```bash
curl -X POST http://localhost:8080/api/v1/code/explain/stream \
  -H "Content-Type: application/json" \
  -d '{
    "code": "public class Hello { public static void main(String[] args) { System.out.println(\"Hello World\"); } }",
    "language": "java",
    "apiKey": "your-api-key"
  }'
```

### 🔮 Future Enhancements

#### TODO: Full LangChain4j Integration
- **Current**: Placeholder implementation
- **Future**: Full streaming with LangChain4j's `StreamingResponseHandler`
- **Benefits**: Real token-by-token streaming from AI models

#### Planned Features
1. **WebSocket Support**: Alternative to SSE for bi-directional communication
2. **Streaming Metrics**: Monitor streaming performance
3. **Rate Limiting**: Prevent abuse of streaming endpoints
4. **Caching**: Cache streaming responses for repeated queries

### 🛠️ Configuration

#### Dependencies
```xml
<!-- Reactor for streaming support -->
<dependency>
    <groupId>io.projectreactor</groupId>
    <artifactId>reactor-core</artifactId>
</dependency>
```

#### Properties
```properties
# Streaming timeout (default: 60 seconds)
spring.mvc.async.request-timeout=60000
```

### 🧪 Testing

#### Test Streaming Endpoints
```bash
# Test streaming explain
curl -X POST http://localhost:8080/api/v1/code/explain/stream \
  -H "Content-Type: application/json" \
  -d '{"code":"console.log(\"Hello\")","language":"javascript"}'

# Test streaming with specific service
curl -X POST http://localhost:8080/api/v1/code/analyze/openai/stream \
  -H "Content-Type: application/json" \
  -d '{"code":"print(\"Hello\")","language":"python","apiKey":"your-key"}'
```

### 📈 Benefits

1. **Improved UX**: Users see responses in real-time
2. **Better Feedback**: Immediate indication that processing has started
3. **Reduced Perceived Latency**: Content appears as it's generated
4. **Error Handling**: Immediate error feedback
5. **Scalability**: Non-blocking reactive streams

### 🔒 Security Considerations

1. **API Key Validation**: Streaming endpoints validate API keys
2. **Rate Limiting**: Consider implementing rate limits for streaming
3. **Timeout Handling**: Proper timeout handling for long-running streams
4. **Error Boundaries**: Graceful error handling and cleanup

## Conclusion

The streaming features provide a foundation for real-time AI interactions while maintaining the existing architecture and design patterns. The placeholder implementation allows for immediate testing and UI development, with full LangChain4j integration planned for future releases. 