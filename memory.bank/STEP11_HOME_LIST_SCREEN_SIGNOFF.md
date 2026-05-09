# STEP11 Home List Screen Sign-off

## Scope
- Validate the MVP Home list against the current local Room-backed diary flow.
- Keep this step as documentation/sign-off only.
- Do not add pagination UI, automated seed tests, sync behavior, or unrelated Home refactors in this step.

## Completed
1. Room-backed Home list:
   - `DiaryListDbScreen` reads local entries from `DiaryEntryDao.observeActiveByUser`.
   - The current main Home route uses Room data, not only mock data.
2. Reverse chronological order:
   - DAO query orders active entries by `entryOccurredAt DESC`.
3. Diary card content:
   - Cards show local date text, title, short preview, category, and up to two tags.
   - Preview falls back to status text for draft/processing/failed states.
4. Local loop visibility:
   - Step 9/10 local loop can produce Room entries that appear on Home after processing.

## Partially Completed
1. Pagination:
   - DAO has `limit` and `offset` parameters.
   - Home UI does not implement incremental paging or load-more behavior.
2. Seed/mock validation:
   - Mock data still exists for placeholder/non-Room routes.
   - Current Step 11 acceptance relies on Room real-device loop validation instead of automated seed-data tests.

## Not Done In Step 11
- No pagination UI.
- No automated seed-data test.
- No search/list enhancement.
- No Step 13 background sync or remote list integration.

## Validation Basis
- Current validation basis is the Room-backed real-device loop:
  `record -> ASR -> diary assembly/save -> Home visible -> Detail opens by entryId`.

## Sign-off Status
- Android: Current Home List MVP baseline documented.
- Product: Accepted current Room real-device loop as validation basis for this step.
- Backend: Not required for this Step 11 local Home list sign-off.
