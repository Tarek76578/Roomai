# RoomAI — AI Interior Designer Source Code

RoomAI is a commercial-ready Android + Flask source-code project for building
an AI-powered interior-design application.

## Features

- AI room redesign
- Room diagnosis
- AI enhancement
- Interior design styles
- Furniture workflow
- Product workflow
- Authentication foundation
- Free/Pro usage quotas
- AdMob integration
- Flask REST backend
- Gemini integration
- Magic Hour image generation
- JSON API responses
- SQLite usage persistence
- Render-compatible backend deployment

## Architecture

Android App
    |
    | HTTPS / JSON
    v
Flask Backend
    |
    +--> Gemini
    +--> Magic Hour
    +--> SQLite

## Requirements

### Android

- Android Studio
- JDK 17
- Android SDK 36

### Backend

- Python 3
- Flask
- Gunicorn
- Gemini API
- Magic Hour API

## Android Configuration

The backend URL is configurable.

Gradle property:

ROOMAI_BACKEND_BASE_URL=https://your-backend.example.com

Example:

./gradlew assembleDebug \
  -PROOMAI_BACKEND_BASE_URL=https://your-backend.example.com

The source package does not contain a production backend URL.
Configure your own HTTPS backend before using the application.

For production, always configure your own backend.

## AdMob Configuration

Debug builds use Google's official test IDs.

Production builds require:

ROOMAI_ADMOB_APP_ID=ca-app-pub-...
ROOMAI_INTERSTITIAL_AD_ID=ca-app-pub-...

Never commit real AdMob credentials.

## Backend Configuration

Copy backend.env.example into your deployment environment.

Required third-party credentials include:

- MAGIC_HOUR_API_KEY
- GEMINI_API_KEY

Optional/fallback Gemini configuration:

- GEMINI_API_KEY_31
- GEMINI_MODEL
- GEMINI_MODEL_31

Usage configuration:

- ROOMAI_FREE_MONTHLY_LIMIT
- ROOMAI_PRO_MONTHLY_LIMIT
- ROOMAI_PRO_DEVICE_IDS
- ROOMAI_USAGE_DB
- ROOMAI_MAX_UPLOAD_BYTES
- ROOMAI_SESSION_DAYS

## Backend Deployment

The backend can be deployed to a Python-compatible hosting provider.

Typical deployment:

1. Create a Python web service.
2. Connect this repository.
3. Install requirements.txt.
4. Use the included Procfile.
5. Configure environment variables.
6. Deploy.
7. Test the API.
8. Put the deployed HTTPS URL into ROOMAI_BACKEND_BASE_URL.

## Security

API keys must remain on the backend.

Do not place Gemini or Magic Hour secrets inside the Android application.

Do not distribute:

- private API keys
- production credentials
- private databases
- personal deployment configuration

## Customization

The purchaser can customize:

- application name
- application ID
- package name
- colors and theme
- branding
- AdMob IDs
- backend URL
- AI provider configuration
- usage limits
- backend deployment
- UI text

## Third-Party Services

RoomAI integrates with third-party services.

The purchaser supplies their own API accounts, API keys and service credits.

Third-party service pricing, availability and quotas are controlled by those
providers and are not included in the source-code purchase.

## Commercial Distribution

This project is intended to be distributed as source code.

Before commercial redistribution, verify the licenses of all third-party
libraries, APIs, icons, fonts, images and other assets.

## Support

Support should cover installation, configuration and documented project
functionality.

External API outages, provider account restrictions, provider pricing changes
and third-party service changes are outside the application's control.
