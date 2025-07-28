#!/bin/bash

# Exit on any error
set -e

echo "🚀 Starting deployment to Google Cloud Run..."

# Build the application
echo "📦 Building the application..."
mvn clean package -DskipTests

# Set variables
PROJECT_ID="at-ai-labs"
SERVICE_NAME="code-assistant-api"
REGION="us-central1"
IMAGE_NAME="gcr.io/$PROJECT_ID/$SERVICE_NAME"

# Build and push Docker image
echo "🐳 Building and pushing Docker image..."
gcloud builds submit --tag $IMAGE_NAME .

# Deploy to Cloud Run
echo "☁️ Deploying to Cloud Run..."
gcloud run deploy $SERVICE_NAME \
  --image $IMAGE_NAME \
  --platform managed \
  --region $REGION \
  --allow-unauthenticated \
  --port 8080 \
  --memory 1Gi \
  --cpu 1 \
  --max-instances 10 \
  --set-env-vars="SPRING_PROFILES_ACTIVE=prod"

echo "✅ Deployment completed!"
echo "🌐 Your application is available at:"
gcloud run services describe $SERVICE_NAME --region $REGION --format="value(status.url)" 