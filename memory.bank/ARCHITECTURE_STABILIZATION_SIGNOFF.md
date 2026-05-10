# Architecture Stabilization Sign-off

## Scope
This record covers the post-Step12 architecture stabilization pass completed on 2026-05-10.

The goal was to stabilize the existing local MVP loop without adding new product features:

`Record -> Processing -> third-party ASR -> local diary save -> Home -> Detail -> edit/delete`

## Explicit Constraints
- No new AI integration.
- No backend diary save or backend sync implementation.
- No WorkManager / Step13 offline queue.
- No Room schema migration.
- No database clearing.
- No `XfyunAudioApi` main implementation changes.
- No `AudioRecorder` changes.
- No large UI refactor or visual redesign.

## Completed Stabilization
1. Status semantics:
   - `DiaryEntry.processingStatus` is limited to processing lifecycle states:
     - `draft_recording`
     - `recorded_pending_upload`
     - `processing`
     - `processed_succeeded`
     - `processed_failed`
   - `SyncState.syncStatus` is limited to sync lifecycle states:
     - `pending_upload`
     - `uploading`
     - `synced`
     - `failed`
     - `deleted`
   - ASR/local save success keeps `SyncState.syncStatus = pending_upload`.
   - ASR/local processing failure writes `SyncState.syncStatus = failed`.
   - Detail soft delete writes `SyncState.syncStatus = deleted`.

2. Error handling:
   - Debug messages are no longer written into `SyncState.lastErrorMessage`.
   - `lastErrorMessage` is reserved for real failure reasons.
   - Processing UI filters legacy `[debug]` text if old local data contains it.

3. Local repository boundary:
   - Added `LocalDiaryRepository` as a thin Room access boundary for current MVP screens.
   - `ProcessingViewModel`, `DiaryDetailDbViewModel`, and `DiaryListDbViewModel` no longer directly access Room DAOs.
   - `AppRoot` restart recovery now queries recoverable entries through `LocalDiaryRepository`.

4. MVP validation cleanup:
   - User-facing validation text in Record/Processing/Detail was checked for mojibake in the active files.
   - Detail edit/save continues to refresh through Room observers.
   - Home hides soft-deleted entries through the existing active-entry query.
   - Detail audio segment lookup/playback path is preserved.

## Validation
- Kotlin compile passed:
  - `./gradlew :app:compileDebugKotlin`
- Real-device MVP validation passed per product/tech-lead check:
  - record
  - processing
  - ASR success
  - Home visibility
  - Detail open
  - edit/save refresh
  - soft delete removes from Home
  - segmented audio remains readable/playable

## Remaining Known Debt
- `DiaryAssemblyUseCase`, `RecordingRepository`, and `Step9ProcessingUseCase` still access Room directly. This is tolerated because they are non-UI local orchestration boundaries for the current MVP.
- `DynamicTag` remains stored as comma-separated text in `DiaryEntry.dynamicTags`.
- `Category` remains a fixed string set, not a table.
- Auth still uses a fake/debug-capable path for local validation.
- `ui.placeholder` package names remain in the active app shell.
- Backend AI, remote diary save, and Step13 sync are still not implemented.

## Next Gate
Do not start full backend sync yet. The next acceptable feature slice is a small AI result boundary around classification/tags/polishing while preserving the current local ASR loop.
