# Base Implementation Plan (MVP)

This plan covers the **base product loop only**: create diary by voice, process with AI, save, and view. Advanced features (full search filters, rich calendar behaviors, deep analytics, large-scale optimization) are deferred.

Feature development is paused after the planning phase and before real business logic. Insert the UI placeholder stage first.

## 1. Freeze MVP Scope and Acceptance Criteria
- Confirm MVP includes: voice record, AI processing, diary save, diary list, entry detail, login, cloud sync baseline.
- Define out-of-scope list for this phase.
- Test: Product review sign-off document exists and matches all teams' understanding.

## 2. Create Repository Modules and Folder Contracts
- Set up separate modules/folders for `ui`, `viewmodel`, `domain`, `data`, `network`, `storage`, `sync`.
- Define ownership and boundaries for each module.
- Test: Architecture review confirms no direct `ui -> network` dependency.

## 3. Define Data Model and IDs
- Finalize entities: User, DiaryEntry, AudioAsset, Category, DynamicTag, SyncState.
- Define required fields, timestamps, and unique IDs.
- Test: Data-model checklist confirms all fields needed for record/process/read flows are present.

## 4. Insert UI Placeholder Stage
- Build mock-only Jetpack Compose screens for `DiaryListScreen`, `RecordScreen`, `ProcessingScreen`, `DiaryDetailScreen`, `CalendarScreen`, `SearchScreen`, and `ProfileScreen`.
- Use mock data only. Do not connect real voice recognition, AI, login, or cloud sync.
- Implement page flow and bottom navigation:
  - Home -> detail on card click.
  - Home -> record -> processing -> detail with fake delay.
  - Bottom navigation switches all main pages.
- Keep code structure simple: one composable file per page.
- Test: Every page renders with mock data and all stated flows can be reached.

## 5. Define API Contracts
- Specify request/response schemas for login, upload audio, processing result, diary fetch, sync.
- Lock Step 5 decisions:
  - Auth: `email+password` only for MVP (phone+OTP deferred).
  - AI result fields: `transcript`, `category`, `tags`, `polishedArticle`.
  - Fixed category set: `work`, `study`, `life`, `emotion`, `health`.
  - Dynamic tag limit: max `5`, user-editable.
  - Diary list: reverse chronological + pagination, `pageSize=20`.
  - Sync conflict: server-wins.
  - Retry policy: network errors retryable; auth/permission non-retryable.
  - Audio constraints: `m4a/aac`, max `30 minutes`, auto-start AI after upload.
- Document error codes and retry behavior.
- Test: API contract review passes with frontend and backend agreement.

## 6. Build Local Persistence Foundation
- Set up Room schema and migration policy for MVP entities.
- Store pending uploads and processed entries locally.
- Test: Create/read/update/delete test cases pass for DiaryEntry and SyncState.

## 7. Implement Authentication Baseline
- Add registration/login flow with `email+password` for MVP.
- Persist session token securely and handle logout.
- Use `sessionToken + sessionExpiresAt` only for MVP (no refresh token, no auto-renew; require re-login on expiry).
- Test: Successful login persists session; invalid credentials return expected error state.

## 8. Implement Voice Recording Flow
- Add record start/pause/resume/stop UX with timer and recording status.
- Save audio locally with link to a draft diary record.
- Use `m4a/aac` format and max duration `30 minutes` for MVP.
- Test: 3-minute recording completes, file exists, and draft entry is created.

## 9. Implement Upload + AI Processing Pipeline
- Auto-start upload and AI processing immediately after recording ends (no manual user confirmation).
- Upload recorded audio to backend.
- Receive transcript, fixed category, dynamic tags, and polished article.
- Test: End-to-end processing of one sample audio returns all required result fields.

Current implementation note:
- The current MVP uses direct third-party ASR from Android through the `AudioApi` abstraction, not a self-hosted backend upload/job pipeline.
- Segmented recordings are processed segment-by-segment, then transcript text is joined.
- Category, tags, title, and polished article are currently produced by a local fake AI boundary after ASR succeeds.
- Real AI API/backend processing remains deferred.
- Backend upload/job processing remains deferred.

## 10. Implement Diary Assembly and Save
- Merge AI result into a complete diary entry and persist locally and remotely.
- Ensure one day supports multiple entries.
- Test: Two entries on same date save correctly and are independently retrievable.

Current implementation note:
- Diary assembly currently persists locally only.
- Remote diary save is still contract-only and must not be assumed complete.
- `polishedArticle` currently comes from the local fake AI processor and falls back to the ASR transcript if local AI processing fails.
- Dynamic tags are normalized and deduplicated before local save; category IDs are not duplicated as tags.
- `SyncState.syncStatus` remains `pending_upload` after local save so future sync can pick it up.

## 11. Implement Home List Screen (MVP)
- Display diary cards in reverse chronological order.
- Show date, category, tags, and short preview text.
- Test: With seed data across dates, order and card content are correct.

## 12. Implement Diary Detail Screen (MVP)
- Show full article, transcript, tags, category, and audio playback action.
- Add basic edit and delete actions.
- Editable fields in MVP: `title`, `polishedArticle`, `primaryCategoryId`, `DynamicTag`, `entryOccurredAt/entryDateLocal`.
- Audio file is not editable in MVP.
- Test: Open detail from list, play audio, edit text, delete entry, and verify list updates.

Current implementation note:
- Detail edit/delete is local-only.
- Soft delete hides the entry from Home and marks `SyncState.syncStatus = deleted`.
- Local audio files and `audio_assets` are intentionally preserved for now.
- Remote delete is still deferred.

## 13. Implement Offline Queue and Background Sync
- Queue upload/process tasks when offline.
- Use background worker to retry when network is available.
- Keep failed states for manual retry (no auto-rollback).
- Test: Create entry offline, reconnect network, confirm automatic upload and state transition to synced.

## 14. Add Loading, Error, and Empty States
- Define consistent UI states for recording, processing, network failure, and no entries.
- Ensure actionable retry paths.
- Test: Simulate timeout/network failure and verify expected UI state and retry behavior.

Current implementation note:
- Processing screen now maps local processing states into user-facing loading, pending, processing, success, failure, missing-entry, and empty-transcript states.
- Failure shows stored local error code/message and keeps manual retry.
- Network/backend sync states remain deferred because backend sync and WorkManager are not connected.

## 15. Add Logging, Privacy, and Safety Controls
- Add structured logs for key flow milestones without sensitive content leakage.
- Ensure user can delete entry and corresponding remote data references.
- Test: Log review confirms no raw secrets/audio content; delete flow removes data as designed.

## 16. Execute MVP QA and Release Gate
- Run regression on core scenarios: first login, first recording, processing, browse, offline sync recovery.
- Track and fix P0/P1 issues only for MVP release.
- Test: QA checklist reaches pass criteria for all MVP-critical scenarios.

