# Step 4 - UI Placeholder Phase Sign-off

## Goal
Build a mock-only Compose UI shell so the app can demonstrate page structure and navigation before real business logic is connected.

## In Scope
1. `DiaryListScreen`
2. `RecordScreen`
3. `ProcessingScreen`
4. `DiaryDetailScreen`
5. `CalendarScreen`
6. `SearchScreen`
7. `ProfileScreen`

## UI Rules
1. Use mock data only.
2. Do not connect voice recognition, AI, login, or cloud sync.
3. Keep one composable file per page.
4. Use simple UI building blocks only: `Text`, `Card`, `Button`.
5. Keep a soft, warm, light background with rounded cards and tag chips.

## Required Flows
1. Home -> detail on card click.
2. Home -> record -> processing -> detail with fake delay.
3. Bottom navigation switches the main pages.

## Validation Test
1. Every page renders with mock data.
2. All stated flows are reachable.
3. No real backend or AI logic is invoked.

## Sign-off Record
- Product: `Pending`
- Android: `Pending`
- Backend: `Pending`
- Date: `Pending`
- Final Result: `Pending`
