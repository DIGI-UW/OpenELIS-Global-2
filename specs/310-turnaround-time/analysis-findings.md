# Pre-Implementation Analysis Findings: 310 Turn Around Time

**Date**: 2026-04-02 **Purpose**: Cross-validation of spec/plan/tasks against
Jira tickets, requirement docs, JSX mockups, and actual codebase.

---

## Critical Findings (Must Fix Before Implementation)

### CF-1: Analysis timestamp fields are DATE, not TIMESTAMP

**Source**: `Analysis.java` + `Analysis.hbm.xml`

The spec assumes hour-level precision for TAT calculations, but three of the six
milestone timestamps are `java.sql.Date` (date-only, no time-of-day):

| Field                      | Java Type            | DB Column         | Has Time? |
| -------------------------- | -------------------- | ----------------- | --------- |
| `sample.enteredDate`       | `java.sql.Date`      | `ENTERED_DATE`    | No        |
| `sample.collectionDate`    | `java.sql.Timestamp` | `COLLECTION_DATE` | Yes       |
| `sample.receivedTimestamp` | `java.sql.Timestamp` | `RECEIVED_DATE`   | Yes       |
| `analysis.startedDate`     | `java.sql.Date`      | `STARTED_DATE`    | No        |
| `analysis.completedDate`   | `java.sql.Date`      | `COMPLETED_DATE`  | No        |
| `analysis.releasedDate`    | `java.sql.Date`      | `RELEASED_DATE`   | No        |

**Impact**: Segments using Analysis fields (3-7) will have date-only precision.
TAT values will be calculated as full-day differences (0h, 24h, 48h, etc.)
rather than "3h 42m". Only segments 1-2 (using Sample timestamps) will have true
hour-level precision.

**Resolution**: **Option B (revised — low risk)**. The DB columns are already
`TIMESTAMP WITHOUT TIME ZONE` — no schema migration needed. The fix is purely at
the Hibernate mapping layer:

1. Change `Analysis.hbm.xml` type from `java.sql.Date` to `java.sql.Timestamp`
2. Change `Analysis.java` field types to `java.sql.Timestamp`
3. Update ~20 caller sites to use `Timestamp` instead of `Date`
4. Fix `PatientDashBoardProvider` TAT calculation to use actual time

This is now **Milestone M0** — a prerequisite for all other milestones. After
M0, all 7 TAT segments have hour-level precision. Historical data shows midnight
(accurate for how it was stored); future data has full time.

### CF-2: BaseObject.sysUserId is @Transient — NOT persisted by JPA

**Source**: `BaseObject.java` — `sysUserId` field is annotated `@Transient`

New JPA entities (PublicHoliday, WeekendConfig) that extend `BaseObject` will
NOT have `sys_user_id` persisted to the database unless the subclass explicitly
adds `@Column(name = "sys_user_id")` to override the `@Transient`.

**Action**: Updated task T006/T007 guidance — must add `@Column` annotation on
`sysUserId` in both new entities.

### CF-3: sys_user_id is VARCHAR(36), not INTEGER FK

**Source**: All recent entities (EQA, Storage, Shipment) use `VARCHAR(36)` for
`sys_user_id`, not an integer FK. The value is a string user ID from
`getSysUserId(request)`.

**Action**: Updated data-model.md to correct column type.

### CF-4: system_module_url table required for URL-based access control

**Source**: Existing permission patterns (e.g.,
`eqa-007-add-eqa-menu-items.xml`, `shipment-007-add-menu-and-role.xml`)

Permission registration requires THREE inserts: `system_module` +
`system_module_url` + `system_role_module`. The tasks only mentioned
`system_module` and `system_role_module`.

**Action**: Updated task T004 to include `system_module_url` entries.

### CF-5: Missing test data seeding for Playwright E2E

**Source**: Existing `e2e-foundational-data.sql` contains only user/org/provider
fixtures — NO sample or analysis records.

TAT report E2E tests need sample/analysis records with timestamps to produce
meaningful results. Calendar management tests need holidays in the database.

**Action**: Added explicit test data seeding task to M2 and M4/M5.

### CF-6: Module name convention is PascalCase

**Source**: Existing modules use PascalCase: `EQAView`,
`SampleShipmentManagement`

The spec used kebab-case (`calendar-management`, `tat-report`). Updated to
PascalCase: `CalendarManagement`, `TATReport`.

---

## High-Priority Findings (Fix in Spec/Tasks)

### HF-1: Histogram bin algorithm undefined

The requirements doc wireframe shows specific non-uniform bins (0-1h, 1-2h,
2-3h, 3-4h, 4-6h, 6-8h, 8-12h, 12-24h, 24-48h, 48h+). The spec says
"auto-calculated bins." These are different approaches.

**Resolution**: Use the requirements doc's fixed non-uniform bins as the default
algorithm. Add as implementation note.

### HF-2: Working Time partial-day calculation needs worked example

The spec says "24-hour working day" but doesn't clarify partial days (start or
end on a working day mid-day). The requirements doc provides a concrete example.

**Resolution**: Added worked example to spec assumptions.

### HF-3: CSV export must use decimal hours

The requirements doc specifies TAT values in CSV as decimal hours (e.g., `3.7`)
for spreadsheet compatibility. The spec doesn't specify format.

**Resolution**: Added to FR-TAT-020.

### HF-4: Trend chart default metric lines

Requirements doc: Median + 90th Percentile on by default, Mean toggleable. Not
specified in spec.

**Resolution**: Added to FR-TAT-015.

### HF-5: Liquibase include registration

New changeset files in `3.5.x.x/` must be registered via `<include>` in
`3.5.x.x/base.xml`. Task T005 covers this but the individual file tasks
(T002-T004) should reference it.

### HF-6: JasperReports PDF complexity understated

Existing PDF reports use a file-based template factory (`IReportCreator`). There
is no `.jrxml` template for TAT. Implementation must either create a template or
use programmatic PDF generation (iText is also on the classpath).

**Resolution**: Documented as implementation decision in research.md.

---

## Medium-Priority Findings — ALL INTEGRATED INTO TASKS

| ID    | Finding                                                             | Integrated Into                                   | Status |
| ----- | ------------------------------------------------------------------- | ------------------------------------------------- | ------ |
| MF-1  | Missing "Clear Filters" button                                      | T053 (i18n), T054 (Jest), T058 (component)        | Done   |
| MF-2  | Missing loading states for Calendar page, Detail List, Trends       | T021 (Jest), T023, T057, T077, T078               | Done   |
| MF-3  | Missing error state handling (API failures)                         | T020 (i18n), T021 (Jest), T023, T053 (i18n), T057 | Done   |
| MF-4  | Active filter summary badges below tabs                             | T057 (TATReport component)                        | Done   |
| MF-5  | Calendar footer count "{N} holidays configured"                     | T020 (i18n), T021 (Jest), T023 (component)        | Done   |
| MF-6  | Secondary sort (Lab Number ascending) for detail list               | T077 (TATDetailListTab)                           | Done   |
| MF-7  | Calculation mode: use ContentSwitcher not RadioButtonGroup          | T054 (Jest), T058 (component)                     | Done   |
| MF-8  | Metric line toggles: use Checkbox not Carbon Toggle                 | T078 (TATTrendsTab)                               | Done   |
| MF-9  | Histogram color grading (teal -> yellow -> orange by bin)           | T060 (TATHistogram)                               | Done   |
| MF-10 | Stat card Median highlight (teal background)                        | T059 (TATStatCards)                               | Done   |
| MF-11 | Breakdown max column red highlight for >24h                         | T061 (TATBreakdownTable)                          | Done   |
| MF-12 | "Click a row to view individual results" helper text                | T053 (i18n), T061 (component)                     | Done   |
| MF-13 | data-testid attributes for Playwright page objects                  | T023 (Calendar), T057 (TAT Report)                | Done   |
| MF-14 | AC-CAL-11 inactive dimming not in Jest/E2E test lists               | T021 (Jest), T029b (PW plan)                      | Done   |
| MF-15 | AC-18 include-cancelled toggle not in any E2E step                  | T054 (Jest), T065b (PW plan)                      | Done   |
| MF-16 | Chart assertions: clarify "visible" not "data values"               | T067, T085, T086 (PW tests)                       | Done   |
| MF-17 | File download assertions: verify initiation not content             | T087, T088 (PW tests)                             | Done   |
| MF-18 | Modal vs inline: requirements doc shows modal, spec chose inline    | T023 (explicit note)                              | Done   |
| MF-19 | `analysis.enteredDate` DB column is `ENTRY_DATE` not `entered_date` | data-model.md already documents                   | Done   |
| MF-20 | Both Sample and Analysis have `releasedDate` fields                 | data-model.md WARNING section                     | Done   |

**CF-5 (test data seeding)** also integrated: T029a (calendar E2E seed), T065a
(TAT report E2E seed).

---

## Coverage Summary

### OGC-306 Calendar Management: 11/12 ACs fully covered, 1 partial

- **Partial**: AC-CAL-11 (inactive dimming visual) — needs explicit Jest test
  case and E2E step

### OGC-307 TAT Report: 19/22 ACs fully covered, 3 partial

- **Partial**: AC-18 (include-cancelled toggle) — needs E2E step
- **Partial**: AC-21 (permissions) — backend-only coverage, no E2E
- **Partial**: AC-22 (10K+ performance) — architectural coverage, no E2E perf
  test
