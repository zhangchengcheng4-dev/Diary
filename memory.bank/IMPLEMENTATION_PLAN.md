# Base Implementation Plan (MVP)

This plan covers the **base product loop only**: create diary by voice, process with AI, save, and view. Advanced features (full search filters, rich calendar behaviors, deep analytics, large-scale optimization) are deferred.

## 1. Freeze MVP Scope and Acceptance Criteria
- Confirm MVP includes: voice record, AI processing, diary save, diary list, entry detail, login, cloud sync baseline.
- Define out-of-scope list for this phase.
- Test: Product review sign-off document exists and matches all teams’ understanding.

## 2. Create Repository Modules and Folder Contracts
- Set up separate modules/folders for `ui`, `viewmodel`, `domain`, `data`, `network`, `storage`, `sync`.
- Define ownership and boundaries for each module.
- Test: Architecture review confirms no direct `ui -> network` dependency.

## 3. Define Data Model and IDs
- Finalize entities: User, DiaryEntry, AudioAsset, Category, DynamicTag, SyncState.
- Define required fields, timestamps, and unique IDs.
- Test: Data-model checklist confirms all fields needed for record/process/read flows are present.

## 4. Define API Contracts
- Specify request/response schemas for login, upload audio, processing result, diary fetch, sync.
- Document error codes and retry behavior.
- Test: API contract review passes with frontend and backend agreement.

## 5. Build Local Persistence Foundation
- Set up Room schema and migration policy for MVP entities.
- Store pending uploads and processed entries locally.
- Test: Create/read/update/delete test cases pass for DiaryEntry and SyncState.

## 6. Implement Authentication Baseline
- Add registration/login flow (phone+OTP or email+password per final decision).
- Persist session token securely and handle logout.
- Test: Successful login persists session; invalid credentials return expected error state.

## 7. Implement Voice Recording Flow
- Add record start/stop UX with timer and recording status.
- Save audio locally with link to a draft diary record.
- Test: 3-minute recording completes, file exists, and draft entry is created.

## 8. Implement Upload + AI Processing Pipeline
- Upload recorded audio to backend.
- Receive transcript, fixed category, dynamic tags, and polished article.
- Test: End-to-end processing of one sample audio returns all required result fields.

## 9. Implement Diary Assembly and Save
- Merge AI result into a complete diary entry and persist locally and remotely.
- Ensure one day supports multiple entries.
- Test: Two entries on same date save correctly and are independently retrievable.

## 10. Implement Home List Screen (MVP)
- Display diary cards in reverse chronological order.
- Show date, category, tags, and short preview text.
- Test: With seed data across dates, order and card content are correct.

## 11. Implement Diary Detail Screen (MVP)
- Show full article, transcript, tags, category, and audio playback action.
- Add basic edit and delete actions.
- Test: Open detail from list, play audio, edit text, delete entry, and verify list updates.

## 12. Implement Offline Queue and Background Sync
- Queue upload/process tasks when offline.
- Use background worker to retry when network is available.
- Test: Create entry offline, reconnect network, confirm automatic upload and state transition to synced.

## 13. Add Loading, Error, and Empty States
- Define consistent UI states for recording, processing, network failure, and no entries.
- Ensure actionable retry paths.
- Test: Simulate timeout/network failure and verify expected UI state and retry behavior.

## 14. Add Logging, Privacy, and Safety Controls
- Add structured logs for key flow milestones without sensitive content leakage.
- Ensure user can delete entry and corresponding remote data references.
- Test: Log review confirms no raw secrets/audio content; delete flow removes data as designed.

## 15. Execute MVP QA and Release Gate
- Run regression on core scenarios: first login, first recording, processing, browse, offline sync recovery.
- Track and fix P0/P1 issues only for MVP release.
- Test: QA checklist reaches pass criteria for all MVP-critical scenarios.
