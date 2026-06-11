# OGC-553 — S-07: Environmental Compliance Dashboard & PDF Export

> **Jira:** [OGC-553](https://uwdigi.atlassian.net/browse/OGC-553)  
> **Branch:** `feature/OGC-553-environmental-dashboard`  
> **Depends on:** S-01 (OGC-528), S-02 (OGC-531), S-03 (OGC-537), S-05 v2.0 (OGC-547 — Done)

---

## Visual Reference (Screenshots)

All 6 mockup screenshots are in `xscreenshots/` at the repo root. They show the
**S-07b PDF export variant** which is the target design for this ticket.

| File | What it shows |
| ---- | ------------- |
| `Screenshot 2026-06-10 at 14.35.42.png` | Interactive dashboard — compliance trend chart, site drill-down, site comparison bar chart |
| `Screenshot 2026-06-10 at 14.35.50.png` | PDF Layout Preview — Cover page (lab name, site, period, standard, prepared-by, generated timestamp) |
| `Screenshot 2026-06-10 at 14.35.54.png` | PDF Page 2 — Compliance Rate Trend: all sites as line chart over time |
| `Screenshot 2026-06-10 at 14.36.05.png` | PDF Page 3 — Site Drill-Down: per-parameter raw value trend lines for one site |
| `Screenshot 2026-06-10 at 14.36.10.png` | PDF Page 4 — Site Comparison: horizontal bar chart, all sites, current period |
| `Screenshot 2026-06-10 at 14.36.15.png` | PDF Page 5 — Exceedance Summary table: Date, Lab #, Parameter, Result, Threshold, Status |

**Key design decisions confirmed by screenshots:**
- Charting library: `@carbon/charts-react` (already in `package.json`) — NOT recharts
- PDF: client-side via existing `jsPDF` + `jspdf-autotable` (follow `ShipmentReport.jsx` pattern)
- Site comparison bars are a single color in the screenshot — **spec requires color-coding** (green ≥90%, yellow 70–89%, red <70%), implement as specified in the ticket not the screenshot
- CSV export is **out of scope** — PDF only

---

## Data Model Notes (read before implementing)

- **Compliance standard on a sample** — stored in `observation_history` with
  type name `envComplianceStandards`. Written by S-03 order entry in
  `SamplePatientUpdateData.java:745`.
- **Sampling site on a sample** — stored in `observation_history` with type
  name `envSamplingSiteId`. Written by S-03 in `SamplePatientUpdateData.java:711`.
  Do NOT use `vecCollectionSiteId` — that is vector surveillance only.
- **Compliance result** — `result.normal_flag`: `N` = Normal (PASS), `A` =
  Abnormal (MARGINAL/counts as passing), `C` = Critical (FAIL). No separate
  ComplianceEvaluation entity exists (S-05 v2.0 dropped it).
- **Descriptive parameters** — `ComplianceThreshold.thresholdType =
  DESCRIPTIVE` — exclude from drill-down chart with explanatory note.

---

## Phase 1 — Backend Dashboard Service & DAO

- [ ] **T001** Create `ComplianceDashboardQueryService` interface + impl  
  `src/main/java/org/openelisglobal/compliance/service/ComplianceDashboardQueryService.java`  
  `src/main/java/org/openelisglobal/compliance/service/ComplianceDashboardQueryServiceImpl.java`

  All methods `@Transactional(readOnly = true)`. Methods:
  - `getSummary(List<String> siteIds, String standardId, LocalDate start, LocalDate end)` → `DashboardSummaryDTO`
  - `getTrend(List<String> siteIds, String standardId, LocalDate start, LocalDate end)` → `DashboardTrendDTO`
  - `getSiteParameters(String siteId, String standardId, LocalDate start, LocalDate end)` → `SiteParameterTrendDTO`
  - `getSiteComparison(String standardId, LocalDate start, LocalDate end)` → `List<SiteComparisonDTO>`
  - `getExceedances(List<String> siteIds, String standardId, LocalDate start, LocalDate end, int page, int size, String sortBy, String sortDir)` → `PagedExceedanceDTO`

  Core JOIN pattern for all queries:
  ```sql
  FROM clinlims.result r
  JOIN clinlims.analysis a ON r.analysis_id = a.id
  JOIN clinlims.sample_item si ON a.sampitem_id = si.id
  JOIN clinlims.sample s ON si.samp_id = s.id
  JOIN clinlims.observation_history oh_site
    ON oh_site.sample_id = s.id
    AND oh_site.observation_history_type_id = (
      SELECT id FROM clinlims.observation_history_type WHERE type_name = 'envSamplingSiteId')
  JOIN clinlims.vector_sampling_site vss ON vss.id::text = oh_site.value
  LEFT JOIN clinlims.observation_history oh_std
    ON oh_std.sample_id = s.id
    AND oh_std.observation_history_type_id = (
      SELECT id FROM clinlims.observation_history_type WHERE type_name = 'envComplianceStandards')
  WHERE s.collection_date BETWEEN :start AND :end
  ```

  Compliance rate formula: `(COUNT(*) FILTER (WHERE r.normal_flag IN ('N','A'))) * 100.0 / COUNT(*)`  
  MARGINAL (Abnormal `A`) counts as passing per spec.  
  `lowData = true` when fewer than 3 orders in a month/site bucket.

- [ ] **T002** Create dashboard DTOs  
  `src/main/java/org/openelisglobal/compliance/controller/rest/dto/`

  - `DashboardSummaryDTO` — `totalOrders`, `complianceRate`, `totalExceedances`, `sitesMonitored`, nested `TrendDTO` (delta vs prior equivalent period)
  - `DashboardTrendDTO` — `months: List<String>`, `series: List<SiteSeriesDTO>`
  - `SiteSeriesDTO` — `siteId`, `siteName`, `siteCode`, `dataPoints: List<MonthDataPointDTO>`
  - `MonthDataPointDTO` — `month`, `complianceRate`, `totalResults`, `lowData`
  - `SiteParameterTrendDTO` — `siteId`, `siteName`, `parameters: List<ParameterSeriesDTO>`
  - `ParameterSeriesDTO` — `parameterCode`, `displayName`, `units`, `threshold`, `thresholdType`, `isDescriptive`, `dataPoints: List<ParameterDataPointDTO>`
  - `ParameterDataPointDTO` — `month`, `avgValue`, `maxValue`, `exceedanceCount`
  - `SiteComparisonDTO` — `siteId`, `siteName`, `complianceRate`, `totalOrders`, `exceedances`, `colorBand` (enum: `GREEN`, `YELLOW`, `RED`)
  - `PagedExceedanceDTO` — `totalCount`, `page`, `pageSize`, `items: List<ExceedanceItemDTO>`
  - `ExceedanceItemDTO` — `date`, `labNumber`, `siteId`, `siteName`, `parameter`, `result`, `threshold`, `status` (`FAIL` | `MARGINAL`)

- [ ] **T003** Create `ComplianceDashboardRestController`  
  `src/main/java/org/openelisglobal/compliance/controller/rest/ComplianceDashboardRestController.java`  
  Base path: `/rest/compliance/dashboard`

  ```
  GET /summary
      ?siteIds=SITE-017,SITE-042  (optional, omit = all)
      ?standardId=3               (optional, omit = all active)
      ?startDate=2025-05-01
      ?endDate=2026-05-01

  GET /trend
      ?siteIds=  ?standardId=  ?startDate=  ?endDate=

  GET /sites/{siteId}/parameters
      ?standardId=  ?startDate=  ?endDate=

  GET /sites/comparison
      ?standardId=  ?startDate=  ?endDate=

  GET /exceedances
      ?siteIds=  ?standardId=  ?startDate=  ?endDate=
      ?page=0  ?size=20
      ?sortBy=date  ?sortDir=desc   (sortBy: date|parameter|site|status)
  ```

  All endpoints: `@PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")`  
  Default date range: `endDate = today`, `startDate = today - 12 months`  
  Parse `siteIds` param as comma-separated string → `List<String>`

---

## Phase 2 — Frontend Dashboard Page

- [ ] **T004** Create dashboard component  
  `frontend/src/components/compliance/EnvironmentalDashboard.jsx`

  Layout (reference `xscreenshots/Screenshot 2026-06-10 at 14.35.42.png`):
  - Filter bar at top: DateRangePicker (start/end), MultiSelect sites, Select standard, Export button with OverflowMenu (PDF only — CSV removed per scope decision)
  - KPI cards row: 4 × Carbon `Tile` — Total Orders, Compliance Rate %, Total Exceedances, Sites Monitored — each with trend delta indicator (↑↓ vs prior period)
  - Compliance Rate Trend: `@carbon/charts-react` `LineChart` — one line per site, monthly X-axis, % Y-axis, dotted line for sites with `lowData` months
  - Site Drill-Down: shown when a line is clicked — `LineChart` with raw values on Y-axis, threshold reference line as `ReferenceLine`, excludes DESCRIPTIVE parameters with note
  - Site Comparison: `@carbon/charts-react` `BarChart` (horizontal) — bars color-coded by `colorBand` (green/yellow/red), sorted worst-first
  - Exceedance Table: Carbon `DataTable` — columns: Date, Lab #, Site, Parameter, Result, Threshold, Status — paginated, sortable, status cells colored (red = FAIL, orange = MARGINAL)

  State:
  - `filters`: `{ siteIds, standardId, startDate, endDate }`
  - `selectedSite`: string (for drill-down), null = no drill-down
  - `summary`, `trend`, `siteParams`, `comparison`, `exceedances` — fetched independently

  Fetch all 5 endpoints on filter change. Show Carbon `InlineLoading` per section.

- [ ] **T005** Add i18n keys to `frontend/src/languages/en.json`

  ```json
  "compliance.dashboard.title": "Environmental Compliance Dashboard",
  "compliance.dashboard.kpi.totalOrders": "Total Orders",
  "compliance.dashboard.kpi.complianceRate": "Compliance Rate",
  "compliance.dashboard.kpi.exceedances": "Total Exceedances",
  "compliance.dashboard.kpi.sitesMonitored": "Sites Monitored",
  "compliance.dashboard.chart.trend.title": "Compliance Rate Trend — All Sites",
  "compliance.dashboard.chart.drilldown.title": "Site Drill-Down — Per-Parameter Trend",
  "compliance.dashboard.chart.comparison.title": "Site Comparison — Current Period Compliance %",
  "compliance.dashboard.table.exceedances.title": "Exceedance Summary",
  "compliance.dashboard.table.col.date": "Date",
  "compliance.dashboard.table.col.labNumber": "Lab #",
  "compliance.dashboard.table.col.site": "Site",
  "compliance.dashboard.table.col.parameter": "Parameter",
  "compliance.dashboard.table.col.result": "Result",
  "compliance.dashboard.table.col.threshold": "Threshold",
  "compliance.dashboard.table.col.status": "Status",
  "compliance.dashboard.filter.sites": "Sampling Sites",
  "compliance.dashboard.filter.standard": "Compliance Standard",
  "compliance.dashboard.filter.dateRange": "Date Range",
  "compliance.dashboard.export.pdf": "Export PDF",
  "compliance.dashboard.lowData": "Low data (< 3 orders)",
  "compliance.dashboard.descriptive.excluded": "Descriptive parameters excluded from trend chart",
  "compliance.dashboard.empty": "No data matches the selected filters"
  ```

- [ ] **T006** Register route and nav entry  
  `frontend/src/App.jsx` — add `<SecureRoute>` for `/EnvironmentalDashboard`  
  `frontend/src/components/layout/` — add nav link under the environmental/compliance section  
  Role guard: `Roles.RESULTS` (inherited by Validation)

---

## Phase 3 — PDF Export (client-side)

Reference implementation: `frontend/src/components/shipment/ShipmentReport.jsx`  
and `frontend/src/components/shipment/utils/pdfGenerator.js`

- [ ] **T007** Create PDF generator utility  
  `frontend/src/components/compliance/utils/compliancePdfGenerator.js`

  Exports one function: `generateCompliancePdf(data, meta)` where:
  - `data` = `{ summary, trend, siteParams, comparison, exceedances }`
  - `meta` = `{ labName, siteName, period, standard, preparedBy, generatedAt }`

  5-page A4 portrait layout (reference all 6 screenshots):

  **Page 1 — Cover** (`xscreenshots/Screenshot 2026-06-10 at 14.35.50.png`):
  - Lab name centered, bold, large
  - Title: "Environmental Compliance Dashboard"
  - Site, Period, Standard, Prepared by, Generated fields
  - Footer: lab name left, "Page 1 of 5" center, "Generated by OpenELIS Global" right

  **Page 2 — Compliance Trend** (`xscreenshots/Screenshot 2026-06-10 at 14.35.54.png`):
  - Title: "Compliance Rate Trend — All Sites"
  - Capture Carbon LineChart as PNG via `chart.ref.current.querySelector('svg')` → canvas → `toDataURL()`
  - Add as `jsPDF.addImage()`

  **Page 3 — Site Drill-Down** (`xscreenshots/Screenshot 2026-06-10 at 14.36.05.png`):
  - Title: "Site Drill-Down — {siteName}: Per-Parameter Trend"
  - Same chart-capture pattern; if no site selected, use first site in comparison list

  **Page 4 — Site Comparison** (`xscreenshots/Screenshot 2026-06-10 at 14.36.10.png`):
  - Title: "Site Comparison — {period} Compliance %"
  - Capture horizontal BarChart as PNG

  **Page 5 — Exceedance Summary** (`xscreenshots/Screenshot 2026-06-10 at 14.36.15.png`):
  - Title: "Exceedance Summary"
  - Use `jspdf-autotable` — columns: Date, Lab #, Parameter, Result, Threshold, Status
  - Status cells: FAIL in red, MARGINAL in orange (use `didParseCell` hook)
  - If exceedances span multiple pages, autotable handles pagination automatically

  Chart capture helper (same pattern as `html2canvas` in shipment):
  ```js
  import html2canvas from 'html2canvas'; // check if available, else use SVG serialiser
  const canvas = await html2canvas(chartRef.current);
  const imgData = canvas.toDataURL('image/png');
  doc.addImage(imgData, 'PNG', margin, y, width, height);
  ```

  `meta.labName` source: read from `/rest/site-information?name=siteName` (existing endpoint).  
  `meta.preparedBy` source: `userSessionDetails.firstName + ' ' + userSessionDetails.lastName`.

- [ ] **T008** Wire export button in `EnvironmentalDashboard.jsx`

  The Export PDF button in the filter bar calls `generateCompliancePdf(...)` with
  the current filter results and meta. Show Carbon `InlineLoading` on the button
  while generating. The PDF filename: `compliance-dashboard-{startDate}-{endDate}.pdf`.

---

## Phase 4 — Hardening

- [ ] **T009** Add composite DB index for dashboard query performance  
  `src/main/resources/liquibase/3.5.x.x/034-compliance-dashboard-indexes.xml`

  ```xml
  <createIndex tableName="observation_history" indexName="idx_obs_hist_type_sample">
      <column name="observation_history_type_id"/>
      <column name="sample_id"/>
  </createIndex>
  ```

  Register in `src/main/resources/liquibase/3.5.x.x/base.xml`

- [ ] **T010** Write unit tests for `ComplianceDashboardQueryServiceImpl`  
  `src/test/java/org/openelisglobal/compliance/service/ComplianceDashboardQueryServiceTest.java`

  Test cases:
  - Empty result set returns zeros (not NPE)
  - `lowData` flag set when site has < 3 orders in a month
  - `colorBand` GREEN / YELLOW / RED boundaries (exactly 90%, exactly 70%)
  - MARGINAL (`A`) counted as passing in compliance rate
  - Trend delta sign correct (positive when current > prior)
  - Exceedance sort by date descending
  - Prior period calculation: `startDate - (endDate - startDate)` → `startDate`

- [ ] **T011** Write Vitest unit tests for PDF generator  
  `frontend/src/components/compliance/utils/compliancePdfGenerator.test.js`

  Test cases:
  - `generateCompliancePdf` called with empty exceedances produces 5-page doc
  - Cover page contains lab name and period
  - Exceedance table rows match input data count
  - FAIL status cell uses red fill

---

## Files to Create / Modify

| File | Action |
| ---- | ------ |
| `compliance/service/ComplianceDashboardQueryService.java` | New |
| `compliance/service/ComplianceDashboardQueryServiceImpl.java` | New |
| `compliance/controller/rest/dto/*.java` (9 DTOs) | New |
| `compliance/controller/rest/ComplianceDashboardRestController.java` | New |
| `liquibase/3.5.x.x/034-compliance-dashboard-indexes.xml` | New |
| `liquibase/3.5.x.x/base.xml` | Modified — add index changeset |
| `frontend/src/components/compliance/EnvironmentalDashboard.jsx` | New |
| `frontend/src/components/compliance/utils/compliancePdfGenerator.js` | New |
| `frontend/src/languages/en.json` | Modified — 23 new keys |
| `frontend/src/App.jsx` | Modified — new SecureRoute |
| `frontend/src/components/layout/` (nav file) | Modified — new nav link |
| `src/test/java/.../ComplianceDashboardQueryServiceTest.java` | New |
| `frontend/src/components/compliance/utils/compliancePdfGenerator.test.js` | New |
