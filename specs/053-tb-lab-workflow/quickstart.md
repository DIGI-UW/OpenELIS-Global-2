# Quickstart - TB Lab Workflow

## Environment
- Ensure Java 21 is active (`sdk env`); Node 16+ installed.
- From repo root: `mvn clean install -DskipTests -Dmaven.test.skip=true` for fast sanity (builds WAR, skips tests); `cd frontend && npm install && cd ..`.
- Start dev stack when needed: `docker compose -f dev.docker-compose.yml up -d`.

## Build & Implement
1. **Schema (Liquibase)**: Add changeLog under `src/main/resources/liquibase/tb/` for new TB tables, TB sample ID yearly sequence, unique indexes, and rollbacks.
2. **Backend packages (`org.openelisglobal.tb`)**:
   - Create valueholders for TbSampleRegistration/QC/CultureReading/Smear/Identification/GeneXpert/Dst/IsolateStorage extending BaseObject.
   - DAOs extend `BaseDAOImpl`; Services annotated `@Service @Transactional` handle TB ID generation, QC propagation, MDR flag compute, culture status compilation.
   - REST controllers under `/rest/tb/...` map forms/DTOs; no `@Transactional` in controllers; services return fully compiled DTOs to avoid lazy loads.
3. **Reuse Notebook infrastructure**: Leverage NotebookEntry/NotebookPageSample for per-page tracking; reuse bulk grid + SampleStorageService for storage UI lookups.
4. **Frontend**:
   - Create Carbon pages under `frontend/src/pages/notebook/tb/` with components in `components/notebook/tb/` (culture grid, QC checklist, DST panel).
   - Add SWR clients in `frontend/src/services/tb/`; all strings via React Intl (`languages/en.json`, `fr.json`).
   - Implement label/printing hooks using existing notebook label patterns; add REDCap export UI (CSV/Excel) referencing backend export endpoint.
5. **Reporting/Export**: Compile result page pulling all Tb* entities; expose CSV/Excel export and exported_at tracking.

## Testing Checklist
- Backend: `mvn -Dtest=Tb*Test test` for unit/ORM validation; add integration tests extending `BaseWebContextSensitiveTest` for REST flows.
- Frontend: `npm test -- TB` (Jest/RTL); ensure component tests cover validation and critical workflows.
- E2E: `npm run cy:run -- --spec "cypress/e2e/tb-workflow.cy.js"` (one file at a time during dev).
- Fast build: `mvn clean install -DskipTests -Dmaven.test.skip=true` + `npm run format` before commit.

## Data/Fixtures
- Use existing fixture loader when adding TB sample test data: `./src/test/resources/load-test-fixtures.sh --reset`.
- Prefer API-based setup for Cypress; avoid UI-based data seeding.
