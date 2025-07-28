# Code Assistant CLI - System Diagram

## 🔄 System Flow

```mermaid
graph TD
    A[User Input] --> B[CLI Parser]
    B --> C{Input Type}
    C -->|Code Snippet| D[Analysis Request]
    C -->|File Path| E[File Reader]
    E --> F[Language Detection]
    F --> D
    D --> G[Prompt Builder]
    G --> H[OpenAI API]
    H --> I[GPT Model]
    I --> J[Response]
    J --> K[Result Formatter]
    K --> L[Output Display]
    
    style A fill:#e1f5fe
    style L fill:#c8e6c9
    style H fill:#fff3e0
    style I fill:#fff3e0
```

## 🏗️ Component Architecture

```mermaid
graph TB
    subgraph "CLI Layer"
        A[CodeAssistantCLI]
        B[Picocli Framework]
    end
    
    subgraph "Service Layer"
        C[CodeAnalysisService]
        D[OpenAI Integration]
    end
    
    subgraph "Model Layer"
        E[AnalysisRequest]
        F[AnalysisType]
    end
    
    subgraph "Utility Layer"
        G[FileUtils]
        H[Language Detection]
    end
    
    subgraph "External Services"
        I[OpenAI API]
        J[GPT Models]
    end
    
    A --> B
    A --> C
    C --> D
    C --> E
    C --> F
    A --> G
    G --> H
    D --> I
    I --> J
    
    style A fill:#e3f2fd
    style C fill:#f3e5f5
    style E fill:#e8f5e8
    style G fill:#fff3e0
    style I fill:#ffebee
```

## 📊 Data Flow Sequence

```mermaid
sequenceDiagram
    participant U as User
    participant CLI as CLI Layer
    participant S as Service Layer
    participant O as OpenAI API
    participant G as GPT Model
    
    U->>CLI: Execute command with code
    CLI->>CLI: Parse arguments
    CLI->>S: Create AnalysisRequest
    S->>S: Build prompt
    S->>O: Send API request
    O->>G: Process with GPT
    G->>O: Return response
    O->>S: API response
    S->>CLI: Formatted result
    CLI->>U: Display output
```

## 🔧 Analysis Types

```mermaid
graph LR
    A[Code Input] --> B{Analysis Type}
    B -->|EXPLAIN| C[Explanation Prompt]
    B -->|REFACTOR| D[Refactoring Prompt]
    B -->|DEBUG| E[Debugging Prompt]
    
    C --> F[GPT Processing]
    D --> F
    E --> F
    
    F --> G[Structured Output]
    
    style C fill:#e8f5e8
    style D fill:#fff3e0
    style E fill:#ffebee
```

## 🛡️ Error Handling

```mermaid
graph TD
    A[User Input] --> B{Validation}
    B -->|Valid| C[Process Request]
    B -->|Invalid| D[Show Error]
    
    C --> E{API Call}
    E -->|Success| F[Display Results]
    E -->|Failure| G[Handle API Error]
    
    G --> H{Error Type}
    H -->|Rate Limit| I[Retry Logic]
    H -->|Invalid Key| J[Authentication Error]
    H -->|Network| K[Connection Error]
    
    I --> E
    J --> D
    K --> D
    
    style D fill:#ffebee
    style F fill:#e8f5e8
    style G fill:#fff3e0
```

## 📈 Performance Metrics

```mermaid
graph LR
    A[Request Start] --> B[CLI Processing]
    B --> C[API Call]
    C --> D[Response Processing]
    D --> E[Output Display]
    
    B -.->|~50ms| F[CLI Time]
    C -.->|2-10s| G[API Time]
    D -.->|~100ms| H[Processing Time]
    
    style F fill:#e8f5e8
    style G fill:#fff3e0
    style H fill:#e3f2fd
```

## 🔐 Security Flow

```mermaid
graph TD
    A[API Key Input] --> B{Validation}
    B -->|Valid Format| C[Secure Transmission]
    B -->|Invalid| D[Error Message]
    
    C --> E[OpenAI API]
    E --> F[Code Analysis]
    F --> G[Response]
    
    G --> H[No Local Storage]
    H --> I[Display Only]
    
    style D fill:#ffebee
    style H fill:#e8f5e8
    style I fill:#e3f2fd
```

---

These diagrams provide a comprehensive view of the Code Assistant CLI system architecture, data flow, and security considerations. 