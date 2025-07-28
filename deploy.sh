#!/bin/bash

# Configuration
PROJECT_ID="at-ai-labs"
SERVICE_NAME="at-ailabs-api"
REGION="us-central1"
IMAGE_NAME="gcr.io/$PROJECT_ID/$SERVICE_NAME"

echo "🚀 Starting deployment process..."

# Build and push using Google Cloud Build
echo "📦 Building and pushing Docker image using Google Cloud Build..."
gcloud builds submit --tag $IMAGE_NAME .

# Deploy to Cloud Run
echo "☁️  Deploying to Cloud Run..."
gcloud run deploy $SERVICE_NAME \
  --image $IMAGE_NAME \
  --platform managed \
  --region $REGION \
  --allow-unauthenticated \
  --port 8080 \
  --memory 1Gi \
  --cpu 1 \
  --max-instances 10

# Get the service URL
SERVICE_URL=$(gcloud run services describe $SERVICE_NAME --region=$REGION --format='value(status.url)')

echo "✅ Deployment complete!"
echo "🌐 Service URL: $SERVICE_URL"

# Test the deployment
echo "🧪 Testing the deployment..."
curl -s "$SERVICE_URL/actuator/health" | head -5
