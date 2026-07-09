# Research: 310 Turn Around Time

**Date**: 2026-04-02 **Feature**: Turn Around Time (TAT) Reporting

---

## Existing TAT Infrastructure

### Decision: Build new TAT reporting alongside existing 96-hour widget

- **Rationale**: The homepage 96-hour delayed TAT widget
  (`src/.../common/rest/provider/PatientDashBoardProvider.java:152-168`,
  `frontend/src/components/home/Dashboard.tsx:305`) uses a simple threshold
  check (startedDate to releasedDate > 96 hours). The new TAT module needs full
  segment-based calculations with Working Time support — fundamentally different
  architecture. The existing widget remains unchanged (FR-TAT-025).
- **Alternatives considered**: Extending the existing widget — rejected because
  the widget is a simple count tile, not a reporting surface.

## Timestamp Field Availability

### Decision: Use existing Sample and Analysis timestamp fields

- **Fields confirmed in codebase**:
  - `sample.entered_date` (Date) — Order Created
  - `sample.collection_date` (Timestamp) — Specimen Collected
  - `sample.received_date` (DB column) / `receivedTimestamp` (Java field) —
    Specimen Received
  - `analysis.started_date` (Date) — Testing Started (may have incomplete
    coverage)
  - `analysis.completed_date` (Date) — Result Entered
  - `analysis.released_date` (Date) — Validated/Released
- **Rationale**: All 7 TAT segments map to existing fields. No schema changes
  needed for timestamps.
- **Risk**: `analysis.started_date` may not be consistently populated. UI will
  show data completeness indicator.

## Calendar/Holiday Management

### Decision: Create new `public_holiday` and `weekend_config` tables

- **Rationale**: No existing calendar/holiday/weekend configuration found in
  codebase. This is net-new functionality.
- **Schema**: clinlims schema, Liquibase changesets, sequences following
  existing conventions (`reflex_rule.xml` pattern).

## Frontend Architecture

### Decision: Use Carbon Design System components with existing project patterns

- **Admin page pattern**: Follow `OrganizationManagement.js` — Carbon DataTable,
  useIntl(), getFromOpenElisServer(), SideNav menu items.
- **Report page pattern**: New top-level route under Reports, following existing
  report routing in App.js.
- **Charting**: Carbon Charts (`@carbon/charts-react`) for histograms and trend
  lines — native Carbon integration, no Recharts despite mockup reference.
- **Alternatives considered**: Recharts (used in mockups) — rejected because
  Carbon Charts provides native Carbon theme integration.

## Playwright E2E Testing

### Decision: Use Playwright `core-demo-video` project pattern for user story validation

- **Rationale**: Existing video infrastructure supports `PLAYWRIGHT_VIDEO=on`
  with slowMo for demo recordings. Tests go in `playwright/tests/demo/core/`
  bucket.
- **Pattern**: Use `test.step()` for organized workflows,
  `showTitleCard()`/`videoPause()` helpers for presentation, page objects for
  reusable locators.
- **Anti-patterns to avoid**: No `{ force: true }` on Carbon inputs (click
  labels), no `response.ok()` as pass/fail, no `page.waitForTimeout()`.

## Backend Architecture

### Decision: Follow 5-layer pattern with existing base classes

- **Controller**: Extend `BaseRestController`,
  `@RestController @RequestMapping("/rest")`
- **Service**: Extend `AuditableBaseObjectServiceImpl`, `@Service`,
  `@Transactional` on methods
- **DAO**: Extend `BaseDAO` interface
- **Valueholder**: Extend `BaseObject` or `EnumValueItemImpl`, JPA annotations
- **Liquibase**: `src/main/resources/liquibase/2.8.x.x/` directory, XML format,
  clinlims schema

## Permission Model

### Decision: Use existing OpenELIS permission module system

- **Rationale**: Existing permission check uses
  `ModuleAuthenticationInterceptor` and role-based SecureRoute on frontend. New
  permissions added to the existing `system_module` and `system_role_module`
  tables via Liquibase.
- **Calendar permissions**: Module-based (`calendar-management` module, admin
  role required)
- **TAT Report permissions**: Module-based (`tat-report` module, reports role
  required)

## PDF Export

### Decision: Server-side PDF generation using JasperReports

- **Rationale**: OpenELIS already uses JasperReports for existing PDF reports.
  Consistent approach.
- **Alternatives considered**: Client-side PDF (jsPDF) — rejected for chart
  rendering consistency.
