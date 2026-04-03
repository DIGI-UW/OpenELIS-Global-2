# Feature Specification: Turn Around Time (TAT) Reporting

**Feature Branch**: `310-turnaround-time` **Created**: 2026-04-02 **Status**:
Draft **Epic**: [OGC-310](https://uwdigi.atlassian.net/browse/OGC-310) — Turn
Around Time **Tickets**: [OGC-306](https://uwdigi.atlassian.net/browse/OGC-306)
(Calendar Management), [OGC-307](https://uwdigi.atlassian.net/browse/OGC-307)
(TAT Report Page) **Input**: User description: "Spec for first two tickets of
OGC-310 epic — Calendar Management Admin Page (OGC-306) and TAT Report Page
(OGC-307)"

---

## User Scenarios & Testing _(mandatory)_

### User Story 1 — Manage Public Holidays and Weekend Configuration (Priority: P1)

As a **lab administrator**, I want to manage public holidays and configure
weekend days through an admin page, so that TAT reports can distinguish between
calendar time and working time when measuring turnaround performance.

**Why this priority**: Calendar Management is a foundational dependency — the
TAT Report's "Working Time" mode cannot function without holiday and weekend
data. This must be built and testable first.

**Independent Test**: Can be fully tested by navigating to Admin > Calendar
Management, adding/editing/deleting holidays, changing weekend days,
importing/exporting CSV, and verifying all CRUD operations work correctly —
delivers value as a standalone configuration surface even before the TAT Report
exists.

**Acceptance Scenarios**:

1. **Given** a user with `calendar.manage` permission, **When** they navigate to
   Admin > Calendar Management, **Then** the page loads showing a holiday table
   for the current year with weekend day checkboxes defaulting to Saturday and
   Sunday.

2. **Given** an empty holiday list for 2026, **When** the administrator clicks
   "Add Holiday" and enters date "2026-01-01" and name "New Year's Day" with
   recurring checked, **Then** the holiday appears in the sorted table with an
   "Annual" badge and "Active" status.

3. **Given** an existing holiday, **When** the administrator clicks Edit on that
   row, **Then** the row becomes editable inline with date picker, name input,
   and recurring checkbox, and Save is disabled until both date and name are
   filled.

4. **Given** an existing holiday, **When** the administrator clicks Delete and
   confirms, **Then** the holiday is removed from the table.

5. **Given** a recurring holiday created in 2025, **When** the administrator
   selects year 2026 or 2027 from the year filter, **Then** that holiday appears
   in the list for those years automatically.

6. **Given** a holiday that falls on a configured weekend day, **When** viewing
   the holiday table, **Then** the day-of-week shows an orange "(weekend)"
   indicator.

7. **Given** a user changes weekend days from Saturday/Sunday to
   Friday/Saturday, **When** they toggle the checkboxes, **Then** the change
   saves immediately with a toast confirmation.

8. **Given** a valid CSV file with holiday data, **When** the administrator
   clicks Import CSV and selects the file, **Then** a preview table appears
   showing parsed holidays before the import is applied.

9. **Given** holidays exist for 2026, **When** the administrator clicks Export
   CSV, **Then** a CSV file downloads containing date, name, recurring, and
   active columns.

10. **Given** a user without `calendar.manage` permission, **When** they access
    the Calendar Management page, **Then** they see the holiday table and
    weekend config in read-only mode — no Add, Edit, Delete, Import, or Export
    buttons.

---

### User Story 2 — Generate TAT Summary Report (Priority: P1)

As a **lab manager**, I want to generate a TAT summary report for a selected
workflow segment with aggregate statistics, a distribution histogram, and a
breakdown table, so I can identify turnaround bottlenecks and monitor laboratory
performance.

**Why this priority**: The Summary tab is the primary landing experience of the
TAT Report and provides the highest-value analytics (mean, median, 90th
percentile, distribution). It is the most commonly used view for lab
accreditation compliance and quality improvement.

**Independent Test**: Can be fully tested by navigating to Reports > Turn Around
Time, applying date range and segment filters, and verifying summary stat cards,
histogram, and breakdown table render with correct calculations — delivers
actionable TAT insights as a standalone view.

**Acceptance Scenarios**:

1. **Given** a user with report access permission, **When** they navigate to
   Reports > Turn Around Time, **Then** the page loads with filters defaulting
   to last 30 days, "Receipt to Validation" segment, and Calendar Time mode.

2. **Given** sample data with known TAT values, **When** the report is generated
   for "Receipt to Validation" over the last 30 days, **Then** 7 stat cards
   display: Total Orders, Mean TAT, Median TAT, 90th Percentile, Min, Max, and
   Std Deviation — all with correct calculations shown in hours and minutes
   format (e.g., "3h 42m").

3. **Given** the summary is displayed, **When** viewing the distribution
   histogram, **Then** bars show TAT value distribution with auto-calculated
   bins, and dashed vertical lines mark the median and 90th percentile
   positions.

4. **Given** the summary is displayed, **When** viewing the breakdown table with
   dimension set to "Lab Unit", **Then** each lab unit row shows count, mean,
   median, 90th percentile, and max TAT values.

5. **Given** the breakdown table is displayed, **When** the user clicks a lab
   unit row (e.g., "Hematology"), **Then** the view navigates to the Detail List
   tab pre-filtered to that lab unit.

6. **Given** the user selects a different TAT segment from the dropdown,
   **When** any of the 7 segments is selected, **Then** all summary stats,
   histogram, and breakdown recalculate for the new segment.

7. **Given** the user toggles calculation mode to "Working Time", **When** the
   toggle takes effect, **Then** all TAT values recalculate excluding weekend
   and holiday hours, and an info bar appears showing "Working Time mode:
   Excluding Saturdays, Sundays, and X public holidays in the selected date
   range."

8. **Given** no holidays are configured in Calendar Management, **When** the
   user selects "Working Time" mode, **Then** a warning appears: "No public
   holidays configured. Configure holidays in Admin > Calendar Management" with
   a link.

---

### User Story 3 — View Detailed TAT Results (Priority: P2)

As a **lab manager or quality officer**, I want to view a detailed, sortable,
paginated list of individual orders/results with all TAT milestone timestamps,
so I can investigate specific delays and identify problematic orders.

**Why this priority**: The Detail List complements the Summary by enabling
drill-down investigation. It is essential for identifying specific problematic
orders, but the Summary tab alone already provides actionable aggregate
insights.

**Independent Test**: Can be tested by switching to the Detail List tab after
generating a report, verifying pagination, sorting, column visibility toggling,
and that clicking a lab number links to the order view.

**Acceptance Scenarios**:

1. **Given** a generated TAT report, **When** the user clicks the "Detail List"
   tab, **Then** a sortable paginated table appears showing lab number, test,
   lab unit, priority, all milestone timestamps, selected segment TAT, and
   overall TAT.

2. **Given** the detail list is displayed, **When** the user clicks the "Lab
   Number" column for an order, **Then** it opens that order's view page in a
   new tab.

3. **Given** orders with STAT priority in the results, **When** viewing the
   detail list, **Then** STAT rows display a red left border indicator.

4. **Given** an order with a missing milestone timestamp (e.g., no collection
   date), **When** viewing that row, **Then** the missing timestamp shows "—"
   and the corresponding segment TAT shows "N/A".

5. **Given** more than 25 results, **When** the detail list loads, **Then**
   server-side pagination controls appear with configurable rows per page (25,
   50, 100).

6. **Given** the detail list, **When** the user clicks the "Columns" dropdown,
   **Then** they can toggle optional columns (Patient, Sample Type, Ordering
   Site, Testing Started, Result Entered) on/off, and selections persist for the
   session.

7. **Given** the detail list, **When** the user clicks any column header,
   **Then** the table sorts by that column (ascending/descending toggle), with
   default sort being Selected Segment TAT descending.

---

### User Story 4 — Analyze TAT Trends Over Time (Priority: P2)

As a **lab manager**, I want to view TAT trend charts over time with
daily/weekly/monthly aggregation and multi-series comparison, so I can track
whether turnaround performance is improving or degrading.

**Why this priority**: Trends provide temporal context that the Summary snapshot
cannot. Critical for demonstrating improvement to accreditation bodies, but the
Summary and Detail List already provide the core reporting value.

**Independent Test**: Can be tested by switching to the Trends tab, selecting
different aggregation intervals, toggling metric lines, enabling multi-series
comparison by lab unit or priority, and verifying the chart renders correctly.

**Acceptance Scenarios**:

1. **Given** a generated TAT report with a date range > 7 days, **When** the
   user clicks the "Trends" tab, **Then** a time series chart appears showing
   median and 90th percentile lines for the selected segment over the date
   range.

2. **Given** the trend chart is displayed, **When** the user changes aggregation
   from Daily to Weekly or Monthly, **Then** the chart redraws with data points
   at the selected interval.

3. **Given** the trend chart, **When** the user enables "Compare by: Lab Unit",
   **Then** the chart displays separate color-coded lines for each lab unit.

4. **Given** the trend chart, **When** the user toggles "Show order volume",
   **Then** a light gray bar chart overlay appears behind the trend lines
   showing the count of orders per period.

---

### User Story 5 — Export TAT Report Data (Priority: P3)

As a **lab manager**, I want to export TAT report data as CSV or PDF, so I can
share findings with stakeholders, include data in accreditation submissions, or
perform further analysis in spreadsheet tools.

**Why this priority**: Export is a supporting capability. The report delivers
value through the on-screen views first; export enables offline sharing and
compliance documentation.

**Independent Test**: Can be tested by generating a report and clicking Export >
CSV or Export > PDF, then verifying the downloaded file contains correct data.

**Acceptance Scenarios**:

1. **Given** a generated TAT report, **When** the user clicks Export > CSV,
   **Then** a CSV file downloads containing all detail rows (up to 100,000) with
   all TAT segments, both Calendar and Working Time values, and raw ISO 8601
   timestamps.

2. **Given** a generated TAT report, **When** the user clicks Export > PDF,
   **Then** a PDF file downloads containing the summary stats, histogram,
   breakdown table, trend chart (if applicable), and up to 1,000 detail rows.

---

### Edge Cases

- What happens when no orders match the selected filters? The system displays an
  empty state message: "No results found for the selected filters. Try adjusting
  the date range or filter criteria."
- What happens when a TAT calculation results in 0 hours (start and end on same
  excluded day in Working Time mode)? Display "0h 0m" — this is a valid result.
- What happens if all milestone timestamps for a segment are missing across all
  results? Show "Insufficient data" in stat cards instead of zeros or NaN.
- What happens if a CSV import contains duplicate dates? Show validation error
  for those rows, import remaining valid rows, report count of imported vs.
  skipped.
- What happens if a user deletes a holiday that was used in a previously
  generated Working Time report? The deletion applies prospectively — previously
  calculated reports are not retroactively changed, but future report generation
  will reflect the updated holiday list.
- What happens with very large date ranges (e.g., 1 year with 50,000+ results)?
  Server-side pagination and aggregation ensure browser performance is
  unaffected. Summary statistics and histograms are computed server-side. Detail
  list uses lazy loading with server-side pagination.

---

## Requirements _(mandatory)_

### Functional Requirements

#### Calendar Management (OGC-306)

- **FR-CM-001**: System MUST provide a Calendar Management page accessible via
  Admin > Calendar Management menu.
- **FR-CM-002**: System MUST display a table of public holidays for the selected
  year, sorted by date, with columns: Date (with day-of-week), Holiday Name,
  Recurring (Annual/One-time badge), Status (Active/Inactive), and Actions
  (Edit/Delete).
- **FR-CM-003**: System MUST support inline add — clicking "Add Holiday" inserts
  an editable row at the top of the table with date picker, name input (max 100
  characters), and recurring checkbox.
- **FR-CM-004**: System MUST support inline edit — clicking Edit on a row swaps
  it to the editable format in-place.
- **FR-CM-005**: System MUST disable the Save button until both date and name
  are provided; other row actions MUST be disabled while any row is being
  edited.
- **FR-CM-006**: System MUST prevent duplicate holiday dates within the same
  year. When a one-time holiday conflicts with a recurring holiday occurrence in
  a target year, the system MUST reject the one-time entry with an error message
  identifying the conflict. Duplicate detection is enforced at the service layer
  (not a DB UNIQUE constraint) to account for recurring expansion.
- **FR-CM-007**: System MUST show a confirmation dialog before deleting a
  holiday.
- **FR-CM-008**: System MUST support recurring holidays — holidays marked as
  recurring carry forward to all subsequent years on the same month/day.
- **FR-CM-009**: System MUST support marking holidays as Active or Inactive;
  inactive holidays MUST be visually dimmed and excluded from TAT Working Time
  calculations.
- **FR-CM-010**: System MUST display an orange "(weekend)" indicator next to
  holidays that fall on a configured weekend day.
- **FR-CM-011**: System MUST provide a year filter dropdown, defaulting to the
  current year.
- **FR-CM-012**: System MUST provide weekend day configuration as a row of
  checkboxes for Monday through Sunday, with Saturday and Sunday checked by
  default.
- **FR-CM-013**: Weekend configuration changes MUST save immediately with a
  toast confirmation.
- **FR-CM-014**: System MUST support CSV import with columns: date, name,
  recurring (true/false). Import MUST validate for format errors and duplicates
  and show a preview before applying.
- **FR-CM-015**: System MUST support CSV export of the current year's holidays
  with columns: date, name, recurring, active.
- **FR-CM-016**: System MUST enforce write access via the `CalendarManagement`
  permission module for add, edit, delete, import, and export operations. Users
  without write access to this module see the page in read-only mode.

#### TAT Report (OGC-307)

- **FR-TAT-001**: System MUST provide a TAT Report page accessible via Reports >
  Turn Around Time menu.
- **FR-TAT-002**: System MUST support 7 TAT segments: (1) Order to Collection,
  (2) Collection to Receipt, (3) Receipt to Testing Started, (4) Receipt to
  Result Entry, (5) Receipt to Validation, (6) Result Entry to Validation, (7)
  Overall TAT (Order to Validation). Default segment: "Receipt to Validation".
- **FR-TAT-003**: System MUST support two TAT calculation modes: Calendar Time
  (default — all elapsed hours) and Working Time (excludes weekends and public
  holidays using Calendar Management data).
- **FR-TAT-004**: System MUST display TAT values in hours and minutes format
  (e.g., "3h 42m") throughout all views.
- **FR-TAT-005**: System MUST provide a filter bar with: date range (required,
  default last 30 days, with quick presets: Today, Last 7 Days, Last 30 Days,
  Last 90 Days, This Month, Last Month, This Quarter; max range 1 year), lab
  unit (multi-select), test/panel (multi-select typeahead), priority
  (All/Routine/STAT/ASAP), sample type, ordering site (typeahead), TAT segment,
  calculation mode, include cancelled/rejected toggle, and a "Generate Report"
  button to trigger report computation.
- **FR-TAT-006**: System MUST organize report content into three tabs: Summary,
  Detail List, and Trends.
- **FR-TAT-007**: Summary tab MUST display 7 stat cards: Total Results, Mean
  TAT, Median TAT, 90th Percentile, Min TAT, Max TAT, and Std Deviation. (Note:
  "Total Results" aligns with per-test segment granularity for segments 4-7; for
  per-order segments 1-3, the count reflects the number of orders.)
- **FR-TAT-008**: Summary tab MUST display a distribution histogram with
  auto-calculated bins and vertical markers for median and 90th percentile.
- **FR-TAT-009**: Summary tab MUST display a breakdown table grouped by a
  selectable dimension (Lab Unit, Test, Priority, Sample Type, Ordering Site),
  with sortable columns and drill-down to Detail List.
- **FR-TAT-010**: Detail List tab MUST display a sortable, server-side paginated
  table with all milestone timestamps and calculated TAT values.
- **FR-TAT-011**: Detail List MUST support configurable column visibility
  (toggle optional columns on/off), persisting selections for the user session.
- **FR-TAT-012**: Detail List MUST link lab numbers to the order view page.
- **FR-TAT-013**: Detail List MUST display a red left border on STAT priority
  rows.
- **FR-TAT-014**: Detail List MUST show "—" for missing timestamps and "N/A" for
  uncalculable TAT segments, excluding them from aggregations.
- **FR-TAT-015**: Trends tab MUST display a time series chart with configurable
  aggregation (Daily, Weekly, Monthly) and toggleable metric lines (Mean,
  Median, 90th Percentile). Default: Median and 90th Percentile on, Mean off.
- **FR-TAT-016**: Trends tab MUST support multi-series comparison by dimension
  (Lab Unit, Priority, Sample Type, Ordering Site).
- **FR-TAT-017**: Trends tab MUST support an optional volume overlay showing
  order count as a bar chart on a secondary y-axis.
- **FR-TAT-018**: When Working Time mode is active, system MUST display an info
  bar showing excluded day count with a link to Calendar Management.
- **FR-TAT-019**: When Working Time mode is selected and no holidays are
  configured, system MUST display a warning with a link to Calendar Management.
- **FR-TAT-020**: System MUST support CSV export (up to 100,000 rows) including
  all TAT segments and both Calendar and Working Time values. TAT values in CSV
  MUST be decimal hours (e.g., `3.7`) for spreadsheet compatibility, not
  formatted strings. Raw timestamps MUST be ISO 8601.
- **FR-TAT-021**: System MUST support PDF export including summary stats,
  histogram, breakdown table, trend chart, and up to 1,000 detail rows.
- **FR-TAT-022**: System MUST handle 10,000+ results without browser performance
  degradation through server-side computation and pagination.
- **FR-TAT-023**: System MUST respect user permissions for report page access
  and patient data visibility.
- **FR-TAT-024**: Working Time calculation MUST use 24-hour working days on
  non-excluded days (labs operate 24/7 on working days).
- **FR-TAT-025**: The existing homepage 96-hour delayed TAT widget MUST remain
  unchanged.

### Constitution Compliance Requirements (OpenELIS Global)

- **CR-001**: UI components MUST use Carbon Design System (@carbon/react) — NO
  custom CSS frameworks. The JSX mockups provided use Carbon-like styling as
  reference; actual implementation must use real Carbon components.
- **CR-002**: All UI strings MUST be internationalized via React Intl (no
  hardcoded text). New message IDs added to `en.json` only — non-English locales
  (fr, etc.) are managed via Transifex, not direct file edits. This includes all
  stat labels, filter labels, tab names, error messages, and toast
  notifications.
- **CR-003**: Backend MUST follow 5-layer architecture (Valueholder > DAO >
  Service > Controller > Form). Services own transactions (@Transactional in
  services ONLY, never controllers).
- **CR-004**: Database changes MUST use Liquibase changesets (NO direct
  DDL/DML). The `public_holiday` and `weekend_config` tables must be created via
  Liquibase.
- **CR-005**: Configuration-driven variation for country-specific requirements
  (NO code branching). Weekend day configuration and holiday lists are
  per-installation, not per-country code branches.
- **CR-006**: Security: RBAC via existing permission module, audit trail
  (sys_user_id + lastupdated on holiday records), input validation on all API
  endpoints.
- **CR-007**: Tests MUST be included (unit + integration + E2E, >70% coverage
  goal). JUnit 4 for backend, Playwright for E2E.
- **CR-008**: Features >3 days effort must be broken into Validation Milestones
  (one PR per milestone).

### Key Entities

- **Public Holiday**: Represents a designated non-working day. Key attributes:
  date, name, recurring (annual vs. one-time), active status, audit fields
  (created/modified by/date).
- **Weekend Configuration**: Represents which days of the week are considered
  weekend (non-working). Key attributes: day of week (0-6), whether it is a
  weekend day. Exactly 7 rows (one per day).
- **TAT Calculation**: A computed value representing elapsed time between two
  workflow milestones for an order/test. Not a persisted entity — calculated
  on-demand from existing timestamps on Sample and Analysis records.
- **TAT Segment**: One of 7 named intervals between workflow milestones (Order >
  Collection > Receipt > Testing Started > Result Entry > Validation).

---

## Assumptions & Constraints

### Assumptions

1. **Timestamp precision requires M0 fix**: The PostgreSQL columns for
   `analysis.started_date`, `completed_date`, and `released_date` are
   `TIMESTAMP WITHOUT TIME ZONE` (with time support), but the Hibernate HBM
   mapping (`Analysis.hbm.xml`) incorrectly maps them as `java.sql.Date`,
   causing time truncation to midnight on read/write. **Milestone M0 fixes
   this** by changing the HBM type to `java.sql.Timestamp` and updating
   `Analysis.java` field types. No schema migration needed — DB columns are
   already TIMESTAMP. After M0, all 7 TAT segments will have hour-level
   precision. Historical data will show midnight (accurate for how it was
   stored); future data will have full time. The `started_date` field may also
   have incomplete coverage — the UI will show a data completeness indicator if
   coverage is low.
2. **Working Time uses 24-hour days**: On non-excluded days, all 24 hours count
   as working time. Partial days count only actual elapsed hours on that day.
   Example: Friday 4:00 PM to Monday 9:00 AM with Sat/Sun excluded = Friday 8h +
   Monday 9h = 17h working time. If both start and end fall on excluded days,
   Working Time TAT is 0.
3. **No new FHIR resources required**: TAT reporting is an internal analytics
   feature. Calendar and TAT data are not externally-facing entities requiring
   FHIR R4 compliance (Constitution Principle III does not apply).
4. **Permissions use existing module system**: New modules
   (`CalendarManagement`, `TATReport` — PascalCase per codebase convention) will
   be added to the existing OpenELIS `system_module` / `system_module_url` /
   `system_role_module` permission system, not a new RBAC framework. All three
   tables must be populated for URL-based access control to work.
5. **Charting library**: The frontend will use a charting library compatible
   with Carbon Design System for histograms and trend charts. The JSX mockups
   reference Recharts; the actual library choice is an implementation decision.
6. **PDF generation**: PDF export will be handled server-side to ensure
   consistent formatting and chart rendering.
7. **Holiday sample data**: The requirements doc uses Rwanda holidays as
   examples (Genocide Memorial Day, Liberation Day, etc.), reflecting OpenELIS
   deployments in East Africa. The system must support any country's holidays.
8. **Maximum date range**: TAT reports are limited to a 1-year maximum date
   range for performance.

### Constraints (Constitution)

- **UI**: Carbon Design System v1.15+ exclusively (Principle II)
- **i18n**: React Intl for all strings; new keys in `en.json` only, non-English
  via Transifex (Principle VII)
- **Architecture**: 5-layer pattern mandatory (Principle IV)
- **Schema**: Liquibase for all DB changes (Principle VI)
- **Testing**: TDD workflow, JUnit 4 (Principle V)
- **Java**: Java 21 LTS, Jakarta EE 9, Spring Framework 6.2.2 (Traditional MVC)
- **Milestones**: Feature must be broken into Validation Milestones with one PR
  each (Principle IX)

---

## Success Criteria _(mandatory)_

### Measurable Outcomes

- **SC-001**: Administrators can create, edit, and delete holidays in under 30
  seconds per operation, with immediate visual feedback.
- **SC-002**: Administrators can configure weekend days in under 10 seconds with
  instant save and toast confirmation.
- **SC-003**: A CSV file with 50+ holidays can be imported with preview and
  applied in under 60 seconds.
- **SC-004**: Lab managers can generate a TAT summary report (with 1,000+
  matching results) in under 5 seconds from filter selection to full display.
- **SC-005**: All 7 TAT segments calculate correctly — verified by comparing
  manual calculations against system output for a known test dataset.
- **SC-006**: Working Time mode correctly excludes configured weekends and
  holidays from TAT calculations — verified by comparing Calendar Time and
  Working Time results for a date range containing known weekends and holidays.
- **SC-007**: Detail list handles 10,000+ results with smooth pagination (page
  load under 2 seconds) and no browser performance degradation.
- **SC-008**: CSV export of 50,000+ rows completes within 30 seconds.
- **SC-009**: PDF export generates a complete report document within 15 seconds.
- **SC-010**: 90% of lab manager users can locate and generate a TAT report on
  first attempt without training (measured by task completion in usability
  testing or demo video).
- **SC-011**: All user stories can be demonstrated via recorded video evidence
  showing end-to-end workflows.
