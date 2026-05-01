# Step 2 - Repository Modules and Folder Contracts Sign-off

## Goal
Establish clear module boundaries for MVP implementation and enforce dependency direction.

## Modules
1. `ui`
2. `viewmodel`
3. `domain`
4. `data`
5. `network`
6. `storage`
7. `sync`

## Folder Contract (Suggested Android structure)
```text
app/
  src/main/java/.../
    ui/
    viewmodel/
    domain/
      model/
      repository/
      usecase/
    data/
      repository/
      mapper/
    network/
      api/
      dto/
      datasource/
    storage/
      room/
      entity/
      dao/
      datasource/
    sync/
      worker/
      queue/
```

## Ownership and Responsibilities
1. `ui`
- Screens, composables, UI state rendering, navigation hooks.
- No business rules, no direct IO, no Retrofit/Room references.

2. `viewmodel`
- State holder and UI event handler.
- Calls `domain` use cases only.
- No direct `network` or `storage` access.

3. `domain`
- Business models, use cases, repository interfaces, core rules.
- Pure Kotlin layer; no Android framework dependency.

4. `data`
- Implements `domain` repository interfaces.
- Orchestrates read/write between `network`, `storage`, and `sync` triggers.
- Contains model mapping between DTO/entity/domain.

5. `network`
- Remote API client and DTO definitions.
- Handles request/response parsing and transport-level errors.
- No UI or domain logic.

6. `storage`
- Local persistence (Room DB, DAO, local datasources).
- Manages cached diary/auth/sync metadata records.

7. `sync`
- Offline queue and background retry workflow (WorkManager workers).
- Tracks sync state transitions and retry policies.

## Dependency Rules
Allowed direction:
`ui -> viewmodel -> domain <- data -> (network, storage, sync)`

Hard constraints:
1. `ui` MUST NOT depend on `network`.
2. `ui` MUST NOT depend on `storage`.
3. `viewmodel` MUST NOT depend on `network` or `storage` directly.
4. `domain` MUST NOT depend on `data`, `network`, `storage`, or `sync`.
5. `network`, `storage`, and `sync` MUST NOT depend on `ui` or `viewmodel`.

## Validation Test (for Step 2)
1. Architecture review confirms all 7 modules are documented.
2. Dependency direction is explicit and reviewable.
3. `ui -> network` direct dependency is explicitly prohibited.

## Sign-off Record
- Product: `Approved`
- Android: `Approved`
- Backend: `Approved`
- Date: `2026-05-01`
- Final Result: `Passed`
