# Real AI integration

StoryForge now includes `HttpAiService`, an OpenAI-compatible implementation of the existing `AiService` contract.

## Security
Do not hard-code an API key in the Android APK. Production should use:

`Android app -> your backend -> AI provider`

For local testing, construct `HttpAiService(apiKey = ...)` from a secure runtime source.

The endpoint and model are configurable, so an OpenAI-compatible provider can be used without changing the UI/domain layer.
