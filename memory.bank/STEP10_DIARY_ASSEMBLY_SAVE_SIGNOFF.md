# STEP10 Diary Assembly and Save Sign-off

## Scope
- Assemble a completed local diary entry from the Step 9 ASR transcript.
- Persist the assembled diary fields into local Room storage.
- Keep the current third-party ASR path intact; Step 10 starts only after ASR returns transcript text.
- Confirm same-day multiple entries are supported by independent `entryId` records and Home/Detail Room reads.

## Explicit Non-goals For Current Step 10
- Do not connect real backend diary create/update APIs.
- Do not implement remote audio upload, backend job creation, or backend polling.
- Do not modify `XfyunAudioApi` or the ASR upload/job polling main path.
- Do not implement AI category classification, dynamic tag generation, or article polishing yet.
- Do not implement detail edit/delete, search/list enhancement, or UI text cleanup.

## Decisions (Locked 2026-05-09)
1. Remote save boundary:
   - Remote save is contract-only for this step.
   - Existing `DiaryApi` / `DiaryRepository` contracts remain the placeholder boundary.
   - No real network implementation is wired in Step 10.
2. Category set:
   - Use the Step 5/9 MVP set: `work`, `study`, `life`, `emotion`, `health`.
   - Blank or unsupported category falls back to `life`.
3. Tag normalization:
   - Trim whitespace.
   - Drop blank values.
   - Remove duplicates.
   - Keep at most 5 tags.
4. Empty transcript handling:
   - Empty transcript must not be saved as `processed_succeeded`.
   - The caller must keep or move the entry into a failure state.
5. Temporary AI placeholder:
   - Until a later AI step exists, `polishedArticle` equals the ASR transcript when no polished article is supplied.
   - Tags may be empty.
   - Category defaults to `life`.

## Implementation Notes
- Added `DiaryAssemblyUseCase` as the Step 10 local assembly/save boundary.
- `Step9ProcessingUseCase` now keeps ASR responsibilities and delegates successful transcript persistence to `DiaryAssemblyUseCase`.
- The use case updates:
  - `rawTranscript`
  - `primaryCategoryId`
  - `dynamicTags`
  - `polishedArticle`
  - `processingStatus = processed_succeeded`
  - linked `SyncState` success/error fields

## Same-day Multiple Entry Validation Target
Pass criteria:
1. Record and process two entries on the same local date.
2. Both entries remain separate rows because `entryId` is the primary key and `entryDateLocal` is not unique.
3. Home list shows both entries through `observeActiveByUser`.
4. Tapping each Home card opens its own Detail route by `entryId`.

## Risk Notes
- Remote diary save is still not implemented; this is intentional for the current phase.
- The canonical sync-state names still differ from the temporary Step 9/10 processing statuses and should be reconciled in a later sync step.
- AI category/tag/polishing remain placeholders and should not be treated as final AI output quality.

## Sign-off Status
- Android: Implemented local diary assembly/save boundary.
- Product: Scope confirmed for local stabilization only.
- Backend: Contract boundary only; real integration deferred.
