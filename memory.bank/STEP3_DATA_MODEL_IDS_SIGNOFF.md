# Step 3 - Data Model and IDs Sign-off

## Goal
Finalize MVP core entities and ID rules so record -> process -> read flows have complete, consistent data coverage.

## Entities in Scope
1. `User`
2. `DiaryEntry`
3. `AudioAsset`
4. `Category`
5. `DynamicTag`
6. `SyncState`

## ID and Timestamp Standards
1. Primary IDs use `String` UUID (v4 suggested) generated client-side for offline safety.
2. Foreign keys store parent ID values as `String`.
3. Time fields use UTC ISO-8601 (`yyyy-MM-dd'T'HH:mm:ss'Z'`).
4. Device display date is derived from `entryOccurredAt` with user timezone.

## Entity Definitions

### 1) User
Required fields:
1. `userId: String`
2. `authProvider: String` (`email_password`)
3. `phoneOrEmailMasked: String`
4. `sessionToken: String`
5. `sessionExpiresAt: String`
6. `createdAt: String`
7. `updatedAt: String`

### 2) DiaryEntry
Required fields:
1. `entryId: String`
2. `userId: String`
3. `entryOccurredAt: String` (when user recorded)
4. `entryDateLocal: String` (`yyyy-MM-dd`, supports multi-entry same day)
5. `title: String` (can be generated preview title)
6. `rawTranscript: String`
7. `polishedArticle: String`
8. `primaryCategoryId: String`
9. `processingStatus: String` (`draft` | `processing` | `processed` | `failed`)
10. `audioAssetId: String`
11. `syncStateId: String`
12. `createdAt: String`
13. `updatedAt: String`
14. `deletedAt: String?` (soft delete support)

### 3) AudioAsset
Required fields:
1. `audioAssetId: String`
2. `entryId: String`
3. `userId: String`
4. `localPath: String`
5. `remoteUrl: String?`
6. `durationMs: Long`
7. `mimeType: String`
8. `fileSizeBytes: Long`
9. `checksumSha256: String?`
10. `uploadStatus: String` (`pending` | `uploading` | `uploaded` | `failed`)
11. `createdAt: String`
12. `updatedAt: String`

### 4) Category
Required fields:
1. `categoryId: String`
2. `name: String` (MVP fixed set: `reading`, `food`, `mood`, `work`, `sports`, `entertainment`)
3. `displayName: String`
4. `colorToken: String`
5. `isSystemPreset: Boolean`
6. `createdAt: String`
7. `updatedAt: String`

### 5) DynamicTag
Required fields:
1. `tagId: String`
2. `entryId: String`
3. `userId: String`
4. `tagText: String`
5. `source: String` (`ai` | `manual` | `user_edited`)
6. `createdAt: String`
7. `updatedAt: String`

### 6) SyncState
Required fields:
1. `syncStateId: String`
2. `entityType: String` (`diary_entry` | `audio_asset` | `ai_processing`)
3. `entityId: String`
4. `userId: String`
5. `syncStatus: String` (`draft` | `pending_upload` | `uploading` | `processing` | `synced` | `failed` | `deleted`)
6. `retryCount: Int`
7. `lastErrorCode: String?`
8. `lastErrorMessage: String?`
9. `lastAttemptAt: String?`
10. `nextRetryAt: String?`
11. `createdAt: String`
12. `updatedAt: String`

## Relationship Rules
1. One `User` has many `DiaryEntry`.
2. One `DiaryEntry` has one `AudioAsset` (MVP baseline).
3. One `DiaryEntry` has one primary `Category`.
4. One `DiaryEntry` has many `DynamicTag`.
5. One `DiaryEntry` links to one active `SyncState`, with optional additional sync rows for related `AudioAsset`.
6. Deleting an entry uses soft-delete for the diary record and immediate local audio file removal.
7. Editing `entryDateLocal` must also update `entryOccurredAt` and reflow list/calendar grouping immediately.

## Data-model Checklist for Step 3 Validation
Pass criteria:
1. All 6 required entities exist and are defined.
2. IDs and FK relationships cover create/process/read flows.
3. Required text outputs are present on `DiaryEntry` (`rawTranscript`, `polishedArticle`).
4. Audio lifecycle fields exist on `AudioAsset`.
5. Sync/retry fields exist on `SyncState`.
6. Multi-entry same day is supported via `entryDateLocal` + unique `entryId`.

## Sign-off Record
- Product: `Approved`
- Android: `Approved`
- Backend: `Approved`
- Date: `2026-05-01`
- Final Result: `Passed`
