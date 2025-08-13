# AT-AI-Labs Deployment Guide

## Overview

This application uses a **hybrid approach** for API key management:
- **Local Development**: Uses Java VM options (`-D`) for convenience
- **Cloud Run**: Uses environment variables for production deployment

## Required API Keys

### API Keys (Required)
- `OPENAI_API_KEY` - Your OpenAI API key
- `GROK_API_KEY` - Your Groq API key

## Running Locally

### Option 1: VM Options (Recommended for Local Development)
Set these in your IDE's VM options:
```
-DOPENAI_API_KEY="your-openai-api-key"
-DGROK_API_KEY="your-grok-api-key"
```

### Option 2: Environment Variables
```bash
export OPENAI_API_KEY="your-openai-api-key"
export GROK_API_KEY="your-grok-api-key"
mvn spring-boot:run -pl executor
```

## Running with Docker

### Build the Image
```bash
docker build -t at-ai-labs .
```

### Run with Environment Variables
```bash
docker run -d \
  --name at-ai-labs \
  -p 8080:8080 \
  -e OPENAI_API_KEY="your-openai-api-key" \
  -e GROK_API_KEY="your-grok-api-key" \
  at-ai-labs:latest
```

## Running with Docker Compose

### Option 1: Environment File
```bash
# Copy and edit the environment file
cp env.example .env
# Edit .env with your actual API keys

# Start the service
docker-compose up -d
```

### Option 2: Direct Environment Variables
```bash
OPENAI_API_KEY="your-key" GROK_API_KEY="your-key" docker-compose up -d
```

## Google Cloud Run Deployment

### Deploy with Environment Variables
```bash
gcloud run deploy at-ai-labs-api \
  --image gcr.io/at-ai-labs/at-ai-labs-api \
  --platform managed \
  --region us-central1 \
  --allow-unauthenticated \
  --port 8080 \
  --set-env-vars OPENAI_API_KEY="your-openai-api-key",GROK_API_KEY="your-grok-api-key"
```

### Update Environment Variables Later
```bash
gcloud run services update at-ai-labs-api \
  --region us-central1 \
  --set-env-vars OPENAI_API_KEY="your-key",GROK_API_KEY="your-key"
```

## How the Hybrid Approach Works

The application automatically detects and uses the best available source:

1. **First Priority**: Java System Properties (VM options: `-DOPENAI_API_KEY=...`)
2. **Second Priority**: Environment Variables (`OPENAI_API_KEY=...`)
3. **Fallback**: Throws clear error message if neither is available

This means:
- ✅ **Local Development**: Works with VM options in your IDE
- ✅ **Docker**: Works with environment variables
- ✅ **Cloud Run**: Works with environment variables
- ✅ **CI/CD**: Works with environment variables

## Testing

Once running, test the application:

```bash
# Health check
curl http://localhost:8080/actuator/health

# Main application
curl http://localhost:8080/
```

## Environment Variable Reference

| Variable | Required | Default | Description |
|----------|----------|---------|-------------|
| `OPENAI_API_KEY` | ✅ | - | OpenAI API key |
| `GROK_API_KEY` | ✅ | - | Groq API key |
| `PORT` | ❌ | 8080 | Server port |
| `OPENAI_MODEL` | ❌ | gpt-3.5-turbo | OpenAI model name |
| `GROQ_MODEL` | ❌ | llama3-8b-8192 | Groq model name |
| `LOGGING_LEVEL_COM_ATAILABS` | ❌ | INFO | Application logging level |
| `LOGGING_LEVEL_ORG_SPRINGFRAMEWORK_WEB` | ❌ | INFO | Spring Web logging level |

## Security Notes

- Never commit API keys to version control
- Use VM options for local development (convenient)
- Use environment variables for production deployments
- In production, use your platform's secret management (Docker secrets, Kubernetes secrets, etc.)

## Troubleshooting

### Common Issues

1. **API Key Not Found**: Ensure either VM options or environment variables are set correctly
2. **Permission Denied**: Check if the application has access to the port
3. **Model Not Found**: Verify the model names are correct for your API provider

### Debug Mode

Enable debug logging:
```bash
export LOGGING_LEVEL_COM_ATAILABS=DEBUG
export LOGGING_LEVEL_ORG_SPRINGFRAMEWORK_WEB=DEBUG
```

### Check API Key Source

The application logs which source it's using:
- "Retrieved OpenAI API key from system property (VM option)" - Using VM options
- "Retrieved OpenAI API key from environment variable" - Using environment variables 