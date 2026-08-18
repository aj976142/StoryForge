# StoryForge AI architecture

```
UI  →  ViewModels / app.js  →  Repositories  →  Local store
                              ↘ AiService (interface)
                                 ↳ MockAiService (V1)
                                 ↳ future OpenAI / Gemini / Claude
```

## Android packages

- `ui.*` — Compose screens only. No file I/O, no prompt templates.
- `data.repository` — project and settings use cases.
- `data.local` — JSON project files + DataStore preferences.
- `data.ai.AiService` — the only generation API the UI may call.
- `di.AppContainer` — composition root. Swap `aiService` here.

## Failure policy

Generation, save, rename, delete, and speech recognition catch errors and surface them in the UI. Failed operations do not crash the process.

## Autosave

The input and editor screens debounce persistence so a process kill does not lose the current draft.
