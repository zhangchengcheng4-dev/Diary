# STEP14 Loading Error Empty States Sign-off

## Scope
- Improve local MVP loading, processing, success, failure, missing-entry, and empty-transcript states.
- Keep the existing local record -> ASR -> fake AI -> Room save flow unchanged.
- Make failure states actionable through manual retry.

## Explicit Non-goals For Current Step 14
- Do not connect backend, sync, WorkManager, or real AI.
- Do not change Room schema.
- Do not modify `XfyunAudioApi`, `AudioRecorder`, or the recording/ASR main path.
- Do not redesign the full UI.

## Implementation Notes
- `ProcessingScreen` now maps raw processing states to user-facing titles and descriptions.
- Active states show a progress indicator:
  - `loading`
  - `recorded_pending_upload`
  - `processing`
- Failure states show stored error code and error message from `SyncState`.
- Success states expose the existing `查看详情` action.
- Failure states expose the existing `重试处理` action.
- Empty transcript copy is explicit and state-dependent.
- The screen states document that processing is still local-only and real AI/backend sync remain deferred.

## Validation
- Compile passed: `./gradlew :app:compileDebugKotlin`.

## Sign-off Status
- Android: Implemented local processing-state UI cleanup.
- Product: MVP state behavior is clearer without changing flow.
- Backend: Not required for this step.
