# StoryForge AI

Android-first writing studio. Speak or type a raw thought, pick a form, and let a mock (swap-in) AI turn it into a novel chapter, screenplay, short story, YouTube script, or polished prose.

## User journey

Home → **New Project** → Voice or Text → raw idea → output format → generate → edit → save.

V1 formats: Novel, Movie Screenplay, Short Story, YouTube Script, Polished Writing.

## Architecture

UI, business logic, AI, and storage are separate.

| Layer | Android | Shared behavior |
| --- | --- | --- |
| UI | Jetpack Compose screens + Navigation | `web/` Material-style mobile client |
| Domain | `domain/model` | `web/js/domain` |
| AI | `AiService` + `MockAiService` | `MockAiService` in `web/js/services/ai.js` |
| Storage | JSON files in app storage + DataStore settings | `localStorage` stores |

`AiService` is the only generation contract. V1 uses **MockAiService** so the app runs without an API key. A later OpenAI / Gemini / Claude implementation can replace the mock without touching screens.

Not in V1: image/video generation, payments, accounts, social, publishing.

## Android app

```
app/src/main/java/com/storyforge/ai/
  ui/           Compose screens (home, input, format, generation, editor, projects, settings)
  data/ai/      AiService interface + mock writer
  data/local/   JSON project store + settings
  data/repository/
  di/           AppContainer (manual DI)
```

Requirements: Android Studio Hedgehog+, JDK 17, Android SDK 34.

```bash
# macOS / Linux
chmod +x gradlew
./gradlew :app:assembleDebug

# Windows
gradlew.bat :app:assembleDebug
```

Install the debug APK from `app/build/outputs/apk/debug/`. Grant microphone permission for voice input. If speech recognition is unavailable, type instead — generation still works.

Unit tests (JVM):

```bash
./gradlew :app:testDebugUnitTest
```

These cover mock generation, text metrics, and create → edit → save → reopen → rename → delete against the JSON store.

## Interactive preview

A mobile web client implements the same journey, mock writer, autosave, and local project storage so the product can be used in a browser.

```bash
node web/server.mjs
# http://0.0.0.0:4173
```

Logic tests (no browser):

```bash
node web/js/verify.mjs
```

## Screens

1. **Home** — StoryForge AI, New Project, Voice Input, Type Idea, recent projects
2. **Input** — large editor, microphone, clear, word/character count, Continue
3. **Format** — selectable cards for all five V1 forms
4. **Generation** — progress, stop, error + retry
5. **Editor** — edit text, regenerate, continue writing, copy, save (autosave)
6. **Projects** — title, last modified, open, rename, delete
7. **Settings** — theme, AI provider placeholder, writing preferences
