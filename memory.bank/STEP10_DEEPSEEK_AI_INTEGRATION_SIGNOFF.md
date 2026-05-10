# STEP10 DeepSeek AI Integration Sign-off

## Scope
- Replace the local fake diary AI processor with a real DeepSeek-backed provider when configured.
- Keep `DiaryAiProcessor` as the diary AI boundary after ASR transcript success.
- Keep local fallback behavior: AI HTTP failure, JSON parse failure, invalid fields, or missing config must not block diary creation.

## Explicit Non-goals For Current Slice
- Do not connect backend, sync, or WorkManager.
- Do not change Room schema.
- Do not modify `XfyunAudioApi`, `AudioRecorder`, or the ASR/recording main chain.
- Do not perform UI polish or UI redesign.
- Do not commit API keys or local secrets.

## Implementation Notes
- Added DeepSeek configuration through `local.properties` and `BuildConfig`:
  - `deepseek.apiKey`
  - `deepseek.baseUrl`
  - `deepseek.model`
  - `deepseek.useFake`
- Added `DeepSeekDiaryAiProcessor` using DeepSeek chat completions.
- Added `DiaryAiPromptFactory` so prompt text is centralized and not scattered through business logic.
- DeepSeek request uses `response_format = {"type":"json_object"}` and non-streaming chat completion.
- Prompt requires JSON-only output with:
  - `title`
  - `polishedArticle`
  - `primaryCategoryId`
  - `dynamicTags`
- Prompt rules:
  - Preserve oral diary feeling.
  - Do not invent facts.
  - Classification must be one of `reading`, `food`, `mood`, `work`, `sports`, `entertainment`.
  - Tags are max 5, unique, and must not duplicate the category.
- `DiaryAssemblyUseCase` still normalizes and validates AI output before saving.
- Failure path continues to save the original transcript and fallback content.

## Privacy and Safety Notes
- `apiKey`, transcript content, and local audio paths are not logged by the DeepSeek provider.
- The API key is read from local build config only and must not be committed.
- Direct DeepSeek calls from Android are acceptable for current development validation only; production should move the DeepSeek key behind a backend proxy.

## Validation
- `./gradlew :app` was attempted but is not a valid Gradle task in this project.
- Compile passed: `./gradlew :app:compileDebugKotlin`.

## Sign-off Status
- Android: Implemented real DeepSeek provider with fake fallback.
- Product: Category set updated for the current diary taxonomy.
- Backend: Still deferred; backend proxy is the recommended next security step.
