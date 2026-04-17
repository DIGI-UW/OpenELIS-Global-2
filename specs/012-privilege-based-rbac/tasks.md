# Tasks: Privilege-Based RBAC with Role Inheritance

**Input**: `specs/012-privilege-based-rbac/` (spec.md, data-model.md)
**Prerequisites**: spec.md ✓, data-model.md ✓

**Sequencing rule**: Phases 1–2 are strict prerequisites. Phases 3–6 map to user
stories and can be parallelized across developers once Phase 2 is complete.
Phase 7 (legacy retirement) runs last, after all stories are stable in
production.

---

## Phase 1: Database Foundation

**Purpose**: Create the schema and seed data that everything else depends on.
**⚠️ CRITICAL**: No application code changes can be tested until this is done.

- [x] T001 Write Liquibase changeset `012-001-create-system-privilege` — create
      `clinlims.system_privilege` table with id, name, description, category,
      active, lastupdated, sys_user_id columns
      `src/main/resources/liquibase/3.5.x.x/012-001-create-system-privilege.xml`

- [x] T002 Write Liquibase changeset `012-002-create-system-role-privilege` —
      create `clinlims.system_role_privilege` join table with FK to
      `system_role` and `system_privilege`, unique constraint on (role_id,
      privilege_id)
      `src/main/resources/liquibase/3.5.x.x/012-002-create-system-role-privilege.xml`

- [x] T003 Write Liquibase changeset `012-003-seed-privileges` — insert all
      privilege definitions across categories: orders, results, patients,
      reports, nce, analyzer, admin, audit, shipment (see data-model.md for full
      list) `src/main/resources/liquibase/3.5.x.x/012-003-seed-privileges.xml`

- [x] T004 Write Liquibase changeset `012-004-seed-role-privilege-mappings` —
      insert direct (non-inherited) privilege assignments for each role per the
      inheritance tree in data-model.md. Reception gets order/patient/nce
      privileges; Results gets result:enter/view/modify; Validation gets
      result:validate/nce:edit/assign; etc.
      `src/main/resources/liquibase/3.5.x.x/012-004-seed-role-privilege-mappings.xml`

- [x] T005 Write Liquibase changeset `012-005-set-role-parent-chains` — update
      `grouping_parent` on functional roles: Results→Reception,
      Validation→Results, Pathologist→Validation, Cytopathologist→Validation.
      Fix Cytopathologist spelling (`CytoPathologist` → `Cytopathologist`) in
      system_role.
      `src/main/resources/liquibase/3.5.x.x/012-005-set-role-parent-chains.xml`

- [x] T006 Write Liquibase changeset `012-006-migrate-legacy-role-assignments` —
      migrate `system_user_role` rows: `Results entry` → `Results`, `Validator`
      → `Validation`, `Results Admin` → `Results`. Migrate `lab_roles` entries
      accordingly.
      `src/main/resources/liquibase/3.5.x.x/012-006-migrate-legacy-role-assignments.xml`

- [x] T007 Write Liquibase changeset `012-007-deactivate-legacy-roles` — set
      `active=false` on `Inventory mgr`, `Results Admin`, `Results entry`,
      `Validator`. These remain in DB for historical FK integrity but are no
      longer assignable.
      `src/main/resources/liquibase/3.5.x.x/012-007-deactivate-legacy-roles.xml`

**Checkpoint**: Run `mvn liquibase:update`. Verify all changesets apply cleanly.
Query `system_privilege` (should have ~25 rows), `system_role_privilege` (should
have mappings for all 10 active roles), `system_role.grouping_parent` chain is
correct.

---

## Phase 2: Backend Privilege Entity & Resolution

**Purpose**: Java layer that resolves privileges at login. Blocks all
story-level work. **⚠️ CRITICAL**: All endpoint protection and frontend stories
depend on this.

- [x] T008 Create `Privilege` valueholder entity (JPA annotations, NOT HBM XML)
      `src/main/java/org/openelisglobal/privilege/valueholder/Privilege.java`

- [x] T009 [P] Create `RolePrivilege` valueholder entity — join between Role and
      Privilege
      `src/main/java/org/openelisglobal/privilege/valueholder/RolePrivilege.java`

- [x] T010 Create `PrivilegeDAO` interface + `PrivilegeDAOImpl`
      `src/main/java/org/openelisglobal/privilege/dao/PrivilegeDAO.java`
      `src/main/java/org/openelisglobal/privilege/daoimpl/PrivilegeDAOImpl.java`

- [x] T011 Create `PrivilegeService` interface + `PrivilegeServiceImpl` with
      methods: `getDirectPrivilegesForRole(roleId)`,
      `resolveAllPrivilegesForRole(roleId)` (walks parent chain recursively),
      `getAllPrivilegesForUser(systemUserId)` (aggregates across all assigned
      roles)
      `src/main/java/org/openelisglobal/privilege/service/PrivilegeService.java`
      `src/main/java/org/openelisglobal/privilege/service/PrivilegeServiceImpl.java`

- [x] T012 Add `Privilege` constants class — all privilege name strings as
      public static final constants. This is the single source of truth for
      privilege names used in `@PreAuthorize`.
      `src/main/java/org/openelisglobal/common/constants/Privileges.java`

- [x] T013 Update `Constants.java` — add missing role constants
      (`ROLE_ANALYSER_IMPORT`, `ROLE_CYTOPATHOLOGIST`). NOTE: `AUTH_ADMIN` /
      `AUTH_GLOBAL_ADMIN` were intentionally NOT added here; Spring authority
      string constants belong in `Privileges.java` (which defines the DB-side
      names that `CustomUserDetailsService` transforms into `PRIV_*` authority
      strings at login). `Constants.java` is for role display-name strings only.
      `src/main/java/org/openelisglobal/common/constants/Constants.java`

- [x] T014 Update `CustomUserDetailsService.loadUserByUsername()` — after
      loading role authorities, call
      `privilegeService.getAllPrivilegesForUser()` and add each resolved
      privilege as a `SimpleGrantedAuthority` with `PRIV_` prefix.
      `Global Administrator` short-circuits to all privileges.
      `src/main/java/org/openelisglobal/security/login/CustomUserDetailsService.java`

- [x] T015 Fix `ModuleAuthenticationInterceptor` auto-allow gap — REST paths are
      now denied (HTTP 401 JSON) when `hasPermission()` returns false. The
      `isRestFullPath()` call in the deny branch writes a JSON error response
      and returns `false`.
      `src/main/java/org/openelisglobal/interceptor/ModuleAuthenticationInterceptor.java`

- [x] T016 Write unit tests for
      `PrivilegeServiceImpl.resolveAllPrivilegesForRole()` — test: direct
      privileges only, single inheritance, multi-level inheritance
      (Reception→Results→Validation), Global Admin returns all, circular
      reference guard (should not infinite loop)
      `src/test/java/org/openelisglobal/privilege/service/PrivilegeServiceImplTest.java`

- [x] T017 Write unit test asserting the auto-allow gap is closed — subclass
      overrides `hasPermission()` → false, verifies REST paths receive HTTP 401
      JSON (not a redirect). Renamed to `*Test` for Maven Surefire pickup.
      `src/test/java/org/openelisglobal/interceptor/ModuleAuthenticationInterceptorTest.java`

**Checkpoint**: Unit tests green. Integration test for auto-allow denial passes.
A `Validation` user's `GrantedAuthority` set at login includes
`PRIV_RESULT_ENTER` (inherited) and `PRIV_RESULT_VALIDATE` (direct).

---

## Phase 3 (US1): Privilege enforcement at every endpoint

**Goal**: Every REST controller uses `hasAuthority('PRIV_*')` — no bare role
checks, no unguarded endpoints.

### Tests (write first — must fail before implementation)

- [x] T018 [P] Write `EndpointPrivilegeCoverageTest` — reflection-based test
      that scans all `@RestController` classes, verifies every `@GetMapping`,
      `@PostMapping`, `@PutMapping`, `@DeleteMapping` method has either a
      method-level or class-level `@PreAuthorize` with `hasAuthority`. GREEN.
      `src/test/java/org/openelisglobal/security/EndpointPrivilegeCoverageTest.java`

- [x] T019 [P] Write access-control integration tests — for each endpoint
      category (orders, results, nce, admin), asserts: (1) wrong privilege →
      403, (2) correct privilege → not 403. Renamed to `*Test` for Maven
      Surefire.
      `src/test/java/org/openelisglobal/security/EndpointAccessControlTest.java`

### Implementation

- [x] T020 Replace all `@PreAuthorize("hasRole('ADMIN')")` on admin/config
      controllers with `@PreAuthorize("hasAuthority('PRIV_ADMIN_SYSTEM')")` —
      affects ~60 controllers under `test/`, `panel/`, `method/`, `uom/`,
      `testresult/`, `dictionary/`, `sitebranding/` packages

- [x] T021 [P] Add `@PreAuthorize` to order entry endpoints — verified GREEN by
      `EndpointPrivilegeCoverageTest`

- [x] T022 [P] Add `@PreAuthorize` to result entry endpoints — verified GREEN by
      `EndpointPrivilegeCoverageTest`

- [x] T023 [P] Add `@PreAuthorize` to validation endpoints — verified GREEN by
      `EndpointPrivilegeCoverageTest`

- [x] T024 [P] Add `@PreAuthorize` to NCE endpoints — verified GREEN by
      `EndpointPrivilegeCoverageTest`

- [x] T025 [P] Add `@PreAuthorize` to patient endpoints — verified GREEN by
      `EndpointPrivilegeCoverageTest`

- [x] T026 [P] Add `@PreAuthorize` to report endpoints — verified GREEN by
      `EndpointPrivilegeCoverageTest`

- [x] T027 [P] Add `@PreAuthorize` to analyzer endpoints — verified GREEN by
      `EndpointPrivilegeCoverageTest`

- [x] T028 [P] Add `@PreAuthorize` to audit endpoints — verified GREEN by
      `EndpointPrivilegeCoverageTest`

**Checkpoint**: `EndpointPrivilegeCoverageTest` passes (no unguarded endpoints).
Access denial tests pass for all categories. ✓ COMPLETE

---

## Phase 4 (US2): Role inheritance wired end-to-end

**Goal**: A user with only `Validation` in Hematology can perform Reception and
Results actions in Hematology without additional role assignments.

### Tests (write first — must fail before implementation)

- [ ] T029 Write E2E Playwright test — create user with `Validation` in
      Hematology only, log in, enter a result for a Hematology analysis, then
      validate it. Both should succeed. Attempt same for a Chemistry analysis —
      should be blocked.
      `frontend/src/tests/e2e/rbac/validation-inheritance.spec.ts`

- [x] T030 Write unit test for `getAllPrivilegesForUser()` with full inheritance
      chain — Validation user receives result:validate + result:view +
      result:enter + order:create + patient:view via
      Validation→Results→Reception chain. Reception user does NOT receive
      Validation-only privileges. Renamed to `*Test` for Maven Surefire.
      `src/test/java/org/openelisglobal/privilege/service/PrivilegeResolutionTest.java`

### Implementation

- [x] T031 Verify `UserServiceImpl.filterResultsByLabUnitRoles()` and
      `filterAnalysesByLabUnitRoles()` correctly scope to the user's assigned
      departments — no production code changes needed; filtering logic is
      correct. Added 12 unit tests via subclass override of
      `getUserTestSections()` to bypass `RequestContextHolder`/`HttpSession`
      coupling.
      `src/test/java/org/openelisglobal/systemuser/service/UserServiceLabUnitFilterTest.java`

- [ ] T032 Update workplan controllers to use privilege check rather than direct
      role check — `WorkplanByTestSectionRestController` should gate on
      `PRIV_RESULT_VIEW` not `Constants.ROLE_RESULTS`
      `src/main/java/org/openelisglobal/workplan/controller/rest/WorkplanByTestSectionRestController.java`

**Checkpoint**: T029 E2E test passes. A Validation user in Hematology can
complete the full workflow: create order → enter result → validate result.

---

## Phase 5 (US3 + US4): Privilege-aware frontend + Admin UI

**Goal**: Frontend checks `hasPrivilege()` not role names. Admin can see
effective privilege sets and assign roles.

### Session endpoint update

- [ ] T033 Update `LoginPageController` session response to include `privileges`
      array — call `privilegeService.getAllPrivilegesForUser()` and include
      result in `UserSession` JSON
      `src/main/java/org/openelisglobal/login/controller/LoginPageController.java`

- [ ] T034 [P] Update `UserSession` form/DTO to include
      `List<String> privileges` field
      `src/main/java/org/openelisglobal/login/form/LoginForm.java` (or
      equivalent session DTO)

### Frontend privilege utility

- [ ] T035 Add `hasPrivilege(privilegeName)` function to
      `frontend/src/components/security/SecureRoute.js` — checks against
      `userSession.privileges` array

- [ ] T036 [P] Add `Privileges` constants object to `Utils.js` — mirrors
      `Privileges.java` constants. All privilege name strings defined here.
      Remove any role-name strings from component files.
      `frontend/src/components/utils/Utils.js`

- [ ] T037 Update `Roles` object in `Utils.js` — fix `CytoPathologist` →
      `Cytopathologist` spelling, ensure all role name strings match
      `Constants.java` `frontend/src/components/utils/Utils.js`

### Frontend component updates

- [ ] T038 [P] Replace role-name checks with `hasPrivilege()` in navigation and
      menu visibility (sidebar, banner menu) — components currently checking
      `roles.has("Global Administrator")` or similar

- [ ] T039 [P] Replace role-name checks in pathology/cytology components
      (currently checking `"Pathologist"`, `"Cytopathologist"`,
      `"CytoPathologist"`) with `hasPrivilege('result:pathology-sign-off')` etc.

- [ ] T040 [P] Update `SecureRoute` lab-unit permission check to also verify the
      effective privilege set — a `Validation` user routed to Results entry
      should be allowed because they inherit `result:enter`

### Admin UI — privilege summary panel

- [ ] T041 Add `GET /rest/roles/{roleId}/privileges` endpoint returning the full
      effective privilege set (direct + inherited) for a given role
      `src/main/java/org/openelisglobal/role/controller/rest/RoleRestController.java`

- [ ] T042 Add privilege summary panel to the role assignment section of the
      user management UI — Carbon `DataTable` showing effective privileges when
      a role is selected. Uses the new endpoint from T041.
      `frontend/src/components/systemuser/` (relevant component file)

### Tests

- [ ] T043 [P] Write Jest test for `hasPrivilege()` — test with populated
      session, missing privilege, empty session, admin (all privileges)
      `frontend/src/tests/unit/security/hasPrivilege.test.js`

- [ ] T044 [P] Write Playwright E2E test — log in as Reception, verify Result
      Entry menu item is absent. Log in as Validation (inherits Results), verify
      Result Entry is present. No role-name string appears in component source.
      `frontend/src/tests/e2e/rbac/privilege-aware-nav.spec.ts`

**Checkpoint**: Frontend has zero bare role-name string literals in component
files. Admin can select a role and see its full effective privilege list. T043
and T044 pass.

---

## Phase 6: Cross-cutting hardening

- [ ] T045 Add `grep` CI check — fail build if any Java file outside
      `Constants.java` and Liquibase XML contains a hardcoded role name string
      (e.g., `"Global Administrator"`, `"Reception"`, `"Validation"`). Use the
      role names from `Constants.java` as the pattern list. `.github/workflows/`
      (or equivalent CI config)

- [ ] T046 [P] Add ESLint rule — fail frontend build if any `.js`/`.tsx` file
      outside `Utils.js` contains a hardcoded role name string
      `frontend/.eslintrc.js` or `frontend/eslint.config.js`

- [ ] T047 Update `RolesConfigurationHandler` CSV processing — when a new role
      is loaded from CSV, if it has a `parentRole` column, resolve and set
      `groupingParent` using the new functional inheritance meaning. Log a
      warning if no privileges are assigned to a non-grouping role.
      `src/main/java/org/openelisglobal/role/service/RolesConfigurationHandler.java`

- [ ] T048 [P] Write javadoc on
      `PrivilegeServiceImpl.resolveAllPrivilegesForRole()` explaining the
      parent-chain walk, the Global Admin shortcut, and the circular-reference
      guard

- [ ] T049 Update `AGENTS.md` RBAC section to document the new privilege model,
      the `Privileges.java` constants file, and the rule: every new endpoint
      MUST have a `@PreAuthorize("hasAuthority('PRIV_*')")` annotation using a
      constant from `Privileges.java`

**Checkpoint**: CI fails on any new hardcoded role/privilege string. All
existing code clean.

---

## Phase 7 (US5): Legacy system retirement — LAST

**⚠️ Do not start until all prior phases are stable in production for at least
one sprint.**

- [ ] T050 Verify `EndpointPrivilegeCoverageIT` covers 100% of endpoints — if
      yes, `ModuleAuthenticationInterceptor` can be disabled

- [ ] T051 Disable `ModuleAuthenticationInterceptor` — remove from
      `WebMvcConfigurer` interceptor registry. Do NOT delete yet.
      `src/main/java/org/openelisglobal/config/` (interceptor registration)

- [ ] T052 Run full E2E suite with interceptor disabled — all access control
      tests must still pass via `@PreAuthorize` alone

- [ ] T053 Delete `ModuleAuthenticationInterceptor` class and its test
      `src/main/java/org/openelisglobal/interceptor/ModuleAuthenticationInterceptor.java`

- [ ] T054 Write Liquibase changeset to drop `system_role_module` and
      `system_module_url` tables (with `preConditions onFail="MARK_RAN"` guard)
      `src/main/resources/liquibase/2.7.x.x/012-009-drop-legacy-module-tables.xml`

- [ ] T055 Remove `SystemModuleUrl`, `SystemModule`, `PermissionModule`,
      `RoleModule` valueholders and their DAOs/services — confirm no remaining
      references `src/main/java/org/openelisglobal/systemusermodule/`

**Checkpoint**: Application runs with zero references to `system_role_module` or
`ModuleAuthenticationInterceptor`. All access control flows through
`@PreAuthorize` + privilege resolution alone.

---

## Dependencies & Execution Order

```
Phase 1 (DB Schema + Seed)
    └── Phase 2 (Privilege Entity + Resolution Service)
            ├── Phase 3 (Endpoint @PreAuthorize) ─────┐
            ├── Phase 4 (Inheritance E2E)              │ can run
            ├── Phase 5 (Frontend + Admin UI)          │ in parallel
            └── Phase 6 (CI Hardening) ────────────────┘
                    └── Phase 7 (Legacy Retirement) ← LAST, production-gated
```

### Parallel opportunities within phases

- T001–T007 (Liquibase changesets): each file is independent — can be written in
  parallel, applied sequentially by Liquibase
- T008–T009 (entity valueholders): independent files, parallel
- T010–T012 (DAO, service, constants): independent files, parallel
- T020–T028 (controller @PreAuthorize additions): different controller files,
  fully parallel across developers
- T035–T040 (frontend updates): different component files, parallel

---

## Notes

- Privilege names in `@PreAuthorize` must always use a constant from
  `Privileges.java` — never a string literal
- Role name strings must always use a constant from `Constants.java` — never a
  string literal
- During Phase 3, when replacing `hasRole('ADMIN')` on ~60 controllers, do it in
  a single commit to avoid a split state where some controllers are on the old
  system and some on the new
- The `system_role_module` and `system_module_url` tables are NOT touched until
  Phase 7 — the module interceptor continues running in parallel during the
  transition as a safety net
- `Global Administrator` privilege resolution shortcut must be the first check
  in `resolveAllPrivilegesForRole()` to avoid unnecessary DB queries for admins
- The `groupingParent` semantic change (display → functional inheritance) is the
  highest-risk schema decision: audit all existing `groupingParent` values in
  the DB before running T005 to confirm no non-grouping roles have unexpected
  parents set
