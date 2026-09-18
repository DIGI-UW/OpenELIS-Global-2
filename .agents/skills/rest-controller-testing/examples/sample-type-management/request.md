# Example Request — SampleTypeManagement Controller

## User Request

> Write integration tests for `SampleTypeManagementRestController`. The main
> regression to cover: updating a sample type to a description already used by
> another type used to return a blank 500 — it should return 409 Conflict.

## What This Example Demonstrates

- `JdbcTemplate` seeding with relational rows (localization + localization_value
  - type_of_sample), with a matching `@After` cleanup.
- `MockHttpSession` with `UserSessionData` for auth.
- A happy-path PUT that asserts the persisted state via a subsequent GET.
- A conflict regression test asserting 409 + `$.success=false` + `$.message`
  exists — not the original 500.

## Key Decisions

- IDs in the 95601–95602 range, well above seed data and below the 99000 block
  reserved for test-only data.
- `typeOfSampleService.clearCache()` called after seeding because the service
  caches sample types in memory.
- No DBUnit XML needed — JdbcTemplate is sufficient and keeps the fixture
  co-located with the test logic.
