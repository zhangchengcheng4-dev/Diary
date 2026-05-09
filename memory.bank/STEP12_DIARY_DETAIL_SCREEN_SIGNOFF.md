# STEP12 Diary Detail Screen Sign-off

## Scope
- Implement MVP local edit/delete actions for the Room-backed diary detail screen.
- Keep audio playback available and keep audio files non-editable.
- Keep all changes local-only; do not connect AI, remote diary APIs, or background sync.

## Implemented
1. Detail read path:
   - Detail opens from Home by `entryId`.
   - Detail observes the Room diary row and refreshes after local updates.
2. Display:
   - Shows title, display time, transcript, polished article, category, tags, and audio playback.
3. Edit:
   - Same-screen edit mode.
   - Saving stays on Detail, exits edit mode, and refreshes from Room.
   - Editable fields:
     - `title`
     - `polishedArticle`
     - `primaryCategoryId`
     - `dynamicTags`
     - `entryDateLocal`
   - Editing date accepts `yyyy-MM-dd`.
   - `entryOccurredAt` is recalculated by preserving the original local time-of-day when possible.
   - Tags are stored in the current comma-separated local field, trimmed, deduplicated, and capped at 5.
4. Delete:
   - Delete uses local soft delete on `DiaryEntry.deletedAt`.
   - Local audio files and `audio_assets` rows are intentionally not deleted in this step.
   - Home list filters `deletedAt IS NULL`, so the deleted entry is removed from Home after returning.

## Explicit Non-goals
- No AI integration.
- No Step 13 background sync.
- No remote push/pull or diary API implementation.
- No ASR, Xfyun, Step 9, or Step 10 main-chain changes.
- No local audio cleanup; this is deferred to a separate step.

## Validation Targets
1. Open a Room diary from Home.
2. Play audio from Detail.
3. Edit allowed fields and save.
4. Confirm Detail exits edit mode and shows the latest local data.
5. Delete the diary.
6. Confirm navigation returns to Home and the soft-deleted entry no longer appears.

## Risk Notes
- `DynamicTag` is currently represented by the existing comma-separated `dynamicTags` field, not a separate tag table.
- Soft delete does not update remote sync state in this step.
- Soft delete does not remove local audio files or `audio_assets`; cleanup is intentionally deferred.

## Sign-off Status
- Android: Implemented local MVP edit/delete behavior.
- Product: Confirmed save/delete behavior and date editing constraint.
- Backend: Not required for this local-only Step 12 slice.
