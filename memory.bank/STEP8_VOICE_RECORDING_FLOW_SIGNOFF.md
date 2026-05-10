# STEP8 Voice Recording Flow Sign-off

## Scope
- Implement start/pause/resume/stop recording UX with timer.
- Save recording in app-specific storage (`files/recordings/`).
- Create local draft at recording start with fixed user id (`mvp_local_user`).
- Support paused recording as segmented files, persisted in sequence.
- Auto-stop at 30 minutes and save draft.
- On permission denial or recording failure, clean empty draft and temp audio.

## Implementation Notes
- Added runtime microphone permission request (`RECORD_AUDIO`) with simplified one-time denial hint.
- Added recording state machine (`IDLE/RECORDING/PAUSED/STOPPED/ERROR`) via `RecordViewModel`.
- Added local persistence bridge `RecordingRepository`:
  - `createDraft(userId)`
  - `saveSegmentsAndFinalize(userId, entryId, segments)`
  - `clearDraftAndFiles(entryId)`
- Added `AudioRecorder` based on `MediaRecorder` (`MPEG_4` + `AAC`, `.m4a`).
- Updated `audio_assets` indexing to allow multiple segments per diary entry.

## Validation
- Build check command: `:app:compileDebugKotlin`
- 2026-05-10 compile passed: `./gradlew :app:compileDebugKotlin`.
- 2026-05-10 real-device MVP validation passed through the record -> processing -> ASR -> Home -> Detail loop.
- Segmented recording remains the active strategy; `AudioRecorder` was not changed during the stabilization pass.

## Sign-off Status
- Android: Completed and compile-verified.
- Product: Accepted for the current local MVP loop.
- Backend: N/A (Step8 is local recording flow only).
