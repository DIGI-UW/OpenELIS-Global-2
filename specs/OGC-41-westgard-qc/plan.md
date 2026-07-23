# Implementation Plan: Westgard QC Rules Dashboard

**Branch**: `feat/qc_westgard_rules` | **Date**: 2026-04-13 | **Spec**:
[spec.md](spec.md) **Jira**:
[OGC-41](https://uwdigi.atlassian.net/browse/OGC-41) | **Design**:
[westgard-rules.md](https://github.com/DIGI-UW/openelis-work/blob/main/designs/quality/westgard-rules.md)

## Summary

Implement a Westgard-rules-based quality control system for laboratory analyzer
instruments, covering automated QC sample identification, statistical evaluation
of 8 standard rules, a real-time compliance dashboard with Levey-Jennings
charts, and configurable alerting. The full scope spans the design spec's
FR1-FR13 across 5 milestones, with M1 delivering the MVP already implemented in
PR #3390 + Bridge #33.

## Technical Context

**Language/Version**: Java 21 LTS (Spring MVC 6.2) + React 17 (JavaScript)
**Primary Dependencies**: Spring Framework, Hibernate/JPA, Carbon React, Carbon
Charts **Storage**: PostgreSQL 14+ via JPA/Hibernate, Liquibase 4.8.0
**Testing**: JUnit 4 + Mockito (backend), Jest (frontend), Playwright (E2E)
**Target Platform**: Docker-composed web application (OE + bridge + mock)
**Performance Goals**: Dashboard loads in <3s, rule evaluation <2s per result,
chart renders <2s for 100 data points **Constraints**: Evaluation must not block
analyzer ingestion pipeline (async); all QC data immutable for audit
**Scale/Scope**: 100+ instruments, 1000 QC results/day, 2-year data retention

## Constitution Check

- [x] **Configuration-Driven**: No country-specific code branches; QC rules come
      from analyzer profiles (per-instrument, not per-country)
- [x] **Carbon Design System**: All QC UI uses @carbon/react (dashboard tiles,
      charts via Carbon Charts, forms, tables)
- [x] **FHIR/IHE Compliance**: QC observations tagged via FHIR R4 meta.tag;
      bridge→OE communication uses FHIR Bundles
- [x] **Layered Architecture**: 5-layer pattern followed — QCControlLot
      (Valueholder) → QCControlLotDAO → QCControlLotService → QCRestController →
      QCControlLotForm. @Transactional in services only.
- [x] **Test Coverage**: ~285 backend+bridge tests exist (~233 QC module + ~26
      analyzer-QC + ~26 bridge-QC @Test methods); frontend 0 tests and E2E 0
      tests are the M1 completion gap
- [x] **Schema Management**: Liquibase for all tables; runtime metadata (QC
      rules) comes from profiles, not seed data
- [x] **Internationalization**: All QC UI strings use React Intl; ~923 new
      en.json keys added vs develop (QC dashboard, charts, rule config, control
      lot setup, alerts, per-analyzer QC rules)
- [x] **Security & Compliance**: RBAC (GLOBAL_ADMIN + LAB_SUPERVISOR),
      sys_user_id audit trail on all entities, violations immutable

## Milestone Plan

### Current State

**Implementation**: PR #3390 (OE) + Bridge #33 implement all M1 backend, bridge,
and frontend code. The code is stacked on the Madagascar FILE analyzer PR #3372,
builds pass in CI, and the local harness runs with 10 seeded analyzers and
profile-driven QC rules (all 7 FILE profiles and 6 ASTM profiles verified
populated).

**Remaining before M1 is deploy-ready**: (1) end-to-end flow validation (M1.1 —
create lot → mock QC → violation → dashboard), (2) REST controller integration
tests (M1.2), (3) one Playwright smoke test (M1.3), (4) cleanup + CI green
(M1.4). These are test-coverage and validation gaps, not new feature work.

### Milestone Table

| ID         | Branch Suffix         | Scope                                                                                                        | User Stories   | Verification                                                                                                         | Depends On |
| ---------- | --------------------- | ------------------------------------------------------------------------------------------------------------ | -------------- | -------------------------------------------------------------------------------------------------------------------- | ---------- |
| **M1**     | m1-mvp                | QC pipeline + dashboard + charts + alerts + rule config + bridge QC identification                           | US1-7 (all)    | Backend tests pass, 1 Playwright smoke test, controller tests, local harness flow validated                          | -          |
| **M2**     | m2-corrective-actions | Corrective action workflow: entity, service, UI (recalibration, maintenance, repeat control, reagent change) | FR7            | Corrective action CRUD + link to violations; violation cannot close without corrective action for REJECTION severity | M1         |
| **[P] M3** | m3-email-alerts       | Email notification transport + per-user notification preferences                                             | FR11.2-11.7    | Email sent on violation; user can configure which severities trigger email                                           | M1         |
| **[P] M4** | m4-trend-reporting    | Trend analysis charts + reporting (PDF/CSV export) + violation history log                                   | FR10, FR12     | Trend graph renders; PDF export works; violation log filterable                                                      | M1         |
| **M5**     | m5-advanced-charts    | Chart zoom/pan, multi-level subplots, manual re-evaluation, preview mode                                     | FR5, FR9.6-9.8 | Chart interactions work; re-evaluation produces results without persisting                                           | M2, M3, M4 |

### Milestone Dependency Graph

```mermaid
graph LR
    M1[M1: MVP Release] --> M2[M2: Corrective Actions]
    M1 --> M3["[P] M3: Email Alerts"]
    M1 --> M4["[P] M4: Trend + Reporting"]
    M2 --> M5[M5: Advanced Charts + Re-eval]
    M3 --> M5
    M4 --> M5
```

### PR Strategy

- **M1**: lands as the current stacked PR #3390 →
  `fix/madagascar-accession-results-file-e2e` → `develop`. Includes spec + all
  implementation + test completion work.
- **M2-M5**: each as a separate PR from `feat/OGC-41-westgard-qc-m{N}-{desc}` →
  `develop`, opened after M1 merges.

---

## M1: MVP Release — Completion Plan

M1 is 95% implemented. The remaining work is test coverage and validation:

### M1.1 End-to-end flow validation (local harness)

**Goal**: Prove the full pipeline works: create lot → send mock QC result →
z-score computed → Westgard evaluation → violation created → dashboard shows it.

**Steps**:

1. Create a control lot for HIV Viral Load on QuantStudio 5 (manufacturer-
   fixed, mean=1000, SD=50)
2. Use the mock server to generate a QC file with a specimen ID "CNEG001" and
   result value 1180 (z=3.6, should trigger 1-3s REJECTION)
3. Drop the file into the QuantStudio watched directory
4. Verify: QCResult created with z-score ~3.6
5. Verify: QCRuleViolation created for 1-3s rule
6. Verify: QCAlert created for active users
7. Verify: Dashboard shows QuantStudio 5 in RED (out of compliance)
8. Verify: Levey-Jennings chart shows the violated point highlighted

### M1.2 Playwright QC smoke test

**Goal**: One E2E test proving the QC dashboard route + API + UI renders in CI.

**Test outline**:

- Authenticate as admin
- Navigate to `/analyzers/qc/db`
- Assert: QCSummaryTiles visible with numeric counts
- Assert: InstrumentsTab renders at least one instrument card
- Click an instrument card
- Assert: navigated to `/analyzers/qc/instruments/:id`
- Assert: breadcrumb trail visible
- Navigate to `/analyzers/qc/control-lots`
- Assert: list page renders

### M1.3 REST controller tests

**Goal**: Verify Spring wiring for QC endpoints (QCRestController,
QCChartDataRestController, QCViolationRestController).

**Tests**:

- GET `/rest/qc/dashboard/summary` → 200 + correct shape
- GET `/rest/qc/dashboard/instruments` → 200 + array
- GET `/rest/qc/control-lots` → 200 + array
- POST `/rest/qc/controlLot` → 201 + lot created
- GET `/rest/qc/charts/{lotId}` → 200 + chart data shape
- GET `/rest/qc/violations` → 200 + array
- POST `/rest/qc/violations/{id}/acknowledge` → 200

### M1.4 Cleanup

- Remove unused PropTypes import from QcRuleBuilderModal.jsx
- Remove the `003-westgard-qc` branch (superseded by OGC-41 naming)
- Verify CI fully green (Build+Test, Static, Frontend, E2E)

---

## M2: Corrective Actions (est. 1-2 weeks)

**Design ref**:
[FR7 — westgard-rules.md](https://github.com/DIGI-UW/openelis-work/blob/main/designs/quality/westgard-rules.md)

**New entities**:

- `QCCorrectiveAction`: action type (RECALIBRATION, MAINTENANCE, REPEAT_CONTROL,
  REAGENT_CHANGE, OTHER), assigned user, status (PENDING/IN_PROGRESS/COMPLETED),
  resolution notes

**Backend**:

- `QCCorrectiveActionService` + DAO + REST controller
- Link violations to corrective actions (FK on `qc_rule_violation`)
- Auto-resolve violation when corrective action completed
- Block patient result release for associated samples until resolved (FR7.7)

**Frontend**:

- Corrective action form (inline within violation detail, not modal)
- Status tracking in AlertsTab (PENDING → IN_PROGRESS → COMPLETED)
- Assignment dropdown (active system users)

---

## M3: Email Alerts (est. 1 week, parallel with M2)

**Design ref**:
[FR11 — westgard-rules.md](https://github.com/DIGI-UW/openelis-work/blob/main/designs/quality/westgard-rules.md)

**Backend**:

- Wire `QCAlertService` to the existing email infrastructure (Spring Mail)
- Email template with: instrument, test, rule violated, z-score, link to detail
- Per-user notification preferences (entity + admin UI)
- Respect 15-minute batching for WARNING severity

**Frontend**:

- User preferences page (which severities trigger email)
- Admin config for SMTP settings (if not already global)

---

## M4: Trend Analysis + Reporting (est. 2 weeks, parallel with M2)

**Design ref**:
[FR10, FR12 — westgard-rules.md](https://github.com/DIGI-UW/openelis-work/blob/main/designs/quality/westgard-rules.md)

**Backend**:

- `QCTrendService`: compliance percentage over time, violation frequency by rule
  type, instruments with recurring violations
- Report generation service (PDF via iText or similar, CSV via streaming)
- Filterable violation history log endpoint

**Frontend**:

- Trend graphs (Carbon Charts line/bar) with date range + instrument + test
  filters
- Export buttons (PDF, CSV) on dashboard and chart pages
- Violation history page with sorting, pagination, filtering

---

## M5: Advanced Charts + Manual Re-evaluation (est. 1-2 weeks)

**Design ref**:
[FR5, FR9.6-9.8 — westgard-rules.md](https://github.com/DIGI-UW/openelis-work/blob/main/designs/quality/westgard-rules.md)

**Backend**:

- `POST /rest/qc/evaluate-range`: on-demand rule evaluation for a date range
- Preview mode: return results without persisting violations
- Re-evaluation after statistics recalculation

**Frontend**:

- Chart zoom and pan (Carbon Charts supports this natively; wire it up)
- Multi-level subplots (Low/Normal/High tabs or separate chart rows)
- Print/export chart to PDF/PNG
- Manual evaluation controls (date range picker, "Evaluate" button, preview
  toggle)

---

## Project Structure

### Documentation

```text
specs/OGC-41-westgard-qc/
├── spec.md              # Feature specification (this feature)
├── plan.md              # This implementation plan
├── checklists/
│   └── requirements.md  # Spec quality checklist
├── research.md          # (to be created if needed)
├── data-model.md        # (to be created for M2+)
└── contracts/           # (to be created for M2+)
```

### Source Code

```text
# Backend (QC module)
src/main/java/org/openelisglobal/qc/
├── controller/          # QCRestController, QCChartDataRestController, QCViolationRestController
├── dao/                 # QCControlLotDAO, QCResultDAO, WestgardRuleConfigDAO, etc.
├── service/             # QCResultService, WestgardRuleEvaluationService, QCAlertService, etc.
├── evaluator/westgard/  # 8 rule evaluators (Spring @Component auto-discovered)
├── listener/            # QCResultCreatedEventListener (@Async, AFTER_COMMIT)
├── form/                # QCControlLotForm, WestgardRuleConfigForm
└── valueholder/         # QCControlLot, QCResult, QCStatistics, QCRuleViolation, QCAlert

# Backend (analyzer QC rules)
src/main/java/org/openelisglobal/analyzer/
├── controller/          # AnalyzerQcRuleRestController
├── dao/                 # AnalyzerQcRuleDAO
├── service/             # AnalyzerQcRuleService, QcRuleDto
└── valueholder/         # AnalyzerQcRule

# Frontend (QC components)
frontend/src/components/qc/
├── dashboard/           # QCDashboard, QCSummaryTiles, InstrumentsTab, AlertsTab, InstrumentDetailPage
├── charts/              # LeveyJenningsChart, ControlChartDetail
├── controlLots/         # ControlLotList, ControlLotSetup, StatisticsConfigModal
├── ruleConfig/          # RuleConfigPanel, RuleConfigFormModal
└── index.js             # Module exports

# Frontend (analyzer QC rules page)
frontend/src/components/analyzers/QcRules/
├── QcRuleBuilderModal.jsx  # Now a routed page at /analyzers/:id/qc-rules
└── QcRuleRow.jsx

# Analyzer profiles (QC rules source of truth)
projects/analyzer-profiles/
├── astm/                # 6 ASTM profiles with FIELD_EQUALS O.12=Q
└── file/                # 7 FILE profiles with instrument-specific QC rules

# Bridge (QC rule evaluation engine)
tools/openelis-analyzer-bridge/src/main/java/org/itech/ahb/
├── qc/                  # QcRule, QcRuleEvaluator
├── fhir/                # ASTMResultParser, FileResultParser, HL7ResultParser (rule-driven isControl)
└── startup/             # AnalyzerRegistryBootstrap (pulls qcRules from OE)

# Liquibase — QC changesets split across two directories
src/main/resources/liquibase/
├── analyzer/            # 004-012-create-analyzer-qc-rule.xml (table)
│                        # 004-013-seed-default-qc-rules.xml (no-op; rules come from profiles via validCheckSum ANY)
└── qc/                  # 001-create-qc-tables.xml (control lot, result, statistics)
                         # 002-create-westgard-rule-config.xml
                         # 003-create-qc-violation-tables.xml
                         # (no 004 — numbering reserved; 004-series lives in analyzer/)
                         # 005-create-qc-alert.xml
                         # 006-fix-lastupdated-column.xml
                         # 007-add-qc-menu-items.xml
                         # 008-add-instrument-fk-constraints.xml
```

## Testing Strategy

**Reference**: [OpenELIS Testing Roadmap](.specify/guides/testing-roadmap.md)
and [Playwright best practices](.specify/guides/playwright-best-practices.md)

**Note**: This project has deprecated Cypress E2E (per CLAUDE.md). All new E2E
tests use **Playwright** with the harness-foundational / harness-demo project
structure. Use `/plan-record-playwright`, `/write-playwright-test`,
`/audit-playwright`, `/debug-playwright` skills for E2E work.

### Coverage Goals

- **Backend**: >80% code coverage for QC module (~285 unit+integration+DAO tests
  across OE and bridge exist; controller tests are the gap)
- **Frontend**: >70% coverage target (currently 0%; M1 adds smoke test, M2+ adds
  component tests)
- **Critical Paths**: 100% coverage for z-score calculation, rule evaluation
  logic, and violation creation

### Test Types

- [x] **Unit Tests**: 233 tests covering services, evaluators, calculators,
      event listener (JUnit 4 + Mockito)
- [x] **DAO Tests**: 7 integration tests for QCControlLotDAO
- [ ] **Controller Tests**: QCRestController, QCChartDataRestController,
      QCViolationRestController — M1 completion target
- [x] **ORM Validation Tests**: QCHibernateMappingValidationTest (2 tests)
- [x] **Bridge Tests**: 56 tests across 4 files (parser QC rules + evaluator)
- [ ] **Frontend Unit Tests**: 0 — M2+ target
- [ ] **E2E Tests (Playwright)**: 0 — M1 adds 1 smoke test; M2+ adds workflow
      tests

### Test Data Management

- **Backend unit**: Test builders (QCControlLotBuilder, QCResultBuilder, etc.)
  for consistent fixture generation
- **Backend integration**: DBUnit XML fixtures
  (`src/test/resources/testdata/ qc-*.xml`) with transaction rollback
- **E2E**: `seed-analyzers.sh` creates 10 analyzers with profile-driven QC
  rules; manual control lot creation via REST API in test setup

### Checkpoint Validations

- [x] **After Phase 1 (Entities)**: ORM validation tests pass ✓
- [x] **After Phase 2 (Services)**: 233 backend unit tests pass ✓
- [ ] **After Phase 3 (Controllers)**: Controller integration tests pass (M1
      completion)
- [ ] **After Phase 4 (Frontend)**: Playwright smoke test pass (M1 completion)
