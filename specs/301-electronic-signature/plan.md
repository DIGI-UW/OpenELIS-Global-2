# Plan: PR #3288 Electronic Signature — Review & Remediation

## Context

PR #3288 by @samuelmale adds FDA 21 CFR Part 11 electronic signature support to
OpenELIS. Jira ticket: **OGC-301** ("Implement 21 CFR Part 11 Compliant
Electronic Signatures"), Madagascar requirement DU-11-03, assigned to Samuel
Male, linked to OGC-343 (Multi-Level Validation Pipeline).

**Authoritative artifacts:**

- FRS: `DIGI-UW/openelis-work/designs/system/electronic-signature.md` (Casey,
  2026-02-09)
- Mockup: `DIGI-UW/openelis-work/designs/system/electronic-signature.jsx`
- Gallery: https://digi-uw.github.io/openelis-work/#/system/electronic-signature

**Current state:** Fix commit `fd5c04d4e` already applied (security,
correctness, quality). Merge conflict with develop (en.json + submodule) in
progress.

---

## Research Findings

### 21 CFR Part 11 Compliance Gaps

| #   | Issue                                                                                                                       | Severity | Source           | Status                            |
| --- | --------------------------------------------------------------------------------------------------------------------------- | -------- | ---------------- | --------------------------------- |
| R1  | **Session timeout 8h is far too long** — industry standard is 15-30 min of inactivity; 8h would draw FDA inspector scrutiny | HIGH     | Research         | **NEEDS FIX**                     |
| R2  | **Default `esig.enabled=true`** in migration — FRS §13 says default false                                                   | MEDIUM   | FRS vs migration | **NEEDS FIX**                     |
| R3  | **No record hash at signing time** — §11.70 requires tamper-evidence linking signature to record state                      | HIGH     | Research         | DEFER (follow-up ticket)          |
| R4  | **No lockout controls** — FRS §13 specifies max_attempts=5, lockout_duration=30min                                          | MEDIUM   | FRS              | DEFER (follow-up ticket)          |
| R5  | **No audit log endpoint** — FRS §12 specifies `GET /admin/.../audit-log`                                                    | LOW      | FRS              | DEFER (follow-up ticket)          |
| R6  | **Dual-signature constraint missing** — FRS §4 says cannot validate own results                                             | HIGH     | FRS              | DEFER (separate validation logic) |
| R7  | **No configurable certification text/version** — FRS §6 specifies versioned configurable text                               | LOW      | FRS              | DEFER                             |

### Mockup vs Implementation Gaps

| #   | Gap                                                                                        | Severity | Status              |
| --- | ------------------------------------------------------------------------------------------ | -------- | ------------------- |
| M1  | **Password-only mode shows editable username** — mockup shows read-only identity bar       | MEDIUM   | **NEEDS FIX**       |
| M2  | **Full auth mode doesn't pre-fill username** — FRS §7.1 says pre-fill, editable            | MEDIUM   | **NEEDS FIX**       |
| M3  | No MeaningBadge colored tag component                                                      | LOW      | DEFER (cosmetic)    |
| M4  | Certification text doesn't interpolate user's full name                                    | LOW      | DEFER               |
| M5  | No PasswordInput with visibility toggle (uses plain TextInput)                             | LOW      | DEFER (cosmetic)    |
| M6  | Certification step bundles password — mockup separates certification from credential entry | MEDIUM   | DEFER (flow change) |

### FRS Role Model vs Implementation

FRS §4 defines three roles: `RESULT_ENTRY_SIGN`, `RESULT_VALIDATE_SIGN`,
`ESIG_ADMIN`. Current implementation only checks `ADMIN` on admin endpoints. The
signing endpoints have no role checks beyond session identity validation.

**Decision needed:** implement FRS role model now, or defer? The role model
requires Liquibase inserts into `system_module` + `system_role_module` tables
and is a significant addition.

---

## Completed Work (fix commit fd5c04d4e)

These items are DONE and will not be repeated:

- [x] PR comment posted
- [x] Switch `BaseController` → `BaseRestController`
- [x] Add identity validation (username must match authenticated session)
- [x] Add `@PreAuthorize("hasRole('ADMIN')")` to admin endpoints
- [x] Scope `/certified/` and `/session-status/` to current user only
- [x] Fix DAO enum binding (`meaning.name()` → `meaning`)
- [x] Thread-safe `AtomicInteger` for session signing counter
- [x] Activity-based session expiry (`lastActivityAt` instead of `startedAt`)
- [x] Throw on unknown user in `revokeCertification()`
- [x] Rethrow system errors in credential verification
- [x] Remove `console.log` from ESignatureModal
- [x] Fix loose equality (`==` → `===`)
- [x] Add missing i18n keys (`esig.label.username`, `esig.placeholder.username`,
      `esig.error.usernameRequired`)
- [x] Fix misleading i18n text (fullAuth/legalBinding mention username+password)
- [x] Use `nextval('reference_tables_seq')` instead of `MAX(id)+1`
- [x] Remove class-level `@Transactional` from DAO

---

## Remaining Work

### Phase 1: Merge Conflicts

#### 1a. Resolve en.json conflict

- Keep BOTH sides: HEAD's `esig.*` keys + develop's `errorBoundary.*` keys
- Maintain alphabetical order
- Conflict is at lines 1542-1587

#### 1b. Resolve submodule conflict

- `tools/openelis-analyzer-bridge`: accept develop's pointer (`299c8359e`)
- This PR doesn't modify the bridge

### Phase 2: Research-Driven Fixes (this PR)

#### 2a. Make session timeout configurable, default 30 min (R1)

- **File:** `ElectronicSignatureServiceImpl.java` → `SigningSessionInfo` +
  `isExpired()`
- The 8-hour value was the PR author's assumption ("typical session timeout")
  but conflates web session with e-sig signing session — different concepts
- Change hardcoded `SESSION_TIMEOUT_HOURS = 8` to read from
  `ConfigurationProperties` with a `Property.ESIG_SESSION_TIMEOUT_MINUTES` key
- Default: 30 minutes (industry standard for 21 CFR Part 11)
- **File:** `ConfigurationProperties.java` — add `ESIG_SESSION_TIMEOUT_MINUTES`
- **File:** `003-electronic-signature.xml` — add site_information row for the
  setting

#### 2b. Default esig.enabled to false (R2)

- **File:** `003-electronic-signature.xml`, changeset `esig-010`
- Change `<column name="value" value="true" />` → `value="false"`

#### 2c. Fix password-only mode UI (M1)

- **File:** `ESignatureModal.js`
- When `flowStep === "passwordOnly"`: render username as read-only text (not
  editable `TextInput`)
- Show styled identity bar like mockup: "{Full Name} ({username})"

#### 2d. Pre-fill username in full auth mode (M2)

- **File:** `ESignatureModal.js`
- When `flowStep === "fullAuth"`: pre-fill `enteredUsername` with `userName`
  from session (currently sets to `""`)

### Phase 3: Build, Test, Push

1. `mvn spotless:apply` + `cd frontend && npm run format`
2. `mvn clean install -DskipTests -Dmaven.test.skip=true` (compile check)
3. Commit with descriptive message
4. `git push origin feature/electronic-signature`
5. Verify CI passes via `gh pr checks 3288`

---

## Phase 4: Jira Epic + Follow-Up Tickets

No epic exists for e-signatures. OGC-301 is a standalone Task.

### 4a. Create Epic

- Title: "21 CFR Part 11 Electronic Signatures"
- Description: Reference FRS at
  `DIGI-UW/openelis-work/designs/system/electronic-signature.md`
- Move OGC-301 and OGC-302 under this epic

### 4b. Create follow-up tickets under the epic

| #   | Title                                                                          | Type | FRS Ref  |
| --- | ------------------------------------------------------------------------------ | ---- | -------- |
| 1   | E-sig: Record hash at signing time (tamper-evidence per §11.70)                | Task | §11.70   |
| 2   | E-sig: Lockout controls (5 attempts / 30 min)                                  | Task | FRS §13  |
| 3   | E-sig: Audit log query endpoint                                                | Task | FRS §12  |
| 4   | E-sig: Role-based access (RESULT_ENTRY_SIGN, RESULT_VALIDATE_SIGN, ESIG_ADMIN) | Task | FRS §4   |
| 5   | E-sig: Configurable/versioned certification text                               | Task | FRS §6   |
| 6   | E-sig: UI polish (MeaningBadge, PasswordInput, cert flow split)                | Task | Mockup   |
| 7   | E-sig: Batch signing with atomic rollback                                      | Task | FRS §8.4 |

Link OGC-343 (dual-signature constraint) to the epic as well.

## Phase 5: Spec Folder

Create `specs/301-electronic-signature/` with lightweight SpecKit structure:

- `spec.md` — references FRS, summarizes scope/decisions/deferred items
- Copy this plan file into the folder as `plan.md`

---

## Decisions Made

- **Roles:** Defer FRS role model to follow-up. Keep `hasRole('ADMIN')` for now.
- **Timeout:** 30 min default, configurable via site_information setting. Origin
  of 8h: PR author's assumption, conflating web session with signing session.
- **Default enabled:** false (per FRS). Sites must opt-in.
- **UI:** Fix password-only mode + pre-fill username in this PR.

## Critical Files

| File                                  | Remaining Changes                                   |
| ------------------------------------- | --------------------------------------------------- |
| `frontend/src/languages/en.json`      | Merge conflict resolution                           |
| `tools/openelis-analyzer-bridge`      | Submodule conflict resolution                       |
| `ElectronicSignatureServiceImpl.java` | Configurable session timeout (default 30min)        |
| `ConfigurationProperties.java`        | Add `ESIG_SESSION_TIMEOUT_MINUTES` property         |
| `003-electronic-signature.xml`        | Default enabled → false, add timeout setting        |
| `ESignatureModal.js`                  | Password-only read-only username, fullAuth pre-fill |

## Verification

1. `mvn clean install -DskipTests -Dmaven.test.skip=true` compiles cleanly
2. `ElectronicSignatureServiceIntegrationTest` passes
3. Playwright `electronic-signature.spec.ts` passes
4. PR merge conflicts resolved (no "merge conflict" label)
5. CI passes after push
