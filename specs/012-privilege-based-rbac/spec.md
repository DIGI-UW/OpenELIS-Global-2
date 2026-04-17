# Feature Specification: Privilege-Based RBAC with Role Inheritance

**Feature Branch**: `012-privilege-based-rbac` **Created**: 2026-04-09
**Status**: Draft

---

## Background & Motivation

The current access control system in OpenELIS Global has three parallel,
loosely-coupled enforcement mechanisms:

1. **`system_role_module` + `ModuleAuthenticationInterceptor`** — URL-path-based
   access mapped to roles via the legacy module system
2. **`@PreAuthorize("hasRole('ADMIN')")`** — Spring Security annotations on
   controllers, inconsistently applied across ~70 controllers, all pointing to a
   phantom `ADMIN` authority with no corresponding constant
3. **`userService.filterResultsByLabUnitRoles()`** — optional, in-memory data
   filtering in a handful of controllers

The result is a system where:

- Adding a new endpoint requires remembering to wire three separate systems
- Any authenticated user can reach REST endpoints not registered in
  `system_module_url` (auto-allow gap in `ModuleAuthenticationInterceptor`)
- A `Validation` user must be separately assigned `Reception` and `Results`
  roles even though validation is a superset of those capabilities
- The frontend and backend use different strings to refer to the same roles
- There is no concept of a "privilege" — only coarse roles that map to entire UI
  sections, not specific operations

This migration replaces all three mechanisms with a single, coherent
**Permission-Based RBAC with Role Inheritance** model.

---

## User Scenarios & Testing

### User Story 1 — Privilege enforcement at every endpoint (Priority: P1)

A lab administrator can trust that every REST endpoint in the system requires an
explicit privilege check. No endpoint is reachable by "any authenticated user"
by default.

**Why this priority**: This closes the most critical security gap. Everything
else builds on top of a properly secured endpoint layer.

**Independent Test**: Deploy the application, create a user with only the
`Reception` role, and attempt to call `GET /rest/nce/dashboard`. The request
should return HTTP 403. Repeat for a dozen other unguarded endpoints. All must
be 403 for unprivileged users.

**Acceptance Scenarios**:

1. **Given** a user with `Reception` role only, **When** they call
   `GET /rest/nce/dashboard`, **Then** they receive HTTP 403
2. **Given** a user with `Validation` role, **When** they call
   `GET /rest/nce/dashboard`, **Then** they receive HTTP 200 (NCE view is a
   validation-level privilege)
3. **Given** an unauthenticated request, **When** it hits any `/rest/*`
   endpoint, **Then** it receives HTTP 401
4. **Given** a user with `Global Administrator`, **When** they call any
   endpoint, **Then** they receive HTTP 200 (admin has all privileges)

---

### User Story 2 — Role inheritance (Priority: P1)

When a user is assigned the `Validation` role in a department, they
automatically have all capabilities of `Results` and `Reception` within that
same department — without needing three separate role assignments.

**Why this priority**: This is the core usability fix. Today, lab admins must
assign 3 roles to a validator; with inheritance they assign 1.

**Independent Test**: Create a user, assign only `Validation` role to
Hematology. Verify they can register a patient (Reception privilege), enter a
result (Results privilege), and validate a result (Validation privilege) — all
scoped to Hematology tests only.

**Acceptance Scenarios**:

1. **Given** a user assigned `Validation` in Hematology only, **When** they
   enter a result for a Hematology analysis, **Then** they succeed
2. **Given** a user assigned `Validation` in Hematology only, **When** they
   attempt to enter a result for a Chemistry analysis, **Then** they receive
   HTTP 403
3. **Given** a user assigned `Results` in Chemistry, **When** they attempt to
   validate a Chemistry result, **Then** they receive HTTP 403 (validation not
   inherited downward)
4. **Given** the `admin` user with `Global Administrator`, **When** the
   privilege resolution runs, **Then** all privileges are granted regardless of
   department

---

### User Story 3 — Privilege-aware frontend (Priority: P2)

The frontend gates UI elements by privilege strings, not role name strings. A
component does not need to know that "Validation inherits Results" — it checks
`hasPrivilege('result:validate')` and gets the correct answer regardless of how
the user acquired that privilege.

**Why this priority**: Eliminates the frontend's role-name coupling and the
`CytoPathologist` typo class of bugs. Unblocks independent frontend/backend
evolution.

**Independent Test**: Log in as a `Validation` user. Verify that the Result
Entry form, Validation queue, and Workplan are all visible. Log in as
`Reception`. Verify Result Entry and Validation queue are hidden. No role-name
strings should appear in frontend component code.

**Acceptance Scenarios**:

1. **Given** a `Reception` user, **When** the navigation renders, **Then** the
   Result Entry menu item is hidden
2. **Given** a `Validation` user (who inherits Results), **When** the navigation
   renders, **Then** Result Entry is visible even though `Results` role was
   never explicitly assigned
3. **Given** the session endpoint `/rest/session`, **When** called by any
   authenticated user, **Then** the response includes a `privileges` array
   containing all resolved privilege strings for that user
4. **Given** a frontend component checking `hasPrivilege('result:validate')`,
   **When** the user has `Validation` role, **Then** the check returns true

---

### User Story 4 — Lab administrator role management UI (Priority: P2)

A lab administrator can view what privileges each role grants (including
inherited ones) and assign roles to users scoped to departments — without
needing developer intervention.

**Why this priority**: Makes the system self-service for labs. Today assigning
roles requires understanding three separate DB tables.

**Independent Test**: Log in as admin, navigate to User Management, select a
user, assign `Results` to the `Hematology` department. Verify the user can now
enter results for Hematology tests and that the privilege summary panel shows
all effective privileges including inherited ones from `Reception`.

**Acceptance Scenarios**:

1. **Given** the role management screen, **When** an admin selects the
   `Validation` role, **Then** the UI shows the full effective privilege set
   (own + inherited from Results + inherited from Reception)
2. **Given** a user with no roles, **When** an admin assigns `Reception` to
   `AllLabUnits`, **Then** the user can immediately create orders in any
   department
3. **Given** a non-admin user, **When** they access the user management
   endpoint, **Then** they receive HTTP 403

---

### User Story 5 — Legacy role/module system retired (Priority: P3)

The `system_role_module` table and `ModuleAuthenticationInterceptor` are
removed. All access control flows exclusively through the privilege system.

**Why this priority**: Removes the maintenance burden of keeping three systems
in sync. Only undertaken after P1/P2 stories are complete and stable.

**Independent Test**: Remove all `system_role_module` rows from the DB and
disable `ModuleAuthenticationInterceptor`. All access control should still work
identically via the privilege system alone.

**Acceptance Scenarios**:

1. **Given** `ModuleAuthenticationInterceptor` is disabled, **When** any
   authenticated user accesses a privileged endpoint without the required
   privilege, **Then** they receive HTTP 403
2. **Given** the `system_role_module` table is empty, **When** the application
   starts, **Then** no errors are thrown and access control works correctly
3. **Given** the codebase after retirement, **When** `grep system_role_module`
   is run, **Then** only Liquibase migration files reference it (for historical
   record)

---

### Edge Cases

- What happens when a role's parent is deleted? Child role must remain
  functional with only its own direct privileges.
- What if a user has both `Results` (Hematology) and `Validation` (Chemistry)?
  Privilege resolution must be per-department, not merged globally.
- What if `AllLabUnits` and a specific lab unit are both assigned to the same
  user with different roles? `AllLabUnits` grants access everywhere; specific
  assignment is additive.
- What if privilege resolution produces an empty set for a user with active
  roles? Should not be possible — every assignable role must have at least one
  direct privilege.
- What happens during the migration period when both the old module system and
  new privilege system are active? Both enforce independently — more restrictive
  wins. Gradually migrate endpoints to privilege-only.

---

## Requirements

### Functional Requirements

- **FR-001**: System MUST have a `system_privilege` table storing atomic
  capability definitions (name, description, category)
- **FR-002**: System MUST have a `system_role_privilege` table mapping roles to
  their directly-owned privileges (not inherited)
- **FR-003**: `CustomUserDetailsService` MUST resolve the full transitive
  privilege set (own + all ancestor roles) at login time and load them as Spring
  `GrantedAuthority` objects
- **FR-004**: Every `@PreAuthorize` annotation MUST reference a privilege
  constant (e.g., `hasAuthority('order:create')`), never a role name
- **FR-005**: The `Role` entity MUST support a parent role reference that drives
  privilege inheritance (the existing `groupingParent` field repurposed)
- **FR-006**: The `/rest/session` endpoint MUST return the resolved `privileges`
  array in its JSON response
- **FR-007**: Frontend privilege checks MUST use a `hasPrivilege(name)` utility
  against the session privilege list — no role name strings in component code
- **FR-008**: The auto-allow fallback in `ModuleAuthenticationInterceptor` MUST
  be changed to deny (return false) for REST endpoints not in
  `system_module_url` as an interim measure before full legacy retirement
- **FR-009**: All role name constants MUST be defined in `Constants.java` — no
  hardcoded role name strings in Java code outside of Liquibase migrations
- **FR-010**: A Liquibase migration MUST seed the full privilege registry and
  role-to-privilege mappings for all existing roles
- **FR-011**: Legacy inactive roles (`Inventory mgr`, `Results Admin`,
  `Results entry`, `Validator`) MUST be formally deactivated and user
  assignments migrated to canonical roles in a Liquibase migration
- **FR-012**: Role inheritance MUST be single-parent (no multiple inheritance)
  to keep resolution predictable
- **FR-013**: `Global Administrator` role MUST implicitly hold all privileges
  without requiring explicit `system_role_privilege` entries

### Constitution Compliance Requirements

- **CR-001**: UI components MUST use Carbon Design System — privilege summary
  panel and updated role assignment UI use `@carbon/react` exclusively
- **CR-002**: All new UI strings MUST be in `en.json` with React Intl keys
- **CR-003**: Backend MUST follow 5-layer architecture — new `Privilege` entity
  follows `Valueholder→DAO→Service→Controller→Form` pattern
- **CR-004**: All schema changes (new tables, foreign keys, seed data) MUST use
  Liquibase changesets — NO direct DDL
- **CR-007**: This feature IS the security improvement — audit trail entries
  (`sys_user_id` + `lastupdated`) required on all new entities
- **CR-008**: Unit tests for privilege resolution logic; integration tests for
  endpoint access scenarios; E2E tests for role assignment UI

### Key Entities

- **Privilege**: Atomic capability (e.g., `result:enter`). Attributes: id, name,
  description, category, active
- **Role**: Existing entity, extended with a parent role reference that drives
  privilege inheritance. `groupingParent` field repurposed from display-only to
  functional inheritance pointer.
- **RolePrivilege**: Join between Role and Privilege for directly-owned
  privileges (not inherited). Replaces `system_role_module` semantically.
- **UserRole**: Existing — global role assignment, no change needed
- **UserLabUnitRoles**: Existing — department-scoped role assignment, no change
  needed. Privilege resolution happens after lab-unit role lookup.

---

## Success Criteria

### Measurable Outcomes

- **SC-001**: Zero endpoints reachable by an authenticated user who lacks the
  required privilege — verified by an automated endpoint audit test
- **SC-002**: A user assigned only `Validation` in one department has the full
  effective privilege set of Reception + Results + Validation for that
  department — verified by unit test on privilege resolution service
- **SC-003**: No hardcoded role name strings remain in Java controller or
  service code outside of `Constants.java` and Liquibase files — verified by
  `grep` in CI
- **SC-004**: The frontend contains zero bare role-name string literals in
  component files — verified by ESLint rule
- **SC-005**: All 18 existing roles have complete privilege mappings in the DB
  seed migration — verified by migration checksum
- **SC-006**: Lab admin can assign a role to a user scoped to a department
  entirely through the UI without developer intervention
- **SC-007**: `ModuleAuthenticationInterceptor` auto-allow fallback is removed —
  verified by integration test asserting 403 on an unregistered REST path
