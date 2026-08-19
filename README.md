# StoryForge AI

StoryForge is an Android-first AI writing studio for turning rough thoughts, notes, or voice input into finished writing.

## What StoryForge does

**Raw idea / voice → understand intent → choose a format → generate → edit → save.**

Supported formats include:

- Novel
- Movie Screenplay
- Short Story
- YouTube Script
- Article
- Essay
- Poetry
- Lyrics
- Dialogue
- Professional Writing
- Polished Writing

Writing preferences include tone, length, point of view, and language.

## Bring your own AI key

StoryForge does not ship with an API key. Each user can configure their own provider from **Settings → AI provider**.

The current real provider is an OpenAI-compatible chat-completions endpoint. Users configure:

- API endpoint
- Model
- API key

The API key is encrypted with Android Keystore-backed storage on the device. It is not stored in DataStore, committed to Git, or included in the project source.

A **Demo / Offline** mode is available so the app can be explored without credentials.

## Architecture

UI, domain models, AI, repositories, and local storage remain separated. `AiService` is the generation contract. `ConfiguredAiService` chooses between the real user-configured provider and demo mode without exposing credentials to UI code.

## Android build

Requirements: Android Studio Hedgehog+, JDK 17, Android SDK 34.

```bash
chmod +x gradlew
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
```

On Windows use `gradlew.bat`.

The debug APK is written to `app/build/outputs/apk/debug/`.

Voice input requires microphone permission. If speech recognition is unavailable, users can type instead.

## Product boundary

StoryForge is a writing/transformation app. It intentionally does **not** include a video generator, publishing platform, payments, accounts, or social feed.
