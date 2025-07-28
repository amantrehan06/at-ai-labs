#!/bin/bash

# Simple deployment script for AT AI Labs
# No Terraform needed - just basic gcloud commands

set -e

echo "🚀 Deploying AT AI Labs to Google Cloud..."

# Check if gcloud is installed
if ! command -v gcloud &> /dev/null; then
    echo "❌ Google Cloud SDK not found. Please install it first:"
    echo "   curl https://sdk.cloud.google.com | bash"
    exit 1
fi

# Get project ID
PROJECT_ID=$(gcloud config get-value project 2>/dev/null || echo "")

if [ -z "$PROJECT_ID" ]; then
    echo "❌ No Google Cloud project configured."
    echo "Please run: gcloud init"
    exit 1
fi

echo "📁 Project ID: $PROJECT_ID"

# Enable required APIs
echo "🔧 Enabling required APIs..."
gcloud services enable run.googleapis.com
gcloud services enable cloudbuild.googleapis.com
gcloud services enable containerregistry.googleapis.com

# Build and deploy Code Assistant
echo "🏗️ Building and deploying Code Assistant..."
cd services/code-assistant

# Build the container
gcloud builds submit --tag gcr.io/$PROJECT_ID/code-assistant .

# Deploy to Cloud Run
gcloud run deploy code-assistant \
  --image gcr.io/$PROJECT_ID/code-assistant \
  --region us-central1 \
  --platform managed \
  --allow-unauthenticated \
  --set-env-vars="SPRING_PROFILES_ACTIVE=prod"

# Get the service URL
SERVICE_URL=$(gcloud run services describe code-assistant --region=us-central1 --format="value(status.url)")

echo "✅ Deployment complete!"
echo "🌐 Code Assistant URL: $SERVICE_URL"
echo "🔍 Health check: $SERVICE_URL/api/v1/code/health"

cd ../..

echo ""
echo "📝 Next steps:"
echo "1. Test the service: curl $SERVICE_URL/api/v1/code/health"
echo "2. Set up GitHub Actions (optional):"
echo "   - Add GCP_PROJECT_ID and GCP_SA_KEY secrets to your GitHub repo"
echo "3. Create a master frontend (optional):"
echo "   - Add React app in frontend/ directory"
