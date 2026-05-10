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
- Do not implement backend-hosted AI yet.
- Do not implement detail edit/delete, search/list enhancement, or UI text cleanup.

## Decisions (Locked 2026-05-09)
1. Remote save boundary:
   - Remote save is contract-only for this step.
   - Existing `DiaryApi` / `DiaryRepository` contracts remain the placeholder boundary.
   - No real network implementation is wired in Step 10.
2. Category set:
   - Use the current diary AI set: `reading`, `food`, `mood`, `work`, `sports`, `entertainment`.
   - Blank or unsupported category falls back to `mood`.
3. Tag normalization:
   - Trim whitespace.
   - Drop blank values.
   - Remove duplicates.
   - Keep at most 5 tags.
4. Empty transcript handling:
   - Empty transcript must not be saved as `processed_succeeded`.
   - The caller must keep or move the entry into a failure state.
5. Local fake AI boundary:
   - `DiaryAssemblyUseCase` calls a configurable `DiaryAiProcessor` after ASR transcript success.
   - AI processor failure must not block diary creation.
   - On AI failure, save `rawTranscript`, fall back `polishedArticle` to transcript, and fall back category to `mood`.
   - Tags may be empty after normalization.

## Implementation Notes
- Added `DiaryAssemblyUseCase` as the Step 10 local assembly/save boundary.
- `Step9ProcessingUseCase` now keeps ASR responsibilities and delegates successful transcript persistence to `DiaryAssemblyUseCase`.
- The use case updates:
  - `rawTranscript`
  - `primaryCategoryId`
  - `dynamicTags`
  - `polishedArticle`
  - `processingStatus = processed_succeeded`
  - linked `SyncState` error fields when appropriate

## 2026-05-10 Stabilization Update
- Successful local diary assembly now keeps `SyncState.syncStatus = pending_upload`.
- The processing result is represented only by `DiaryEntry.processingStatus = processed_succeeded`.
- `SyncState.syncStatus` no longer stores `processed_succeeded`.
- Remote diary save remains deferred and must not be treated as implemented.

## 2026-05-10 Local Fake AI Update
- Added `DiaryAiProcessor` / `FakeDiaryAiProcessor` as the minimal local AI boundary.
- `DiaryAssemblyUseCase` now maps ASR transcript into:
  - `title`
  - `rawTranscript`
  - `polishedArticle`
  - `primaryCategoryId`
  - `dynamicTags`
- Tag normalization now trims, lowercases, removes blanks, removes duplicates, maps category synonyms, and keeps at most 5 tags.
- Category IDs (`work`, `study`, `life`, `emotion`, `health`) are not stored as dynamic tags.
- `work` / `工作` style synonyms are canonicalized to Chinese display tags, e.g. `工作`.
- Detail display separates category from dynamic tags; category remains in summary and the tags card displays only `dynamicTags`.
- Verified on real device by user after fixing duplicate display for the work/meeting transcript case.

## 2026-05-10 DeepSeek AI Update
- Added `DeepSeekDiaryAiProcessor` as the real AI provider.
- Added `DiaryAiPromptFactory` as the centralized prompt builder.
- `DiaryAiProcessor` now selects DeepSeek or fake processor based on `BuildConfig.DEEPSEEK_USE_FAKE`.
- DeepSeek configuration is read from `local.properties`:
  - `deepseek.apiKey`
  - `deepseek.baseUrl`
  - `deepseek.model`
  - `deepseek.useFake`
- DeepSeek uses chat completions with `response_format = json_object`.
- The prompt requires JSON only and maps transcript into `title`, `polishedArticle`, `primaryCategoryId`, and `dynamicTags`.
- The allowed category set is now `reading`, `food`, `mood`, `work`, `sports`, `entertainment`.
- `DiaryAssemblyUseCase` still normalizes category/tags and falls back to transcript if DeepSeek fails.

## Same-day Multiple Entry Validation Target
Pass criteria:
1. Record and process two entries on the same local date.
2. Both entries remain separate rows because `entryId` is the primary key and `entryDateLocal` is not unique.
3. Home list shows both entries through `observeActiveByUser`.
4. Tapping each Home card opens its own Detail route by `entryId`.

## Risk Notes
- Remote diary save is still not implemented; this is intentional for the current phase.
- Processing status and sync status are now separated for the local MVP.
- DeepSeek is currently called directly from Android for development validation; production should move the DeepSeek key behind a backend proxy.

## Sign-off Status
- Android: Implemented local diary assembly/save boundary.
- Product: Scope confirmed for local stabilization only.
- Backend: Contract boundary only; real integration deferred.
