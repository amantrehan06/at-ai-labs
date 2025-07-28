# 🚀 AT AI Labs - AI-Powered Development Platform

A comprehensive monorepo showcasing modern AI integration with Java Spring Boot, featuring multiple AI services and a professional web interface.

## 🎯 Project Overview

**AT AI Labs** is a full-stack AI platform demonstrating advanced backend development skills, cloud deployment, and AI integration. Perfect for showcasing to recruiters and demonstrating full-stack capabilities.

### ✨ Key Features

- **🤖 Multiple AI Providers**: OpenAI GPT and Groq Llama integration
- **🎨 Professional UI**: Modern, responsive design with consistent theming
- **☁️ Cloud Native**: Deployed on Google Cloud Run
- **🏗️ Monorepo Architecture**: Scalable service structure
- **🔧 Strategy Pattern**: Clean, maintainable code architecture
- **📱 Responsive Design**: Works on all devices

## 🏗️ Architecture

```
at-ai-labs/
├── services/
│   └── code-assistant/          # ✅ Code analysis service
│       ├── src/main/java/       # Spring Boot backend
│       ├── src/main/resources/  # Frontend & config
│       └── deploy.sh           # Deployment script
├── shared/
│   ├── ui-components/          # 🎨 Shared UI library
│   └── api-client/             # 🔌 Shared API client
├── docs/                       # 📚 Documentation
└── infrastructure/             # 🏗️ Cloud resources
```

## 🚀 Quick Start

### Prerequisites

- **Java 17+**
- **Maven 3.6+**
- **Google Cloud CLI**
- **Git**

### Local Development

1. **Clone the repository**
   ```bash
   git clone https://github.com/amantrehan06/at-ai-labs.git
   cd at-ai-labs
   ```

2. **Navigate to service**
   ```bash
   cd services/code-assistant
   ```

3. **Set up API keys** (optional)
   ```bash
   # Create application.properties with your keys
   cp src/main/resources/application.properties.example src/main/resources/application.properties
   # Edit with your API keys
   ```

4. **Run locally**
   ```bash
   mvn spring-boot:run
   ```

5. **Access the application**
   - Landing Page: http://localhost:8080/
   - Code Assistant: http://localhost:8080/code-assistant

### Cloud Deployment

1. **Deploy to Google Cloud**
   ```bash
   cd services/code-assistant
   ./deploy.sh
   ```

2. **Access deployed application**
   - URL will be displayed after deployment
   - Example: https://code-assistant-api-xxx-uc.a.run.app

## 🛠️ Technology Stack

### Backend
- **Java 17** - Modern Java features
- **Spring Boot 3.2.0** - Rapid application development
- **LangChain4j 0.27.1** - AI integration framework
- **Maven** - Build automation

### AI Integration
- **OpenAI API** - GPT models for code analysis
- **Groq API** - Llama models for fast inference
- **Strategy Pattern** - Clean service architecture

### Frontend
- **HTML5/CSS3** - Modern, responsive design
- **JavaScript (ES6+)** - Dynamic interactions
- **Shared UI Components** - Consistent theming

### Cloud & DevOps
- **Google Cloud Run** - Serverless container platform
- **Google Cloud Build** - Automated builds
- **Docker** - Containerization
- **GitHub** - Version control

## 📁 Project Structure

### Services
```
services/
├── code-assistant/              # ✅ Code analysis service
│   ├── src/main/java/
│   │   ├── controller/         # REST controllers
│   │   ├── service/           # Business logic
│   │   ├── model/             # Data models
│   │   └── config/            # Configuration
│   ├── src/main/resources/
│   │   ├── static/            # Frontend files
│   │   └── application.properties
│   ├── Dockerfile             # Container definition
│   └── deploy.sh             # Deployment script
└── [future services]          # 🔮 AI Chat, Code Generator, etc.
```

### Shared Components
```
shared/
├── ui-components/
│   └── styles.css             # 🎨 Shared CSS theme
└── api-client/
    └── api-client.js          # 🔌 Shared API client
```

## 🎨 Design System

### Color Palette
- **Primary**: `#667eea` to `#764ba2` (Gradient)
- **Background**: Linear gradient
- **Cards**: White with subtle shadows
- **Text**: Dark gray (`#333`)

### Typography
- **Font**: Segoe UI, Tahoma, Geneva, Verdana, sans-serif
- **Headers**: Bold with text shadows
- **Code**: Courier New monospace

### Components
- **Cards**: Rounded corners, gradient top border
- **Buttons**: Gradient backgrounds, hover animations
- **Forms**: Clean inputs with focus states
- **Status Bars**: Glassmorphism effect

## 🔧 API Endpoints

### Health Check
```http
GET /api/v1/health
```

### Code Analysis
```http
POST /api/v1/code/{type}
POST /api/v1/code/analyze/{service}
```

### Service Information
```http
GET /api/v1/code/services
GET /api/v1/code/services/stats
```

## 🚀 Deployment

### Google Cloud Run
- **Automatic scaling** based on demand
- **Serverless** - pay only for usage
- **Global CDN** for fast access
- **SSL certificates** included

### Environment Variables
```bash
# Required for AI services
OPENAI_API_KEY=sk-...
GROQ_API_KEY=gsk_...

# Optional
SPRING_PROFILES_ACTIVE=prod
```

## 📊 Performance

- **Cold Start**: ~2-3 seconds
- **Warm Start**: <500ms
- **Memory**: 1GB allocated
- **CPU**: 1 vCPU
- **Max Instances**: 10 (auto-scaling)

## 🔒 Security

- **HTTPS only** in production
- **API key validation** on all AI requests
- **CORS configured** for web access
- **Input sanitization** on all endpoints

## 🧪 Testing

### Unit Tests
```bash
mvn test
```

### Integration Tests
```bash
mvn verify
```

### Manual Testing
1. **Health Check**: Verify API is running
2. **Code Analysis**: Test with sample code
3. **Service Selection**: Test different AI providers
4. **Error Handling**: Test with invalid inputs

## 📈 Monitoring

### Google Cloud Console
- **Logs**: Application and access logs
- **Metrics**: Request count, latency, errors
- **Alerts**: Error rate monitoring

### Application Metrics
- **Health checks** every 30 seconds
- **Service availability** monitoring
- **API response times** tracking

## 🔮 Future Enhancements

### Planned Services
- **AI Chat Service** - Conversational AI
- **Code Generator** - AI-powered code generation
- **Documentation Generator** - Auto-documentation
- **Test Generator** - AI test case generation

### Technical Improvements
- **Microservices** architecture
- **React/Vue** frontend
- **Database integration** (PostgreSQL)
- **Authentication** system
- **Rate limiting** and quotas

## 🤝 Contributing

1. **Fork** the repository
2. **Create** a feature branch
3. **Make** your changes
4. **Test** thoroughly
5. **Submit** a pull request

## 📄 License

This project is for demonstration purposes. Feel free to use as a reference for your own projects.

## 👨‍💻 Author

**Aman Trehan** - Backend Software Engineer
- **GitHub**: [@amantrehan06](https://github.com/amantrehan06)
- **LinkedIn**: [Aman Trehan](https://linkedin.com/in/amantrehan)

---

## 🎯 For Recruiters

This project demonstrates:

✅ **Full-Stack Development** - Backend + Frontend + DevOps  
✅ **Modern Java** - Spring Boot, Java 17, Maven  
✅ **AI Integration** - Multiple providers, clean architecture  
✅ **Cloud Deployment** - Google Cloud, Docker, CI/CD  
✅ **Professional UI/UX** - Responsive, modern design  
✅ **Code Quality** - Clean architecture, documentation  
✅ **Scalability** - Monorepo structure, microservices ready  

**Perfect for showcasing backend engineering skills with modern technologies!** 🚀 # Contributors fix
