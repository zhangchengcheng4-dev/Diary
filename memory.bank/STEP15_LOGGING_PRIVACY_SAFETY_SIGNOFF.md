# STEP15 Logging Privacy Safety Sign-off

## Scope
- Review local MVP logging and stored failure messages for sensitive content.
- Keep diagnostics useful without writing raw transcript, audio bytes, API secrets, auth tokens, passwords, or local file paths into logs or sync error fields.
- Preserve the current record -> ASR -> fake AI -> Room save flow.

## Explicit Non-goals For Current Step 15
- Do not connect backend, sync, WorkManager, or real AI.
- Do not change Room schema.
- Do not modify the ASR protocol, recording behavior, or audio processing pipeline.
- Do not clear local data or alter user-visible diary content.

## Findings
- `Step9ProcessingUseCase` already logs ASR transcript length only, not raw transcript content.
- `XfyunAudioApi` masks WebSocket authorization and API key in auth logs.
- Processing failure messages are stored in `SyncState.lastErrorMessage` for local retry/error display.
- Two privacy risks were found:
  - Step 9 debug log included full local audio path.
  - Xfyun file-not-found / decode failure messages could include local path or decoder details.

## Implementation Notes
- Removed full local audio path from Step 9 asset debug logs.
- Added Step 9 failure-message sanitization before storing `SyncState.lastErrorMessage`.
- Removed file path from Xfyun file-not-found error.
- Removed decoder exception detail from Xfyun PCM decode failure error.
- Kept useful non-sensitive diagnostics:
  - entry id
  - asset id
  - file existence
  - file size
  - mime type
  - duration
  - transcript length
  - Xfyun chunk/frame counts

## Validation
- Compile passed: `./gradlew :app:compileDebugKotlin`.

## Sign-off Status
- Android: Implemented logging/privacy cleanup.
- Product: No product behavior change.
- Backend: Not required for this step.
