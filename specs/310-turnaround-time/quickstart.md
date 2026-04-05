# Quickstart: 310 Turn Around Time

**Branch**: `310-turnaround-time` **Start Milestone**: M1 (Calendar Management
Backend)

---

## Prerequisites

1. OpenELIS-Global-2 running locally (Docker or native)
2. Database accessible with clinlims schema
3. Frontend dev server running (`cd frontend && npm start`)
4. Playwright installed (`cd frontend && npm run pw:install`)

## Development Order

```
M0 (Timestamp Fix) → M1 (Calendar Backend) → M2 (Calendar Frontend + E2E)
                                            ↘
                                             M3 (TAT Backend) → M4 (TAT Summary Frontend + E2E) → M5 (TAT Detail/Trends/Export + E2E)
```

M0 is a small prerequisite (fix Analysis HBM mapping for timestamp precision).
M2 and M3 can be developed in parallel after M1.

## Starting M0

1. Create milestone branch:

   ```bash
   git checkout develop
   git checkout -b fix/310-OGC-310-turnaround-time-m0-timestamp-precision
   ```

2. Fix HBM mapping: `src/main/resources/hibernate/hbm/Analysis.hbm.xml` (change
   `type="java.sql.Date"` to `type="java.sql.Timestamp"` for `startedDate`,
   `completedDate`, `releasedDate`)

3. Update `Analysis.java` field types to `java.sql.Timestamp`

4. See tasks.md M0 section for full list of caller site updates

## Starting M1

1. Create milestone branch:

   ```bash
   git checkout 310-turnaround-time
   git checkout -b feat/310-OGC-306-turnaround-time-m1-calendar-backend
   ```

2. Create Liquibase changeset:

   ```bash
   # Create file: src/main/resources/liquibase/2.8.x.x/public_holiday.xml
   # Follow reflex_rule.xml pattern for table creation
   ```

3. Create Valueholder classes:

   ```bash
   # PublicHoliday.java in org.openelisglobal.calendar.valueholder
   # WeekendConfig.java in org.openelisglobal.calendar.valueholder
   ```

4. Implement DAO → Service → Controller (see data-model.md and contracts/)

5. Run tests:

   ```bash
   # Unit tests
   mvn test -pl . -Dtest="*CalendarService*"

   # Integration tests
   mvn verify -pl . -Dit.test="*CalendarController*"
   ```

## Running Playwright E2E (M2+)

```bash
cd frontend

# Run specific test file
npm run pw:test -- --project=core-demo -g "calendar"

# Run with video recording for demo
npm run pw:test:core-demo-video -- -g "calendar"

# View report
npm run pw:show-report
```

## Key Files to Reference

| What                | Where                                                                  |
| ------------------- | ---------------------------------------------------------------------- |
| Spec                | `specs/310-turnaround-time/spec.md`                                    |
| Data Model          | `specs/310-turnaround-time/data-model.md`                              |
| API Contracts       | `specs/310-turnaround-time/contracts/api-contracts.md`                 |
| Plan                | `specs/310-turnaround-time/plan.md`                                    |
| Admin page pattern  | `frontend/src/components/admin/OrganizationManagement/`                |
| Controller pattern  | `src/.../organization/controller/rest/OrganizationRestController.java` |
| Liquibase pattern   | `src/main/resources/liquibase/2.8.x.x/reflex_rule.xml`                 |
| Playwright patterns | `frontend/playwright/tests/demo/core/ogc-62-shipment-workflow.spec.ts` |
| PW helpers          | `frontend/playwright/helpers/`                                         |
| PW best practices   | `../../.specify/guides/playwright-best-practices.md`                   |
