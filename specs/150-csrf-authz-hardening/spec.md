# Feature Specification: CSRF & REST Authorization Hardening

> **Status:** PARTIALLY SUPERSEDED — F2 landed elsewhere; F3 still outstanding.
> **Last reconciled:** 2026-04-20 **Reality notes:** F2 (REST interceptor
> bypass) was addressed by PR #3329 ("security: REST @PreAuthorize
> remediation"). Frontend CSRF token wiring was touched in commit `3c27b8ac2`.
> The comprehensive deny-by-default REST authorization + server-side CSRF
> enforcement scope described here did not ship as a single feature. Remaining
> work (F3 / CSRF globally enabled for `/rest/**`) is still outstanding. Spec
> retained as the design-of-record for the unfinished F3 scope; no
> plan.md/tasks.md drafted.

**Feature Branch**: `150-csrf-authz-hardening` **Created**: 2026-02-09
**Status**: Draft **Input**: User description: "Implement CSRF protection and
deny-by-default REST authorization to fix the two remaining HIGH/MEDIUM findings
from the project-wide security audit (F2: REST interceptor bypass, F3: CSRF
globally disabled)"

## User Scenarios & Testing _(mandatory)_

### User Story 1 - REST Endpoints Enforce CSRF Protection (Priority: P1)

An authenticated user's browser session is protected from cross-site request
forgery attacks. When a malicious website tries to trick the user's browser into
making state-changing requests (POST, PUT, DELETE, PATCH) to OpenELIS, the
system rejects those requests because they lack a valid CSRF token.

**Why this priority**: CSRF protection for REST endpoints is the highest-value
fix because: (a) the frontend already sends CSRF tokens on all state-changing
requests via `X-CSRF-Token` header across 14 files / 29 call sites, (b) the
backend already generates and validates tokens for non-REST paths, and (c) the
only backend change required is removing the `/rest/**` exception from the
default security filter chain. This is a high-impact fix with minimal risk.

**Independent Test**: Can be fully tested by attempting a cross-origin POST to
any `/rest/` endpoint without a CSRF token and verifying it is rejected with
HTTP 403, while the same request with a valid token succeeds.

**Acceptance Scenarios**:

1. **Given** an authenticated user session, **When** a state-changing request
   (POST/PUT/DELETE/PATCH) is made to a `/rest/` endpoint with a valid CSRF
   token, **Then** the request succeeds as normal.
2. **Given** an authenticated user session, **When** a state-changing request is
   made to a `/rest/` endpoint without a CSRF token or with an invalid token,
   **Then** the request is rejected with HTTP 403 Forbidden.
3. **Given** an authenticated user session, **When** a GET request is made to
   any `/rest/` endpoint without a CSRF token, **Then** the request succeeds
   (CSRF only applies to state-changing methods).
4. **Given** the React frontend application, **When** any existing workflow
   (order entry, result validation, patient search, etc.) is used normally,
   **Then** all operations complete successfully because the frontend already
   sends `X-CSRF-Token` headers.
5. **Given** an HTTP Basic authenticated client (e.g., external analyzer
   integration), **When** it makes requests to the system, **Then** CSRF is not
   required because Basic auth is not cookie-based and is not vulnerable to
   CSRF.

---

### User Story 2 - REST Endpoints Require Module Authorization (Priority: P2)

All REST endpoints enforce role-based access control. When a non-admin user
tries to access a REST endpoint, the system checks whether the user's roles
grant permission for that specific module. Endpoints that currently auto-allow
any authenticated user are mapped to appropriate permission modules.

**Why this priority**: This is the larger engineering effort — 109 REST
controllers exist but only 3 have module authorization mappings in the
`system_module_url` table. Every unmapped REST endpoint currently auto-allows
access for any authenticated user. Fixing this requires cataloging all
endpoints, creating module mappings, assigning them to roles, and flipping the
interceptor default from allow to deny.

**Independent Test**: Can be tested by logging in as a user with a limited role
(e.g., "Reception"), attempting to access a REST endpoint outside that role's
scope (e.g., an admin-only configuration endpoint), and verifying access is
denied.

**Acceptance Scenarios**:

1. **Given** a user with only the "Reception" role, **When** they request a REST
   endpoint for reception workflows (e.g., sample order entry), **Then** access
   is granted.
2. **Given** a user with only the "Reception" role, **When** they request a REST
   endpoint for admin-only operations (e.g., site configuration, user
   management), **Then** access is denied with HTTP 401.
3. **Given** an admin user, **When** they request any REST endpoint, **Then**
   access is granted (admin bypass is preserved).
4. **Given** a new REST controller added to the codebase without a module
   mapping, **When** any non-admin user tries to access it, **Then** access is
   denied by default (secure-by-default).
5. **Given** the existing UI application, **When** any user performs their
   normal workflows, **Then** all operations succeed because their roles include
   the required module permissions.

---

### User Story 3 - Graceful Error Handling for Denied Requests (Priority: P3)

When a user is denied access due to missing CSRF tokens or insufficient
permissions, they receive clear, actionable feedback rather than a generic error
page.

**Why this priority**: Good error handling ensures users understand why access
was denied and what to do about it. For CSRF failures, the frontend should
detect the 403 and prompt the user to refresh their session. For authorization
failures, the user should see an appropriate "access denied" message.

**Independent Test**: Can be tested by triggering a CSRF rejection (expired
token) and verifying the frontend displays a session-refresh prompt rather than
an opaque error.

**Acceptance Scenarios**:

1. **Given** a user whose CSRF token has expired (e.g., session timed out),
   **When** they attempt a save or submit action, **Then** they see a clear
   message indicating their session needs to be refreshed, not a generic error.
2. **Given** a user who navigates to a page they don't have permission for,
   **When** the REST call returns 401, **Then** they see an "access denied"
   notification consistent with the existing UI pattern.

---

### Edge Cases

- What happens when a user's session expires mid-form-entry and they submit? The
  CSRF token becomes invalid — the frontend should detect the 403 and guide the
  user to re-authenticate without losing their form data if possible.
- How does the system handle legitimate cross-origin requests from external
  integrations (e.g., FHIR endpoints, analyzer bridges)? These use HTTP Basic or
  certificate auth, which have separate filter chains not affected by CSRF.
- What happens if a module mapping is accidentally deleted from the database?
  The endpoint becomes inaccessible to non-admin users — the deny-by-default
  behavior protects against accidental exposure.
- How are new REST controllers onboarded after this change? Developers must add
  a module mapping via Liquibase changeset, or the endpoint is inaccessible to
  non-admin users.
- What about REST endpoints that should be accessible to all authenticated users
  regardless of role (e.g., session info, user preferences)? These should be
  mapped to a universal module that all roles include.

## Requirements _(mandatory)_

### Functional Requirements

- **FR-001**: System MUST enforce CSRF token validation on all state-changing
  requests (POST, PUT, DELETE, PATCH) to `/rest/**` and
  `/api/OpenELIS-Global/rest/**` endpoints when the request is authenticated via
  session cookies.
- **FR-002**: System MUST NOT enforce CSRF on endpoints authenticated via HTTP
  Basic or client certificates (these authentication methods are not vulnerable
  to CSRF).
- **FR-003**: System MUST NOT enforce CSRF on GET/HEAD/OPTIONS requests
  (read-only methods are not CSRF-sensitive per the HTTP specification).
- **FR-004**: System MUST deny access to REST endpoints that have no module
  authorization mapping, for non-admin users (secure-by-default).
- **FR-005**: System MUST preserve admin bypass — users with `is_admin = 'Y'`
  retain access to all endpoints regardless of module mappings.
- **FR-006**: System MUST provide module authorization mappings for all 109
  existing REST controllers, assigned to appropriate roles so that no existing
  user workflow is broken.
- **FR-007**: System MUST return HTTP 403 with a JSON error body for CSRF
  failures on REST endpoints.
- **FR-008**: System MUST return HTTP 401 with a JSON error body for
  authorization failures on REST endpoints (consistent with current interceptor
  behavior).
- **FR-009**: Frontend MUST detect CSRF rejection (HTTP 403) and display a
  user-friendly session-refresh notification.
- **FR-010**: System MUST continue to allow the `/ValidateLogin` endpoint
  without CSRF (login form submission before a session exists).
- **FR-011**: System MUST provide a "common" module that all authenticated roles
  include, for endpoints that any logged-in user should access (e.g., session
  details, user preferences, dashboard data).

### Constitution Compliance Requirements (OpenELIS Global)

- **CR-004**: Database changes MUST use Liquibase changesets (NO direct
  DDL/DML). All `system_module`, `system_module_url`, and `system_role_module`
  entries must be added via Liquibase.
- **CR-007**: Security: RBAC, audit trail (sys_user_id + lastupdated), input
  validation. This feature directly implements RBAC enforcement for REST
  endpoints and adds CSRF protection against forged requests.
- **CR-008**: Tests MUST be included (unit + integration + E2E, >70% coverage
  goal). CSRF enforcement and authorization denial must have test coverage.

### Key Entities

- **SystemModule**: Represents a named permission unit (e.g.,
  "PatientManagement", "ResultEntry"). Each module has default CRUD flags
  (select, add, update, delete). New modules will be created to group related
  REST endpoints.
- **SystemModuleUrl**: Maps a URL path to a SystemModule. The interceptor uses
  this to determine which module protects a given endpoint. Currently has ~200
  entries for legacy JSP paths but only 3 for REST paths
  (`/rest/GenericSampleOrder` and variants).
- **SystemRoleModule**: Maps a role to a SystemModule with specific CRUD
  permissions. Determines which roles can access which modules.
- **SystemRole**: Named roles assigned to users (e.g., "Reception", "Results",
  "Validation", "Admin"). Module mappings must be assigned to appropriate roles.

## Assumptions & Constraints

- **Frontend CSRF already implemented**: The React frontend already sends
  `X-CSRF-Token` headers (from `localStorage`) on all POST/PUT/DELETE/PATCH
  requests across 14 files / 29 call sites. No frontend changes are needed for
  basic CSRF enforcement (only error handling for 403 responses is new).
- **CSRF token generation exists**: The `/session` endpoint
  (`LoginPageController.getSesssionDetails`) already provides CSRF tokens to the
  React frontend via `UserSession.csrf`. The `HttpSessionCsrfTokenRepository` is
  the token store (Spring default).
- **HTTP Basic and certificate chains are CSRF-exempt by design**: These
  authentication methods don't use cookies, so they are not vulnerable to CSRF.
  Their filter chains should remain CSRF-disabled.
- **Open pages chain is CSRF-exempt by design**: Unauthenticated pages don't
  need CSRF protection.
- **Authorization mode is "ROLE"**: The system uses role-based permissions
  (`permissions.agent=ROLE`), not user-module mode. Module mappings go through
  `SystemRoleModule`, not `SystemUserModule`.
- **Liquibase for all DB changes**: All `system_module`, `system_module_url`,
  and `system_role_module` inserts must be Liquibase changesets.
- **No frontend framework changes**: Carbon Design System is used; no new UI
  frameworks are introduced. Error handling uses existing notification patterns.
- **Milestone delivery**: This feature exceeds 3 days effort and should be
  broken into validation milestones (per Constitution Principle IX): Milestone 1
  = CSRF enforcement (P1), Milestone 2 = Authorization mappings (P2), Milestone
  3 = Error handling (P3).
- **Existing interceptor behavior preserved**: The
  `ModuleAuthenticationInterceptor` architecture is kept. The only change to its
  logic is removing the REST auto-allow fallback. No new interceptor or filter
  is introduced.

## Success Criteria _(mandatory)_

### Measurable Outcomes

- **SC-001**: Zero REST endpoints accept state-changing requests without valid
  CSRF tokens when accessed via session cookie authentication. Verified by
  automated tests that attempt CSRF-less POST/PUT/DELETE to representative
  endpoints.
- **SC-002**: Zero REST endpoints are accessible to non-admin users without an
  explicit module authorization mapping. Verified by querying for REST
  controllers without corresponding `system_module_url` entries (count = 0).
- **SC-003**: All existing user workflows (order entry, result validation,
  patient management, reporting, admin configuration) continue to function
  without authorization errors after module mappings are deployed. Verified by
  running the full E2E test suite with zero new failures.
- **SC-004**: New REST controllers added without module mappings are
  automatically denied for non-admin users (secure-by-default). Verified by
  adding a test controller with no mapping and confirming 401 response.
- **SC-005**: Users whose CSRF tokens expire see a clear session-refresh prompt,
  not a generic error. Verified by E2E test simulating token expiry.
