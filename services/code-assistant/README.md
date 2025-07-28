# Code Assistant API

A Spring Boot REST API that uses LangChain4j and multiple AI providers (OpenAI, Groq) to analyze code. Demonstrates Strategy Pattern and Factory Pattern for AI service management.

## 🚀 Features

- **Multiple AI Providers**: Support for OpenAI GPT models and Groq (for Llama models)
- **Strategy Pattern**: Different analysis types (explain, refactor, debug, comprehensive)
- **Factory Pattern**: Dynamic AI service selection based on availability
- **REST API**: Clean REST endpoints for code analysis
- **Web Interface**: Simple HTML frontend for testing

## 🔐 API Key Setup

**IMPORTANT**: Never commit API keys to version control!

### Option 1: Environment Variables (Recommended)
```bash
export OPENAI_API_KEY=your_openai_api_key_here
export GROQ_API_KEY=your_groq_api_key_here
```

### Option 2: Development Properties File
1. Copy the template: `cp application-dev.properties.template application-dev.properties`
2. Edit `application-dev.properties` and add your API keys
3. Run with: `mvn spring-boot:run -Dspring.profiles.active=dev`

### Option 3: Frontend Input
Enter your API keys directly in the web interface when making requests.

## 🛠️ Prerequisites

- Java 17+
- Maven 3.6+
- API keys for OpenAI and/or Groq

## 📦 Installation

1. Clone the repository:
```bash
git clone <repository-url>
cd ai-sample-api-codeassistant
```

2. Set up your API keys (see API Key Setup above)

3. Run the application:
```bash
mvn spring-boot:run
```

4. Access the web interface: http://localhost:8080

## 🎯 Usage

### Web Interface
- Open http://localhost:8080
- Select AI service (OpenAI or Llama via Groq)
- Enter your API key
- Paste code and select analysis type
- Get instant analysis results

### REST API Endpoints

#### Health Check
```bash
GET /api/v1/code/health
```

#### Code Analysis
```bash
POST /api/v1/code/analyze
Content-Type: application/json

{
  "code": "public int factorial(int n) { return n <= 1 ? 1 : n * factorial(n-1); }",
  "language": "java",
  "analysisType": "EXPLAIN",
  "apiKey": "your_api_key_here"
}
```

#### Specific Analysis Types
```bash
POST /api/v1/code/explain
POST /api/v1/code/refactor
POST /api/v1/code/debug
POST /api/v1/code/analyze
```

#### Service-Specific Analysis
```bash
POST /api/v1/code/analyze/OpenAIChatService
POST /api/v1/code/analyze/GroqAIChatService
```

#### Available Services
```bash
GET /api/v1/code/services
GET /api/v1/code/services/stats
```

## 🏗️ Architecture

### Design Patterns

#### Strategy Pattern
- **AI Services**: `OpenAIChatService`, `GroqAIChatService`
- **Analysis Types**: `ExplainAnalysisStrategy`, `RefactorAnalysisStrategy`, etc.

#### Factory Pattern
- `AIServiceFactory`: Manages and provides AI service instances

### Package Structure
```
src/main/java/com/codeassistant/
├── controller/          # REST endpoints
├── model/              # DTOs and enums
├── service/
│   ├── ai/            # AI service implementations
│   ├── factory/       # Factory pattern
│   ├── strategy/      # Analysis strategies
│   └── CodeAnalysisService.java
└── config/            # Configuration classes
```

## 🔧 Configuration

### Application Properties
```properties
# Server
server.port=8080

# OpenAI
openai.api.key=${OPENAI_API_KEY:}
openai.model=gpt-3.5-turbo

# Groq (for Llama models)
groq.api.key=${GROQ_API_KEY:}
groq.model=llama3-8b-8192
```

## 🧪 Testing

```bash
# Run tests
mvn test

# Run with test profile (uses MockAIChatService)
mvn spring-boot:run -Dspring.profiles.active=test
```

## 🔒 Security

- API keys are never stored in code
- Use environment variables or secure configuration
- Frontend allows user input of API keys
- All sensitive files are in `.gitignore`

## 📝 License

This project is for educational purposes and demonstrates design patterns in Java.

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests
5. Submit a pull request

## 📞 Support

For issues or questions, please open an issue on GitHub. 