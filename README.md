# Gemini Fashion Styler Spring Boot on Cloud Run

This project is a Spring Boot application deployed to Google Cloud Run. It provides:

- A web UI for uploading a photo of an outfit.
- Uses Google’s Gemini multimodal API to generate styling suggestions, including simple improvements and an image editing prompt.
- Generates a new image with improved outfit styling using the Gemini image editing model.
- Returns the result as Base64-encoded PNG in JSON (no files are saved on the server).

## Requirements

- Java 21.
- Maven.
- A Gemini API key stored in an environment variable `GOOGLE_API_KEY` (or provided via Secret Manager on Cloud Run).

## Running locally

```
export GOOGLE_API_KEY=YOUR_KEY
mvn spring-boot:run
```

Then open http://localhost:8080/ to access the UI.

## Deployment

Enable the required services and deploy to Cloud Run from source:

```
gcloud services enable run.googleapis.com cloudbuild.googleapis.com artifactregistry.googleapis.com
gcloud run deploy gemini-fashion-styler --source . --region us-central1 --allow-unauthenticated --set-secrets GOOGLE_API_KEY=GEMINI_API_KEY:latest
```
