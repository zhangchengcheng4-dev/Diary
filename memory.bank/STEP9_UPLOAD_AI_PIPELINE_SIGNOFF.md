# STEP9 Third-party ASR Local Diary Draft Sign-off

## Scope
- Auto start processing right after recording stops.
- Use mature third-party ASR directly from Android for the current phase. Current implementation: Xfyun WebSocket ASR.
- Convert recorded audio to transcript and persist it into the local diary draft.
- Make the processed local diary visible in the Home list.
- Keep `AudioApi` as a local abstraction shape for ASR processing, but it is **not** a self-hosted backend upload/job pipeline in this phase.

## Explicit Non-goals For Current Step 9
- Do not implement self-hosted backend upload.
- Do not implement backend AI job creation or backend polling.
- Do not implement final AI category classification, dynamic tag generation, or article polishing yet.
- Do not continue to Step 10 until this local loop is stable:
  `recording -> third-party ASR -> transcript -> local diary -> Home visible`.

## Decisions (Locked 2026-05-09)
1. ASR provider strategy:
   - Android calls a mature ASR service directly for this phase.
   - Current provider: Xfyun WebSocket ASR.
   - The implementation should be named and documented as third-party ASR processing, not backend AI pipeline.
2. Segmented recording strategy:
   - Do not concatenate `.m4a` container bytes.
   - Process each recorded segment independently through ASR, then join transcript text.
3. State machine for current phase:
   - `recorded_pending_upload` -> `processing` -> `processed_succeeded` / `processed_failed`
   - These names are current implementation states and should be reconciled later with the canonical sync states.
4. Result persistence fields:
   - `rawTranscript`: real ASR transcript.
   - `primaryCategoryId`: temporary placeholder, default `life`.
   - `dynamicTags`: temporary placeholder, empty.
   - `polishedArticle`: temporary placeholder, equals `rawTranscript`.

## Temporary AI Placeholder Rule
Until the later AI step is implemented:
- `category` uses a default value.
- `tags` is empty.
- `polishedArticle` equals `transcript`.
- Code must mark these as `Temporary AI placeholder` or equivalent TODO.

## Validation Targets
1. End-to-end flow from recording stop to local `processed_succeeded`.
2. Xfyun ASR returns a non-empty transcript for a real recording.
3. Failure path transitions to `processed_failed` and stores a non-sensitive error code/message.
4. Transcript is saved in Room and appears in the Home list.
5. Logs and sync debug fields must not store raw transcript content or audio content.

## 2026-05-10 Stabilization Update
- `DiaryEntry.processingStatus` is now treated as the only processing lifecycle field.
- `SyncState.syncStatus` is no longer written with `processing`, `processed_succeeded`, or `processed_failed`.
- During ASR processing, `SyncState.syncStatus` remains `pending_upload`.
- On ASR/local processing failure, `SyncState.syncStatus` becomes `failed`.
- Debug trace messages are logged through Android debug logging and are no longer stored in `SyncState.lastErrorMessage`.
- `lastErrorMessage` is reserved for real failure reasons.

## Sign-off Status
- Android: Implemented as third-party ASR local loop; real-device MVP validation passed after stabilization.
- Product: Current scope clarified.
- Backend: Not required for this Step 9 phase.
