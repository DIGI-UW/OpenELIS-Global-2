# AHRI SRS Persona RBAC — Smoke Results

**Environment:** `https://localhost:8843`
**Date:** 2026-05-23
**Password (local smoke only):** `adminADMIN!`
**User seed:** `./scripts/populate-notebook-users.sh --clean-install`

## Users created (department + persona)

Each lab user has **one** `lab_unit_role_map` with **one** SRS persona role (not the old multi-role `mntd` account).

| Username | User ID | Department (test_section) | Lab persona (SRS) | Global role |
|----------|---------|---------------------------|-------------------|-------------|
| `mntd_collector` | 1100 | MNTD (177) | Sample Collector | — |
| `mntd_technician` | 1101 | MNTD (177) | Laboratory Technician | — |
| `mntd_researcher` | 1102 | MNTD (177) | Junior Researcher | — |
| `mntd_manager` | 1103 | MNTD (177) | Lab Manager | — |
| `mntd_biomedical` | 1104 | MNTD (177) | Biomedical Staff | — |
| `biorepo_collector` | 1105 | Biorepository (182) | Sample Collector | — |
| `biorepo_technician` | 1106 | Biorepository (182) | Laboratory Technician | — |
| `biorepo_researcher` | 1107 | Biorepository (182) | Junior Researcher | — |
| `biorepo_manager` | 1108 | Biorepository (182) | Lab Manager | — |
| `global_admin` | 1109 | — | — | Global Administrator |
| `system_admin` | 1110 | — | — | System Admin |
| `admin_staff` | 1111 | — | — | Administrative Staff |
| `eqa_user` | 1112 | — | — | EQA Personnel |

**Smoke notebooks:** MNTD entry `9` (notebook `34`, `workflow_type=mntd`), Biorepository entry `1` (notebook `16`, `workflow_type=biorepository`).

**Page IDs (bulk ACL):** MNTD intake `1795`, analysis `1803`; Biorepo intake `182`, reporting `188`.

---

## Automated checks

| Check | Result | Notes |
|-------|--------|-------|
| `populate-notebook-users.sh --clean-install` | **PASS** | 13 persona users; `System Admin` role seeded if missing |
| `mvn test` `NotebookStageAccessServiceTest`, `NotebookStageAccessRbacMatrixTest` | **PASS** | Unit/matrix proof of persona × stage rules |
| `npm run check:workflow-registry` | **PASS** | CSV ↔ JS drift |
| Cypress `login.cy.js` (admin) | **PASS** | 8/8 @ `:8843` |
| Cypress `ahriSrsPersonaRbacApi.cy.js` | **BLOCKED** | See below |
| Cypress `ahriSrsPersonaRbac.cy.js` (browser stage nav) | **PARTIAL** | Login OK; Workflow tab + `PageNavigation` needs stable entry load |

### API smoke (`cypress/e2e/ahriSrsPersonaRbacApi.cy.js`)

Uses `POST /api/OpenELIS-Global/rest/notebook/bulk/page/{pageId}/samples/apply` (same **EDIT** stage ACL as complete).

| Persona | Active lab unit | Action tested | Expected | Observed (latest run) |
|---------|-----------------|---------------|----------|------------------------|
| `mntd_collector` | 177 | EDIT analysis page 1803 | **403** | 404 before WAR redeploy; re-test after `mvn package` + Tomcat WAR copy |
| `mntd_collector` | 182 | EDIT biorepo intake 182 (cross-dept) | **403** | Same |
| `mntd_technician` | 177 | EDIT analysis | **403** | Same |
| `mntd_technician` | 177 | EDIT intake | 200/400 | Same |
| `mntd_researcher` | 177 | EDIT intake | **403** | Same |
| `mntd_manager` | 177 | EDIT analysis | 200/400 | Same |
| `mntd_biomedical` | 177 | EDIT intake | **403** | Same |
| `biorepo_collector` | 182 | EDIT reporting 188 | **403** | Same |
| `biorepo_manager` | 182 | EDIT reporting | 200/400 | Same |
| `global_admin` | 177 | EDIT intake | 200/400 | Same |
| `system_admin` | 177 | EDIT intake | **403** per SRS | App may allow via `hasUnrestrictedDepartmentAccess` — **product gap** |
| `admin_staff` | 177 | EDIT intake | **403** | Same |
| `eqa_user` | 177 | EDIT intake | **403** | Same |

**Deploy note:** Prebuilt Docker image did not expose `/rest/notebook/bulk/*` (404). After `mvn package` and `docker cp target/OpenELIS-Global.war` into `openelisglobal-webapp`, re-run:

```bash
./scripts/populate-notebook-users.sh --clean-install
cd frontend && CYPRESS_baseUrl=https://localhost:8843 npx cypress run --spec cypress/e2e/ahriSrsPersonaRbacApi.cy.js
```

---

## Browser UI smoke (manual / Cypress `ahriSrsPersonaRbac.cy.js`)

| Persona | Active lab unit | UI expectation | Browser tested | Pass/Fail |
|---------|-----------------|----------------|----------------|-----------|
| Sample Collector | Own dept | Intake stage enabled; analysis/reporting disabled | Cypress: login OK; Workflow tab navigation flaky | **PARTIAL** |
| Laboratory Technician | Own dept | Processing enabled; analysis complete disabled | API spec | Pending WAR |
| Junior Researcher | Own dept | No intake; mid-workflow enabled | API spec | Pending WAR |
| Lab Manager | Own dept | QC/analysis enabled | API spec | Pending WAR |
| Biomedical Staff | Own dept | Notebook stages disabled | API spec | Pending WAR |
| Global Admin | Chooses dept on `/landing` | All stages (unrestricted) | Manual: `/landing` + `/admin/userEdit` shows **All Lab Units** | **PASS** (admin UI) |
| System Admin | — | Admin menus; not unrestricted scientific without Global Admin | API | See SRS gap above |
| EQA / Admin Staff | — | No notebook EDIT | API | Pending WAR |

**All Lab Units:** Confirmed only in **Admin → User Management** (`UserAddModify.js`), not in storage/inventory department pickers (filtered via `filterOwningDepartments`).

---

## How to re-run

```bash
./scripts/populate-notebook-users.sh --clean-install
cd frontend
CYPRESS_baseUrl=https://localhost:8843 npm run cy:run -- --spec cypress/e2e/ahriSrsPersonaRbacApi.cy.js
CYPRESS_baseUrl=https://localhost:8843 npm run cy:run -- --spec cypress/e2e/ahriSrsPersonaRbac.cy.js
```
