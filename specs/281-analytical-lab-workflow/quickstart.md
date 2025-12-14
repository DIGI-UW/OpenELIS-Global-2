# Quickstart: Analytical Laboratory Workflow

1. **Confirm environment**

- Java 21, Maven 3.8+, Node 16+, Docker running.
- Set feature context: `export SPECIFY_FEATURE=281-analytical-lab-workflow`.

2. **Generate artifacts**

- Plan/research/data model/contracts generated in
  `specs/281-analytical-lab-workflow/` (this folder).
- Keep Liquibase changes under `src/main/resources/liquibase/analytical/` with
  rollbacks.

3. **Backend implementation order**

- Add entities under `org.openelisglobal.analytical.valueholder` (Assignment,
  Result, DissolutionResult, Report, RetentionSample) with UUID `fhir_uuid`
  where applicable.
- Create DAOs + services with @Transactional; controllers remain thin. Use
  notebook/page services from OGC-51 for per-page state.
- Add Liquibase changesets (tables + enums + indexes).
- Implement CSV import service reusing bulk import utilities; support mapping
  preview + row-level errors.
- Wire retention transfer to `SampleStorageService` for storage
  assignments/movements.
- Add security permissions and audit logging for
  assignments/results/reports/retention.

4. **Frontend implementation order**

- Pages under `frontend/src/pages/analytical/`: Reception, Assignment, Analysis,
  Reporting, Retention.
- Components under `frontend/src/components/analytical/`: grids, assignment
  dialogs, result entry/import modal, report review, retention transfer form.
- Hooks/services in `frontend/src/services/analytical/` using SWR; map to
  contracts.
- All UI with Carbon components; strings via React Intl (add keys to
  `languages/en.json` and `fr.json`).

5. **Testing**

- Backend: JUnit 4 + Mockito for services; BaseWebContextSensitiveTest for
  controllers/DAO; include ORM validation for new entities.
- Frontend: Jest + RTL for page logic and CSV mapping flows.
- Cypress: targeted E2E per notebook flow
  (reception→assignment→analysis→reporting→retention) with fixture loader.

6. **Build/format**

- Format: `mvn spotless:apply` and `cd frontend && npm run format && cd ..`.
- Build (skip tests during dev):
  `mvn clean install -DskipTests -Dmaven.test.skip=true`.

7. **Agent context refresh**

- After updating plan, run
  `.specify/scripts/bash/update-agent-context.sh cursor-agent` to propagate
  stack info.
