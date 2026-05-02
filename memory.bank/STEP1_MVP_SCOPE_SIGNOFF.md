# Step 1 - MVP Scope & Acceptance Criteria Sign-off

## Goal
Freeze the base MVP scope for the diary app so implementation can proceed without scope drift.

## In Scope (MVP)
1. User authentication (email+password login, session persistence, logout).
2. Voice recording from app UI (start/pause/resume/stop, duration indicator, local audio file saved).
3. Audio upload and AI processing pipeline (transcript + fixed category + dynamic tags + polished article).
4. Diary persistence (local + cloud baseline, multiple entries per same date supported).
5. Home list view (reverse chronological cards with date/category/tags/preview).
6. Diary detail view (full content + transcript + audio playback + basic edit/delete).
7. Offline-to-online baseline sync (queue and retry after network recovery).
8. Basic loading/error/empty states for core flows.

## Out of Scope (This Phase)
1. Advanced search filtering and highlight logic.
2. Full calendar interaction behaviors beyond basic marker support.
3. Deep analytics/reporting dashboards.
4. Large-scale performance tuning and non-critical UX polish.
5. Multi-tenant/admin tooling.

## Decision Backfill (2026-05-02)
1. Source-of-truth priority:
   - `IMPLEMENTATION_PLAN.md` > `architecture.md` > `tec.md` > `progress.md`
2. MVP auth baseline:
   - `email+password` only.
   - `sessionToken + sessionExpiresAt` only (no refresh token in MVP; re-login when expired).
3. Recording baseline:
   - `m4a/aac`, max `30 minutes`, upload retry on failure with failed state retained.
   - Auto-start upload + AI processing after recording ends (no manual confirmation).
4. Fixed category set:
   - `reading`, `food`, `mood`, `work`, `sports`, `entertainment`
5. Dynamic tag source values:
   - `ai`, `manual`, `user_edited`
6. Detail edit scope:
   - Editable: title, article, category, dynamic tags, date.
   - Not editable: original audio file.
7. Deletion baseline:
   - Soft-delete diary entry.
   - Delete local audio file immediately.
   - Sync deletion state to remote; remote audio cleaned asynchronously by backend.
8. Offline/sync baseline:
   - Canonical sync states: `draft`, `pending_upload`, `uploading`, `processing`, `synced`, `failed`, `deleted`.
   - Offline new entry: `draft/pending_upload`.
   - Failed upload/processing remains `failed` and supports manual retry; no auto-rollback.
9. Date policy:
   - `entryDateLocal` is computed by device local timezone at recording time.
   - Editing date must update both `entryOccurredAt` and `entryDateLocal`, and immediately affect list order and calendar grouping.

## Non-Functional Baseline
1. Target Android 6.0+.
2. No sensitive data in logs.
3. Core user data survives app restart and network interruption.

## Acceptance Criteria
1. Product, Android, and backend owners all confirm the same in-scope/out-of-scope list.
2. No MVP feature is missing from the “In Scope” list required for base loop:
   record -> process -> save -> browse -> sync.
3. Any requested new feature is either:
   - added to “In Scope” with explicit approval, or
   - moved to “Out of Scope” for later phase.

## Validation Test (for Step 1)
Run a scope review meeting and produce explicit sign-off:
1. Reviewer list includes Product + Android + Backend.
2. Each reviewer marks `Approved` or `Blocked`.
3. Result is considered pass only when all required reviewers mark `Approved`.

## Sign-off Record
- Product: `Pending`
- Android: `Pending`
- Backend: `Pending`
- Date: `Pending`
- Final Result: `Pending`
