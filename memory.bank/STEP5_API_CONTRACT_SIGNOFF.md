# Step 5 - API Contract Sign-off

## Goal
Define stable MVP API contracts for auth, audio upload + AI processing, diary list/detail, and sync, with clear error/retry behavior.

## Scope
1. Authentication (`email + password` only for MVP).
2. Audio upload and AI processing result retrieval.
3. Diary list/detail fetch and save/update/delete baseline contracts.
4. Sync pull/push contracts and conflict handling.
5. Error model and retryability classification.

## Product Decisions (Confirmed 2026-05-03)
1. Login method:
   - MVP supports `email + password` only.
   - Phone + verification-code login is deferred to future phases.
2. AI response fields (required):
   - `transcript`
   - `category`
   - `tags`
   - `polishedArticle`
3. Fixed category set (MVP):
   - `work`, `study`, `life`, `emotion`, `health`
4. Dynamic tag rule:
   - Max `5` tags per entry.
   - User edit is allowed in detail edit flow.
5. Diary list API:
   - Reverse chronological order.
   - Pagination enabled.
   - Page size fixed to `20` for MVP.
6. Sync conflict policy:
   - Server wins on conflict in MVP.
7. Error handling:
   - Network failures are retryable.
   - Authentication/authorization failures are non-retryable.
8. Audio constraints:
   - Format: `m4a/aac`
   - Max duration: `30 minutes`
   - Processing starts automatically after upload.

## API Surface (MVP)
1. `POST /v1/auth/login`
2. `POST /v1/audio/upload`
3. `GET /v1/audio/jobs/{jobId}`
4. `GET /v1/diaries?page={n}&pageSize=20`
5. `GET /v1/diaries/{entryId}`
6. `POST /v1/diaries`
7. `PUT /v1/diaries/{entryId}`
8. `DELETE /v1/diaries/{entryId}`
9. `POST /v1/sync/push`
10. `GET /v1/sync/pull?cursor={cursor}`

## Contract Shape Requirements
1. Every response includes:
   - `requestId: String`
   - `serverTime: String (ISO8601)`
2. Standard error envelope:
   - `error.code: String`
   - `error.message: String`
   - `error.retryable: Boolean`
3. Time fields are ISO8601 UTC unless explicitly marked local date.
4. IDs are string-based stable IDs.

## Validation Checklist
1. Frontend can implement all required MVP flows using these APIs only.
2. Backend can produce all required AI result fields and category/tag constraints.
3. Retry policy is deterministic (`network=true`, `auth/permission=false`).
4. Sync conflict outcome is deterministic (`server-wins`).

## Sign-off Record
- Product: `Confirmed`
- Android: `Confirmed`
- Backend: `Confirmed`
- Date: `2026-05-03`
- Final Result: `Step 5 Fully Confirmed (Product/Android/Backend)`

